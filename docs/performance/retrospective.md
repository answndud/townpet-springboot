# 기존 성능 개선 회고와 재현 계획

현재 코드에는 이미 여러 성능·안정성 개선이 적용되어 있다. 당시 before 수치를 측정하지 않았더라도 개선을 기록할 수 있다. 다만 기록의 증거 수준을 구분해야 한다.

## 증거 수준

| 수준 | 이름 | 인정하는 근거 |
|---|---|---|
| A | historical replay | 변경 전 commit을 별도 실행하고 현재와 같은 fixture·부하·환경으로 다시 측정함 |
| B | code evidence | 변경 전후 코드·migration·query plan을 비교하고 현재 구현을 측정함. 과거 latency는 없음 |
| C | operational evidence | timeout·shutdown·limit처럼 안정성·자원 상한을 코드와 설정으로 확인함. 처리량 개선으로 표현하지 않음 |

이전에 측정하지 않은 수치를 추정해서 “몇 배 개선”이라고 쓰지 않는다. A 수준의 재현이 불가능하면 B 또는 C로 낮춰 기록하고 그 이유를 남긴다.

## 현재 개선 ledger

| 개선 | 변경 전 근거 | 변경 commit | 현재 근거 | 회고 수준 | 다음 조치 |
|---|---|---|---|---|---|
| performance fixture와 PostgreSQL `EXPLAIN (ANALYZE, BUFFERS)` | 대표 query에 전용 fixture·plan 검증 없음 | `c1f5c68` | `ReleaseCandidateQueryPlanTest`가 volunteer/report queue와 metric upsert를 검증 | B | `fb7241d` 이전 schema를 복원해 같은 SQL을 다시 실행 |
| 운영 queue 복합 index | queue query에 versioned composite index 없음 | `fb7241d` (`V051`) | 현재 migration과 plan test가 index 이름을 확인 | A 후보 | old/new schema에서 Medium fixture의 plan·실행시간 비교 |
| volunteer/report `LIMIT 100`과 안정 정렬 | unbounded list와 단일 timestamp 정렬 | `fb7241d` | `LIMIT 100`, `(starts_at|created_at, id)` 정렬과 V051 index | A 후보 | old/new HTTP 목록을 같은 VU로 비교 |
| publication feed keyset cursor | `f634b00` 이전 feed 구현 없음/계약 상이 | `f634b00` | `(createdAt, id)` cursor와 page boundary | B | legacy route adapter가 가능할 때만 offset 대비 replay |
| 작성자 게시글 공개범위 bulk update | 모든 entity를 읽어 `saveAll` | `dab5527` | 조건부 PostgreSQL bulk update와 `V053` index | A 후보 | 2만/10만 게시글에서 heap·query count·p95 비교 |
| 조회수 atomic upsert | `find → save`와 race 가능 | `c1f5c68` | `ON CONFLICT DO UPDATE`, 160 concurrent fixture | A 후보 | old endpoint와 같은 contention script로 lost update·p95 비교 |
| 목록 상한 확대 | 여러 repository/service가 무제한 반환 | `fb7241d` 및 이후 개선 | notification·comment·admin 등 대표 목록에 상한 | B | 전체 endpoint inventory에서 상한 누락을 찾고 선택 항목 replay |
| DB connection timeout | 운영 설정의 장애 반응 기준이 명시되지 않음 | 기존 `application.yml` 설정 | Hikari `connection-timeout: 3000` | C | pool saturation 실험에서 timeout과 recovery 확인 |
| graceful shutdown timeout | 종료 phase 상한 없음 | `7de4416` | graceful shutdown과 20초 phase limit | C | SIGTERM 중 진행 요청과 재기동 recovery 확인 |

## A 수준 historical replay 방법

### 1. 변경 전 버전 확인

Git history에서 변경 직전 commit을 고정한다.

```bash
git show --stat c1f5c68
git show --stat dab5527
git show --stat fb7241d
git show --stat f634b00
```

### 2. 변경 전 애플리케이션을 별도 build directory로 복원

현재 working tree를 바꾸지 않고 `build/performance/history` 아래에 source archive를 만든다. `build/`는 결과 산출물 위치이므로 source archive를 저장소에 commit하지 않는다.

```bash
rm -rf build/performance/history
mkdir -p build/performance/history/<commit>
git archive <commit> | tar -x -C build/performance/history/<commit>
```

변경 전 migration과 Java dependency가 현재와 다를 수 있으므로, old source의 Gradle Wrapper와 migration을 사용한다. old app이 현재 JDK·Docker 환경에서 기동하지 않으면 A 수준을 강제하지 않고 B 수준으로 기록한다.

### 3. 동일 조건 고정

before/after 사이에서 다음을 바꾸지 않는다.

- Java·Docker·PostgreSQL/PostGIS 버전
- CPU·RAM·container resource limit
- fixture row count와 ID 생성 규칙
- VU·stage·duration·warm-up
- request body·읽기 순서·connection pool 설정
- 실행 횟수와 결과 집계 규칙

