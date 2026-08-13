# PLAN.md

## Goal

배포 전 TownPet의 실제 기능·보안·운영 경계를 완성한다. 공개 환경은 실제 개인정보와 공개 demo 계정을 사용하지 않는 portfolio sandbox로 유지한다. SMTP 계정 복구와 private media는 실제 production 경로로 구현하고, Redis·Kafka 같은 확장은 측정된 병목이 생길 때만 검토한다.

## Active

1. **운영 경계 감사 결과를 반영한다.** ✅
   - demo sanitize가 익명 telemetry를 삭제하지 않도록 범위를 제한하고, private moderator bootstrap이 중복 email을 안전하게 거절하도록 유지한다.
   - account token 발급은 계정별 시간당 상한을 적용한다. 응답은 계속 generic `202`로 유지해 email enumeration을 막는다.
   - 완료 조건: 스크립트 syntax·컴파일·identity 회귀 테스트가 통과하고 report에 실제 변경 근거가 남는다. (완료)

2. **외부 운영 전제의 마지막 확인 목록을 닫는다.**
   - SMTP provider의 TLS/SPF/DKIM·deliverability, MinIO public domain의 DNS/TLS/CORS, VPS offsite backup retention은 실제 운영 계정과 호스트에서 확인한다.
   - 로컬에서 증명할 수 없는 항목은 구현 완료로 포장하지 않고 [`docs/runbooks/external-production-checklist.md`](docs/runbooks/external-production-checklist.md)에 operator checklist로 남긴다.

3. **최종 release gate를 한 번 실행한다.** (backend/frontend unit gate 완료, browser gate 미완료)
   - 최신 backend/frontend 변경을 고정한 뒤 browser E2E의 오래된 제목·mock·visual snapshot 기준선을 현재 화면 계약에 맞추고, 54개 전체를 재실행한다.
   - 결과와 재현 명령, 남은 외부 전제를 `docs/report/production-readiness-reassessment.md`에 갱신한다. 실제 공개 판정은 외부 운영 체크리스트가 채워진 뒤 별도로 한다.

## Deferred (trigger가 생길 때만)

- Redis: DB/cache/session 병목이 반복 재현될 때
- Kafka: PostgreSQL event publication으로 감당할 수 없는 외부 consumer·처리량이 생길 때
- Elasticsearch/SearchDocument: 검색 corpus·latency·정확도가 PostgreSQL 기준을 넘을 때
- 개인화 ranking projection: 실제 ranking 요구와 refresh 비용이 생길 때
- Kubernetes/microservice, 고급 WAL/PITR/HA, social login, 실제 public signup
- Marketplace 안전 규칙: public listing과 실제 사용자 입력을 열 때
- Hetzner 실제 DNS/TLS 배포: 로컬 release rehearsal이 끝난 뒤

## Working rules

- 기능은 작은 파일 단위가 아니라 충분한 vertical slice로 진행한다.
- 구현 중에는 가장 가까운 컴파일·기능 테스트만 실행하고, 큰 phase 종료 때 full gate를 실행한다.
- 중요한 결정·실패 원인·재현 가능한 수치만 `docs/report/`에 기록한다.
- 적용된 Flyway migration은 수정하지 않고 새 migration 또는 명시적 운영 스크립트를 추가한다.
