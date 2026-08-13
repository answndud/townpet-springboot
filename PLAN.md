# PLAN.md

## Goal

배포를 시작하기 전에 저장소 안에서 완성할 수 있는 기능·보안·복구·관측·성능 검증을 모두 닫아 **production release candidate**를 만든다. 실제 VPS의 DNS/TLS, 외부 SMTP 발송, 외부 백업 보관과 공개 트래픽 측정은 배포 단계에서 수행하되, 그 절차와 실패 시 대응은 이 계획에서 재현 가능하게 준비한다.

## Active

### P1 - production 경계와 로컬 운영 흐름을 닫는다

1. P1.1 - production 환경·media·email 설정을 fail-fast 상태로 고정한다
   - 파일: `deploy/compose/portfolio.yml`, `deploy/portfolio.env.example`, `scripts/validate-portfolio-env.sh`, `src/main/resources/application-production.yml`, 관련 설정 테스트
   - 변경: placeholder·비HTTPS URL·비활성 SMTP·root credential 사용·공개 media 기본값이 시작 전에 실패하는지 확인하고, backend가 MinIO application credential과 SMTP production adapter만 사용하도록 정리한다. 실제 secret은 추가하지 않는다.
   - 검증: `bash scripts/validate-portfolio-env.sh`, `docker compose -f deploy/compose/portfolio.yml config`(dummy env), 관련 backend 설정 테스트
   - 완료: 잘못된 production env가 조용히 기동하지 않고, 올바른 env의 Compose 구성이 backend·PostgreSQL·MinIO·Caddy·SMTP 경계를 명확히 표현한다.

2. P1.2 - media 업로드와 이메일 인증·복구의 fresh-volume 로컬 흐름을 끝까지 재현한다
   - 파일: `deploy/compose/portfolio.yml`, `deploy/compose/smtp-local.yml`, `deploy/Caddyfile`, `docs/runbooks/email-local.md`, media·identity 테스트 및 필요한 smoke script
   - 변경: 새 volume에서 회원 인증, SMTP Mailpit 수신, token 링크, presigned PUT/GET/delete, checksum/finalize, owner 차단, Caddy media CORS를 한 흐름으로 점검한다. 실패·만료·재사용 token과 잘못된 MIME도 핵심 경계만 회귀 테스트한다.
   - 검증: `docker compose ... up -d` 기반 local SMTP/media rehearsal, `caddy validate`, `./gradlew ...`의 관련 테스트, `corepack pnpm test:e2e`의 media/auth 흐름
   - 완료: local filesystem adapter가 아니라 production과 같은 SMTP·private MinIO 경로가 fresh volume에서 정상 성공하고, private object가 공개 URL로 노출되지 않는다.

3. P1.3 - demo·개인정보·권한 경계를 배포 가능한 초기 상태로 고정한다
   - 파일: `scripts/seed-local-community-demo.sh`, `scripts/sanitize-production-demo.sh`, `scripts/bootstrap-private-moderator.sh`, `docs/runbooks/deploy-update.md`, `docs/runbooks/secret-rotation.md`, 관련 권한 테스트
   - 변경: local demo seed와 production sanitize를 분리하고, 공개 credential·관리자 계정·실제 개인정보가 production seed에 들어가지 않도록 한다. MEMBER/MODERATOR의 공개 범위, ADMIN/OPERATOR 비공개, reset의 범위·확인 문구·감사 로그를 문서와 스크립트에서 일치시킨다.
   - 검증: seed dry-run → rollback → verification, sanitize dry-run 및 scoped apply rehearsal, 역할별 API 권한 테스트
   - 완료: 새 DB를 초기화했을 때 공개 가능한 합성 데이터만 남고, reset·bootstrap이 다른 사용자 데이터나 관리자 자격을 훼손하지 않는다.

### P2 - 장애·보안·복구 운영을 실제 실행 가능한 수준으로 만든다

4. P2.1 - backup/restore와 rollback/restart 절차를 fresh 환경에서 재현한다
   - 파일: `deploy/backup-portfolio.sh`, `deploy/restore-portfolio.sh`, `deploy/backup-postgres.sh`, `deploy/restore-postgres.sh`, `docs/runbooks/README.md`, `docs/runbooks/rollback.md`, `docs/runbooks/incident-restart.md`
   - 변경: PostgreSQL·MinIO paired backup manifest/checksum, 별도 restore 대상, destructive confirmation, application-only rollback과 data rollback의 경계를 점검한다. migration 실패·backend crash·MinIO 불능 시 판단 순서를 명확히 한다.
   - 검증: 임시 Compose와 새 volume에서 backup → checksum → restore → row/object 비교 → rollback rehearsal; 실행 로그와 소요 시간을 `docs/report/release-readiness.md`에 한 번 기록
   - 완료: 운영자가 명령을 추측하지 않고 복구할 수 있으며, 실행하지 않은 외부 보관·RPO/RTO는 미완료로 명시된다.

