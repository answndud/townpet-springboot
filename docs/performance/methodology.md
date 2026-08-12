# 성능 측정 방법론

## 1. 목적과 범위

목표는 “빠르다”는 인상을 만드는 것이 아니라, TownPet 백엔드가 어떤 부하에서 어떤 비용을 보이는지 재현하고 병목을 수정하는 것이다.

측정 범위는 다음과 같다.

- Spring Boot HTTP 응답시간과 처리량
- PostgreSQL query plan·connection·lock·transaction 비용
- session·CSRF·RBAC를 포함한 실제 사용자 요청 경로
- 대량 목록 조회와 상태 변경의 메모리·flush 비용
- 동시 요청에서 데이터 불변식과 오류 응답의 안정성
- Redis·Kafka 도입 전후의 latency·throughput·일관성·운영 복잡도

다음은 초기 범위가 아니다.

- 실제 공개 사용자의 traffic 예측
- 네트워크 CDN·DNS·TLS·object storage 성능
- Redis/Kafka를 먼저 설치한 뒤 효과를 추정하는 실험
- 측정하지 않은 최대 사용자 수나 SLA 주장

## 2. 측정 계층

### L0 - 정적·query 검증

현재 이미 일부 구현되어 있다.

- `EXPLAIN (ANALYZE, BUFFERS)`로 대표 queue index 확인
- atomic upsert 동시성 확인
- migration·constraint·architecture gate

L0는 query plan과 데이터 안전성을 확인하지만 HTTP 처리량을 의미하지 않는다.

### L1 - 단일 요청 기준선

1 VU 또는 순차 호출로 애플리케이션의 최소 비용을 측정한다. warm-up과 cold start를 분리한다.

### L2 - HTTP 시나리오 부하

k6로 실제 HTTP 요청을 실행한다. 로그인, session cookie, CSRF, 권한, DB를 우회하지 않는다. 고정 fixture에서 public·member·moderator 요청을 실행한다.

### L3 - 동시성·상태 경합

같은 resource를 의도적으로 공유해 row lock, unique constraint, optimistic version, 409 수렴을 확인한다. 성공률만 보지 않고 최종 DB row와 상태를 대사한다.

### L4 - 장시간·혼합 부하

S7 mixed workload와 S8 soak/spike를 phase 종료 시 실행한다. 매 commit마다 반복하지 않는다.

## 3. 도구와 실행 환경

### 도구

- HTTP 부하: k6
- 애플리케이션: Spring Boot 실행 jar 또는 Docker backend image
- DB: PostgreSQL/PostGIS Docker container
- 리소스: `docker stats`, JVM/Actuator metrics, PostgreSQL system view
- query: `EXPLAIN (ANALYZE, BUFFERS)`, 필요 시 `pg_stat_activity`

Grafana, Prometheus, Gatling, Redis, Kafka는 첫 기준선에 포함하지 않는다. 먼저 k6·로그·PostgreSQL만으로 병목을 재현한다.

### 실행 profile

일반 `local`·`test`·`production`과 분리된 `perf` profile을 사용한다.

`perf` profile의 원칙:

- 전용 DB와 전용 application role 사용
- 합성 계정과 합성 콘텐츠만 사용
- metrics endpoint는 localhost 또는 내부 Docker network에서만 노출
- demo 계정과 production secret을 재사용하지 않음
- 테스트 종료 후 DB volume을 삭제하거나 명시적으로 reset
- 로그에는 credential·session·token·정확 위치를 남기지 않음

예정된 실행 진입점은 다음과 같다. 구현 전에는 명령이 존재한다고 주장하지 않는다.

```bash
./scripts/performance/prepare.sh --scale medium
./scripts/performance/run.sh --scenario public-read --profile medium
./scripts/performance/collect.sh --run-id <run-id>
./scripts/performance/reset.sh
```

## 4. 데이터 규모

모든 규모는 deterministic seed와 동일한 ID 규칙을 사용한다.

| 규모 | 대표 데이터 | 목적 |
|---|---:|---|
| Small | 게시글·신고·volunteer 각 2,000건 | 스크립트와 계약 확인 |
| Medium | 대표 목록 데이터 각 20,000건 | 첫 기준선과 query 비교 |
| Large | 대표 read 테이블 각 100,000건 | index·pagination 민감도 확인 |

Small과 Medium을 우선 실행한다. Large는 Medium에서 query plan 또는 메모리 문제가 드러난 목록에만 적용한다.

