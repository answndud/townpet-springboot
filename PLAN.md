# PLAN.md

## Goal

공개 배포 전 성능 판단을 끝낸다. 현재 PostgreSQL 기준선과 이미 적용한 개선을 근거로, Redis·Kafka를 실제로 추가할지 또는 보류할지를 결정하고 남은 배포 전 검증만 실행한다.

## Active

1. **VPS 환경에서 대표 workload를 재측정한다.**
   - 파일: `docs/performance/results/`, `scripts/performance/`
   - 변경: local Docker와 분리된 환경에서 public/member/mixed 대표 시나리오를 동일 fixture·profile로 실행하고 DB CPU, connection, lock wait, JVM 자원을 함께 수집한다.
   - 검증: 기존 k6 profile과 `scripts/performance/validate.sh`; unexpected 5xx·timeout·무결성 위반을 확인한다.
   - 완료: 운영 후보 환경의 병목과 local 결과의 차이가 결과 문서에 기록된다.

2. **Redis·Kafka 도입 조건을 실제 관측값으로 갱신한다.**
   - 파일: `docs/performance/redis-kafka-evaluation.md`, `docs/performance/results/`
   - 변경: VPS에서 병목이 재현될 때만 한 후보를 구현하고 `postgres-only`/`candidate-enabled`/`failure-mode`/`rollback`을 같은 조건으로 비교한다. 병목이 없으면 현재 4차 판단을 유지한다.
   - 검증: p50/p95/p99, DB·broker 자원, stale·duplicate·fallback·lag.
   - 완료: 기술별 `accepted` 또는 `deferred`가 수치와 trade-off로 확정된다.

3. **배포 직전 release gate를 실행한다.**
   - 파일: `deploy/`, `docs/runbooks/`, `docs/performance/results/`
   - 변경: 새 Docker volume에서 migration·seed·health·backup/restore·대표 smoke를 검증하고 rollback 절차를 확인한다.
   - 검증: 큰 phase 종료 시 전체 gate와 배포 체크리스트를 한 번 실행한다.
   - 완료: 애플리케이션 배포와 복구 절차가 재현 가능하고, 측정하지 않은 운영 SLA를 주장하지 않는다.

## Backlog

- `docs/report/technical-notes.md`에 최종 성능 판단과 남은 제한을 요약한다.
- 실제 트래픽·DB saturation·후속 이벤트 backlog가 생길 때만 Redis/Kafka를 정식 운영 구성으로 승격한다.
- G9: Hetzner VPS, DNS/TLS/Caddy, 외부 object storage/SMTP, monitoring/alerting.