5. P2.2 - 관측·로그·secret 노출 방어와 rate-limit 경계를 점검한다
   - 파일: `docs/runbooks/observability.md`, `docs/runbooks/secret-rotation.md`, `src/main/java/com/townpet/common/RequestTraceFilter.java`, rate limiter·error handler·Caddy 설정 및 테스트
   - 변경: credential/session/token/exact location이 log·trace·error body에 없는지 확인하고 health·SMTP failure·backup·rate-limit 신호를 운영자가 찾을 수 있게 한다. X-Forwarded-For 신뢰 범위와 단일 인스턴스 limiter 한계를 문서화한다.
   - 검증: `bash scripts/security-static-check.sh`, `bash -n scripts/*.sh deploy/*.sh`, 의도적 401/403/429/5xx 시나리오 확인, Caddy config validation
   - 완료: 보안 이벤트와 장애 원인을 추적할 수 있지만 민감정보가 로그로 유출되지 않고, 다중 인스턴스 한계가 숨겨지지 않는다.

6. P2.3 - 공급망·컨테이너·릴리스 입력을 자동 차단한다
   - 파일: `.github/workflows/security.yml`, `.github/workflows/main.yml`, `deploy/Dockerfile.backend`, `deploy/Dockerfile.frontend`, `.gitignore`, `scripts/security-static-check.sh`
   - 변경: non-root runtime, dependency/secret/image scan, SBOM, dependency review, build artifact와 env 입력 검사를 CI에서 실행한다. action 버전과 scan 실패 시 release가 중단되는지 확인한다.
   - 검증: `bash scripts/security-static-check.sh`, `git diff --check`, local Trivy filesystem/image scan 가능 범위, GitHub Actions 실행 결과
   - 완료: secret·고위험 dependency·root image·잘못된 production 설정이 release gate를 통과하지 못한다.

### P3 - 최종 성능 기준선과 release candidate gate를 닫는다

7. P3.1 - 마지막 코드 상태에서 대표 성능 workload를 재측정한다
   - 파일: `scripts/performance/`, `docs/performance/results/`, `docs/performance/README.md`, 필요 시 query/index 테스트
   - 변경: final commit과 동일한 fixture로 S1/S2 public/member read, S3 write, S4 contention, S5 moderator, S6 media, S7 mixed, S8 spike/30분 soak을 실행한다. p50/p95/p99·throughput·error·DB connection·CPU/memory를 분리해 기록하고 local 결과를 운영 SLA로 표현하지 않는다.
   - 검증: `scripts/performance/prepare.sh`, `scripts/performance/run.sh`, `scripts/performance/validate.sh`와 `ReleaseCandidateQueryPlanTest`
   - 완료: 마지막 변경으로 성능 회귀가 없고, feed index·atomic view·capacity lock·query cap의 근거와 남은 한계가 최신 문서에 반영된다.

8. P3.2 - 보안·backend·frontend·parity 전체 gate를 한 번 실행한다
   - 파일: `scripts/validate-release-candidate.sh`, `docs/parity/matrix.yaml`, `docs/report/release-readiness.md`, 기존 테스트 설정
   - 변경: 기능 구현을 더 추가하지 않고 release candidate 기준을 고정한다. parity `pending=0`, backend migration/test/coverage, frontend typecheck/unit/build, browser E2E, smoke, security static check, Compose/Caddy 검증 결과를 한 묶음으로 남긴다.
   - 검증: `./scripts/validate-release-candidate.sh`, 필요 시 저장소 기본 gate 전체 명령
   - 완료: 현재 commit을 기준으로 재현 가능한 모든 저장소 내부 gate가 통과하고, 실패 항목은 공개 전 blocker로 남는다.

9. P3.3 - 배포 직전 체크리스트와 증거 문서를 최종 동기화한다
   - 파일: `docs/report/release-readiness.md`, `docs/report/security-review.md`, `docs/performance/README.md`, `docs/runbooks/README.md`, `docs/runbooks/external-production-checklist.md`, `docs/report/interview-prep.md`
   - 변경: 실제 실행한 결과만 evidence로 갱신하고, VPS DNS/TLS·외부 SMTP deliverability·media domain CORS·offsite backup·public workload처럼 아직 외부에서 해야 하는 항목은 명시적 blocker로 유지한다. Redis/Kafka는 병목 증거가 없으면 deferred로 유지한다.
   - 검증: 문서의 명령과 코드 경로 교차 확인, `rg -n "pending|미실행|deferred"`로 상태 점검, `git diff --check`
   - 완료: 초보자도 “무엇이 완료됐고 무엇을 VPS에서 해야 하는지” 구분할 수 있으며, 배포를 시작해도 체크리스트 누락으로 기능이 조용히 503이 되지 않는다.

## Backlog

- 실제 VPS DNS/TLS·Caddy forwarded-header·secure cookie·edge rate limit 검증
- 실제 SMTP provider TLS/SPF/DKIM/deliverability 검증
- 외부 failure domain에 backup 보관 후 restore와 RPO/RTO 측정
- 실제 VPS에서 동일 workload와 CPU/memory/disk/DB connection을 측정
- Redis/Kafka는 DB saturation, cache miss 병목, notification/projection backlog가 재현될 때만 별도 실험

## Working rules

- 한 slice는 기능·운영 경계가 연결된 vertical slice로 진행하고, 파일 단위 작업과 반복적인 전체 gate를 만들지 않는다.
- 구현 중에는 가장 가까운 컴파일·기능 검증만 실행하고, P3.2에서 전체 gate를 한 번 실행한다.
- report는 새로운 설계 판단·실패 원인·재현 가능한 수치가 생길 때만 갱신한다.
- 적용된 Flyway migration은 수정하지 않고 새 migration 또는 명시적 운영 스크립트를 추가한다.
- 실제로 실행하지 않은 외부 배포·백업·SMTP·성능 결과를 완료했다고 기록하지 않는다.