변경 전 API 계약이 현재와 다르면 k6 script 전체를 복사해 수정하지 말고 version adapter를 둔다. adapter가 의미를 바꾸면 두 결과를 직접 비교하지 않는다.

### 4. 결과 표기

historical replay 결과는 다음처럼 구분한다.

```text
historical before: old commit에서 동일 조건으로 재현한 측정
current after: 현재 commit에서 동일 조건으로 재현한 측정
original before: 당시 실제로 측정한 값 (없으면 N/A)
```

`original before`가 없는데 historical replay가 성공하면 “당시 수치”가 아니라 “현재 환경에서 재구성한 변경 전 수치”라고 쓴다.

## 항목별 재현 판단

### A 후보: 반드시 before/after를 시도할 항목

#### 1. Bulk moderation update

변경 전 `findByAuthorMemberId → entity loop → saveAll`과 현재 bulk update는 경로가 명확히 다르다. 2만·10만 게시글 fixture에서 다음을 비교한다.

- HTTP p50/p95
- SQL query 수
- JVM heap/RSS
- transaction duration
- 실제 affected row 수

동일한 상태 row만 변경해야 하므로 이미 `HIDDEN`인 row를 다시 update하지 않는 조건도 확인한다.

#### 2. 조회수 atomic upsert

old endpoint와 current endpoint에 같은 publication을 대상으로 contention script를 보낸다.

- 8/16/32 concurrent VU
- 160/1,000 total requests
- 최종 `view_count`
- 5xx·deadlock·retry
- p95

old 구현이 lost update를 재현하지 않더라도 “재현되지 않음”을 기록한다. 테스트가 실패하도록 데이터를 조작하지 않는다.

#### 3. Queue index와 list bound

old migration으로 DB를 만들고 current와 같은 Medium fixture를 넣는다. `EXPLAIN`만 보지 않고 실제 실행시간·rows·buffers를 같이 저장한다.

현재 `ReleaseCandidateQueryPlanTest`는 `enable_seqscan = off`를 설정한다. 따라서 “planner가 실제 운영 조건에서 항상 이 index를 선택한다”는 증거가 아니라 “해당 index path가 유효하다”는 증거로만 표현한다. replay에서는 기본 planner 설정과 `enable_seqscan`을 켠 결과를 둘 다 기록한다.

### B 수준으로 우선 기록할 항목

#### 1. Keyset cursor

`f634b00` 이전에는 publication feed 모듈·계약이 달라 historical HTTP replay 비용이 크다. 우선 commit diff와 현재 Medium fixture의 page boundary·query plan을 기록한다. legacy offset 구현을 정확히 복원할 수 있을 때만 A 수준 비교를 추가한다.

#### 2. 목록 상한

`LIMIT 100` 적용은 무한 결과와 메모리·응답 payload의 상한을 제거한 구조적 개선이다. 과거 p95가 없으면 “응답시간이 몇 % 줄었다”고 쓰지 않고, before/after query shape와 최대 반환 row를 기록한다. 상한 누락 endpoint는 inventory로 별도 추적한다.

### C 수준으로 기록할 항목

DB connection timeout과 graceful shutdown은 정상 처리량을 높이는 기능이 아니다. pool 고갈·SIGTERM 상황에서 무한 대기와 partial shutdown을 막는 **resilience/capacity boundary**로 설명한다. 부하 결과에는 timeout 발생 시 recovery와 진행 요청 완료 여부를 기록한다.

## 결과 문서 연결

historical replay 결과는 일반 baseline과 같은 형식을 사용하되, 제목에 `historical-replay`를 붙인다.

```text
docs/performance/results/
├── <date>-historical-replay-publication-bulk-<old>-vs-<new>.md
├── <date>-historical-replay-metric-contention-<old>-vs-<new>.md
└── <date>-historical-replay-queue-index-<old>-vs-<new>.md
```

각 결과에는 다음을 반드시 포함한다.

- old commit와 current commit
- original before 값 또는 `N/A`
- reconstructed before인지 여부
- 동일 조건을 증명하는 command와 fixture hash
- before/after 수치와 편차
- old app 기동 실패·계약 차이·재현 불가 사유
- 성능 외 trade-off(정확성, stale, lock, 운영 복잡도)

## 면접에서의 정확한 표현

좋은 표현:

> “당시에는 before latency를 수집하지 않았습니다. 대신 변경 전 commit을 보존하고 같은 fixture와 부하를 재생해 재구성 기준선을 만들었습니다. 재현이 어려운 cursor와 shutdown 설정은 수치가 아닌 코드·운영 근거로 구분했습니다.”

피해야 할 표현:

- “기존보다 3배 빨라졌다” — 동일 조건의 before 수치가 없으면 금지
- “모든 목록이 최적화됐다” — inventory와 query 측정이 없으면 금지
- “Redis/Kafka 없이도 충분하다” — 실제 부하·운영 조건을 측정하지 않았다면 금지