demo seed는 브라우저 기능 확인용이고 performance seed는 부하 전용이다. 두 seed를 섞지 않는다.

## 5. 부하 단계와 반복 규칙

각 시나리오는 다음 기본 단계를 따른다.

1. Smoke: 1 VU, 30초
2. Baseline: 1 VU, 2분
3. Calibration: 5 VU, 3분
4. Ramp: 10 → 20 → 40 → 80 VU, 단계별 5분
5. Soak: 선택된 안정 부하, 30~60분
6. Spike: 안정 부하에서 목표 VU로 급증

첫 실행은 Smoke와 Baseline만으로 스크립트·fixture·권한 오류를 제거한다. Ramp 이상은 baseline이 기록된 뒤 실행한다.

각 baseline과 개선 실험은 최소 3회 실행한다. p95를 단순 평균하지 않고 각 실행값, 중앙값, 편차를 함께 기록한다. 같은 조건을 재현할 수 없으면 before/after 비교를 승인하지 않는다.

## 6. 측정 항목

### HTTP 지표

- 요청 수, iteration 수, 처리량
- p50/p90/p95/p99
- timeout과 connection error
- status별 성공·예상 실패·예상 밖 실패
- scenario·endpoint별 latency

### JVM·Spring 지표

- process CPU·RSS·heap·GC pause
- live thread와 executor queue
- Hikari active/idle/pending/timeout
- request duration과 `X-Trace-Id` 연계 로그

### PostgreSQL 지표

- active/idle connection과 pool 대기
- lock wait·deadlock
- transaction 수와 rollback
- buffer hit·rows read·temporary file
- slow query와 query plan

### 무결성 지표

- 중복 reaction/bookmark/follow row
- volunteer capacity 초과
- care assignment 중복
- report open case 중복
- version lost update
- 이벤트 중복 처리

## 7. 초기 판정 기준

아래는 첫 local Docker baseline을 위한 **잠정 기준**이다. baseline과 실제 목적이 맞지 않으면 변경 이유를 결과 문서에 남긴다.

- 예상 밖 5xx: 0%
- timeout: 0%
- 데이터 불변식 위반: 0건
- public/member read p95: 300ms 이하
- 일반 write p95: 500ms 이하
- moderator bulk p95: 1초 이하
- Hikari pool timeout: 0건
- soak 중 지속적인 heap 증가: 없음

예상된 401/403/404/409는 각 scenario가 의도한 결과이면 실패로 세지 않는다. 단, 예상한 비율보다 급증하면 별도 결함으로 기록한다.

VPS에서는 다음을 분리한다.

- 서버 로그의 request duration: 애플리케이션 처리시간
- k6 `http_req_duration`: 네트워크 포함 사용자 관점 시간

## 8. 최적화 루프

```text
동일 fixture 준비
→ baseline 실행
→ 느린 endpoint/query와 resource saturation 확인
→ 가장 작은 변경 1개 적용
→ 동일 조건 재실행
→ before/after와 trade-off 기록
→ 다음 병목으로 이동
```

한 번에 index·캐시·비동기 이벤트를 모두 추가하지 않는다. 무엇이 효과를 냈는지 분리할 수 없기 때문이다.

## 9. Redis·Kafka 비교 원칙

Redis와 Kafka는 성능 개선 후보이지 기본 구성요소가 아니다.

### Redis 후보

- 반복되는 read projection 또는 짧은 TTL catalog cache
- rate limit counter
- 분산 lock이 실제로 필요한 경우

검증할 항목:

- cache hit/miss와 stale window
- invalidation 누락
- DB 직접 조회 대비 p95·DB load 변화
- Redis 장애·eviction 시 안전한 fallback

### Kafka 후보

- notification/event 후속 처리가 request latency를 실제로 지연시키는 경우
- 재시도·consumer lag·fan-out이 PostgreSQL transaction 범위를 넘어서는 경우

검증할 항목:

- producer 성공 후 DB commit과의 일관성
- consumer lag·중복·순서·재처리
- 동기 처리 대비 사용자 응답시간과 end-to-end 완료시간
- broker 운영 비용과 장애 시 backlog 복구

기준선보다 p95가 좋아졌다는 이유만으로 도입을 승인하지 않는다. 데이터 일관성, 장애 복구, 운영 복잡도까지 포함해 판단한다.
