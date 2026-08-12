# PLAN.md

## Goal

공개 배포 전에 TownPet 백엔드의 성능을 재현 가능한 부하로 측정하고, 병목을 한 번에 하나씩 개선한다. 최종 완료는 대표 사용자 흐름·상태 경합·혼합 부하에서 수치와 DB 무결성 근거가 문서화되고, Redis·Kafka 도입 여부가 측정 결과와 운영 trade-off로 결정된 상태다.

성능 계획·시나리오·결과 형식은 [`docs/performance/README.md`](docs/performance/README.md)를 기준으로 한다. S0~S8 대표 workload와 S0~S2 3회 반복 baseline, 공개 feed 인덱스 before/after가 측정됐다. 기존 query-plan·동시성 테스트를 HTTP 부하 결과로 표현하지 않는다.

## Active

### P1 - 부하 테스트 기반을 구현한다

1. **기존 성능 개선의 historical replay 대상을 확정한다.**
   - 파일: `docs/performance/retrospective.md`, `src/test/java/com/townpet/performance/`, Git history
   - 변경: 변경 전 commit을 A/B/C 증거 수준으로 분류하고, bulk update·metric contention·queue index를 우선 replay 대상으로 고정한다. 재현 불가 항목은 원인과 정성 근거를 기록한다.
   - 검증: old/current source archive가 같은 fixture·환경으로 실행 가능한지 가장 작은 smoke로 확인한다.
   - 완료: 당시 수치와 재구성 수치가 혼동되지 않고, 첫 baseline 전에 replay 순서가 정해진다.

2. **k6 실행 경로와 perf profile을 추가한다.**
   - 파일: `loadtest/`, `scripts/performance/`, `src/main/resources/application-perf.yml`, `deploy/compose/`
   - 변경: 전용 PostgreSQL DB·합성 계정·metrics 접근·reset 경로를 만들고, 일반 local/demo/production 설정과 분리한다. 실행 전 환경과 데이터 규모를 출력한다.
   - 검증: `./scripts/performance/validate.sh` 또는 구현된 가장 가까운 smoke command.
   - 완료: 빈 perf DB에서 준비→기동→reset을 반복할 수 있고 secret·demo data를 재사용하지 않는다.

3. **deterministic performance fixture를 만든다.**
   - 파일: `migration/` 또는 `scripts/performance/`, `docs/performance/methodology.md`
   - 변경: Small(2천), Medium(2만), 선택 Large(10만) 규모의 게시글·신고·volunteer·member 데이터를 고정 ID 규칙으로 생성한다.
   - 검증: reset 후 같은 seed를 두 번 실행해 row count와 주요 index/query plan을 비교한다.
   - 완료: demo seed와 분리된 같은 데이터가 매 실행 재현된다.

### P2 - 시나리오별 기준선을 측정한다

1. **S0~S2를 실행한다.**
   - 범위: smoke, public read, member read
   - 검증: 1 VU baseline → 5 VU calibration → 10/20/40 VU ramp. 3회 반복 후 p50/p95/p99·처리량·오류율·DB pool을 기록한다.
   - 현재: smoke·public/member baseline을 각 3회 반복하고 p99·resource snapshot을 기록했다. 결과는 [2026-08-12-s0-s2-baseline.md](docs/performance/results/2026-08-12-s0-s2-baseline.md)에 있다.

2. **S3~S6을 실행한다.** ✅
   - 범위: write burst, 상태 경합, moderator/admin, media I/O
   - 검증: 예상된 4xx와 unexpected 5xx를 분리하고, 최종 DB row·version·capacity·중복 상태를 대사한다.
   - 완료: [2026-08-12-s3-s8-workloads.md](docs/performance/results/2026-08-12-s3-s8-workloads.md)에 정상 처리량·데이터 불변식·conflict 수렴 결과를 기록했다.

3. **S7~S8을 실행한다.** ✅
   - 범위: mixed workload, soak/spike
   - 검증: 안정 부하 30~60분과 순간 증가 후 회복을 확인한다.
   - 완료: 20 VU spike와 30분 soak의 p99·앱/DB resource·JVM heap 결과를 기록했다. Hikari 시계열은 N/A로 남겼고, spike의 Docker bridge timeout은 환경 제한으로 분리했다.

### P3 - 측정 결과로 개선하고 확장 기술을 판단한다

1. **병목을 하나씩 수정한다.** ✅
   - 파일: 병목이 확인된 service/repository/migration과 `docs/performance/results/`
   - 변경: query/index/pagination/transaction/pool 중 원인이 확인된 한 항목만 수정하고 동일 조건으로 before/after를 비교한다.
   - 검증: 수정 전후 동일 fixture·VU·duration·warm-up 재실행.
   - 완료: V054 feed index before/after와 row-lock capacity trade-off를 결과 문서에 남겼다. 2차에서 login 비용을 분리한 capacity 전용 metric으로 실제 application p95를 재측정하고, 3차에서 lock query projection을 줄여 전후 경합을 비교했다. 결과는 [phase2](docs/performance/results/2026-08-13-phase2-capacity-diagnostics.md)와 [phase3](docs/performance/results/2026-08-13-phase3-capacity-query-shape.md)에 있다.

2. **Redis 후보를 평가한다.** `deferred`
   - 파일: `docs/performance/redis-kafka-evaluation.md`, 후보 구현과 관련 test
   - 변경: 반복 public read 또는 rate-limit counter처럼 근거가 있는 한 지점만 실험한다. hit/miss·stale·invalidation·fallback을 함께 측정한다.
   - 검증: `postgres-only`와 `candidate-enabled`를 동일 조건으로 비교하고 Redis 장애·eviction도 확인한다.
   - 완료: feed/read p95와 DB 자원에서 캐시 병목이 입증되지 않아 deferred로 기록했다.

3. **Kafka 후보를 평가한다.** `deferred`
   - 파일: `docs/performance/redis-kafka-evaluation.md`, event producer/consumer와 idempotency test
   - 변경: request latency를 실제로 차지하는 eventual-consistency 후속 작업만 후보로 분리한다. 핵심 source transaction은 PostgreSQL에 남긴다.
   - 검증: 동기 처리와 비교해 HTTP p95, end-to-end 완료시간, consumer lag, 중복·재처리·broker 장애를 확인한다.
   - 완료: request 후속 작업 backlog가 입증되지 않아 PostgreSQL event publication을 유지하고 deferred로 기록했다.

## Backlog

- 최종 성능 결과와 남은 제한을 `docs/report/technical-notes.md`에 요약한다.
- 배포 직전 새 Docker volume에서 migration·seed·health·대표 smoke를 실행한다.
- G9: Hetzner VPS, DNS/TLS/Caddy, 외부 object storage/SMTP, backup/restore, monitoring/alerting.
- 실제 트래픽·독립 팀 소유권·확장 요구가 생길 때만 Redis/Kafka를 정식 운영 구성으로 승격한다.

## 완료 판정

S0~S8 중 현재 제품 범위에 해당하는 시나리오의 결과 문서가 있고, 최소 3회 반복 기준선과 개선 전후 비교가 재현되며, unexpected 5xx·timeout·데이터 불변식 위반이 없거나 원인과 제한이 명시되어야 한다. 현재 이 조건을 충족했지만 local Docker 결과를 운영 SLA로 주장하지 않는다. spike bridge timeout과 soak RSS 증가 추적은 배포 전 VPS 재측정 backlog다.
