# 재현 가능한 릴리스 근거

G1~G8을 로컬·CI에서 재현할 수 있는 현재 릴리스 상태만 기록한다. 실제 VPS 공개 운영은 G9 범위다.

## 확인된 근거

- `./gradlew clean check migrationTest`: backend unit/integration, Modulith, parity inventory, Spotless와 migration test 전체 통과
- `scripts/validate-parity-matrix.sh`: 104개 page/API 행 중 `verified=95`, `excluded=9`, `pending=0` 확인
- `frontend/node_modules/.bin/tsc -p frontend/tsconfig.json --noEmit`: 타입 검사 통과
- `frontend/node_modules/.bin/vitest run`: 11개 파일, 35개 테스트 통과
- `frontend/node_modules/.bin/vite build --config vite.config.ts`: production bundle 생성 통과
- `corepack pnpm test:e2e`: Chromium desktop/mobile 54개 테스트 통과
- `migration/fixtures/logical-fixture.yaml`: guest/member/moderator와 Care·검색·신고 대표 시나리오를 고정된 logical fixture로 연결
- `src/test/java/com/townpet/care/CareControllerTest.java`: Care Request → Application → Assignment → Feedback 전체 상태 전이 통과
- `docker compose -f deploy/compose/portfolio.yml config`: 필요한 환경 변수를 주입한 VPS용 Compose 해석 성공
- `deploy/Caddyfile`: React history fallback과 `/api` reverse proxy를 단일 public entrypoint로 구성
- `TOWNPET_DOMAIN`을 설정하면 같은 Caddy 구성이 로컬 `:80` 또는 VPS 도메인의 자동 HTTPS site address로 동작한다.
- Portfolio Compose의 PostgreSQL 초기화 script가 `APP_DB_USER/PASSWORD` role을 생성하고 public schema 권한만 부여한다. backend가 별도 app role로 접속하는 것을 임시 production stack에서 확인했다.
- production backend/frontend image build와 임시 portfolio stack 기동 후 backend·PostgreSQL health, Caddy `/api/health` JSON 응답과 SPA history fallback을 확인했다. 테스트 stack은 검증 후 제거했다.
- `deploy/backup-portfolio.sh`: PostgreSQL·MinIO paired custom-format backup과 manifest checksum 제공
- `deploy/restore-portfolio.sh`: 명시적 `ALLOW_DESTRUCTIVE_RESTORE=YES` 없이는 실행되지 않는 paired 복구 명령 제공
- 임시 local Compose에서 `backup-portfolio.sh`로 PostgreSQL·MinIO paired backup을 만들고 `restore-portfolio.sh`로 별도 DB와 bucket에 복구한 뒤 row count·object checksum 확인까지 통과했다. 테스트 대상과 임시 object는 검증 뒤 제거했다.
- `docs/parity/matrix.yaml`: `pending` 0개. 현재 구현 근거가 있는 `verified` 95개와 ADR에 근거한 `excluded` 9개를 구분한다.

## 배포 전 작업 상태

| 항목 | 상태 | 배포 전 판단 |
|---|---|---|
| backend/frontend unit·build quality gate | 확인됨 | backend `clean check migrationTest`, frontend typecheck/Vitest/build 통과 |
| browser E2E/parity gate | 확인됨 | 최신 화면 계약·mock·visual snapshot·모바일 feed layout 반영 후 54개 전체 통과 |
| parity inventory | 확인됨 | `pending=0`; `excluded`는 ADR 범위 밖 기능 |
| local Docker migration·health·smoke | 확인됨 | 운영 환경과 혼동하지 않음 |
| local backup/restore | 확인됨 | VPS에서 동일 절차와 외부 보관 위치를 다시 검증 |
| VPS public workload 성능 | 미실행 | 배포 전에 실행 필요 |
| DNS·TLS·secure session cookie | 미실행 | 실제 domain을 정한 뒤 실행 필요 |
| production media | local/fresh-volume 구현·검증 | 실제 DNS/TLS/CORS와 browser flow 확인 필요 |
| demo seed·scoped sanitize | local dry-run·bootstrap rehearsal 완료 | production DB에서 1회 apply 필요 |
| email verification·password recovery | SMTP local integration·production adapter 구현 | 실제 provider TLS/deliverability 확인 필요 |
| 외부 관측·알림 | 최소 health/structured log/runbook 구현 | 외부 collector·장기 retention은 선택 사항 |

현재 코드상 production media는 private MinIO, presigned PUT/GET, finalize checksum 검증, owner authorization과 paired backup까지 구현됐다. 공개를 막는 남은 경계는 기능 구현이 아니라 실제 media domain DNS/TLS/CORS와 fresh-volume browser 검증이다. `TOWNPET_MINIO_PUBLIC_ENDPOINT`는 Caddy가 proxy하는 HTTPS host로 운영 env에 주입해야 한다.

demo 계정은 local fixture와 production 운영 데이터가 다르다. production은 공개 demo credential·콘텐츠를 사용하지 않고, 초기 migration 뒤 scoped sanitize와 private moderator bootstrap을 1회 실행한다. 실제 사용자의 회원가입과 개인정보 수집은 현재 제품 범위가 아니다.

## 현재 한계

- 실제 VPS에서 DNS/TLS 리허설은 아직 실행하지 않았다. 이번 복구 리허설은 임시 local Compose 환경이며, VPS 자격·DNS·TLS 상태를 대체하지 않는다.
- 배포 전 local runtime은 `SPRING_PROFILES_ACTIVE=local`에서 filesystem media adapter와 demo reset script를 사용한다. production은 SMTP와 private MinIO adapter를 사용한다.
- `excluded` 항목은 현재 제품 범위 밖이며 ADR에 근거를 둔다.
- local Docker 성능 측정과 bundle/Web Vital 경로는 확인했지만, local 수치를 운영 SLA나 VPS 처리량으로 주장하지 않는다.

## 최종 release gate 순서

```text
production env·media/demo/email 정책 확인
→ 새 Docker volume에서 migration·sanitize·health 확인
→ VPS에서 Caddy·TLS·session cookie 확인
→ public/member/mixed 성능과 resource 측정
→ backup 외부 보관·restore·rollback 확인
→ backend/frontend/browser 전체 gate 1회
→ 공개 전 demo/개인정보/로그 점검
```

이 문서의 `확인된 근거`는 local·CI evidence이고, `배포 전 작업 상태`의 미실행 항목을 완료된 운영 경험처럼 사용하지 않는다.

## 2026-08-13 사전 release gate 재실행

- `scripts/validate-release-candidate.sh`: parity 104개 중 `verified=95`, `excluded=9`, `pending=0`; performance script syntax도 통과했다.
- `./gradlew clean check migrationTest --no-daemon`: backend 전체 성공(5분 42초).
- frontend typecheck, Vitest 11개 파일·37개 테스트, production build와 bundle budget은 통과했다.
- Playwright는 feed breadcrumb/pagination UI 변경에 맞춰 visual snapshot을 갱신한 뒤 desktop/mobile **54개 전체 통과**했다. 변경된 feed 소스·pagination 회귀 테스트·snapshot은 `69c18b3`에 함께 고정했다.
- `scripts/security-static-check.sh`, 전체 shell syntax, Caddy 2.9 config validation은 통과했다. Trivy filesystem 재실행은 vulnerability DB 다운로드 중 Docker 내부 공간 부족으로 중단됐고, CI의 Trivy/dependency-review/image scan 결과가 공개 전 최종 증거가 되어야 한다.

따라서 이 시점의 저장소 내부 release candidate는 backend·frontend·parity·기능 E2E gate를 통과했다. 남은 저장소 외부 또는 CI 의존 항목은 GitHub Actions의 보안 scan 결과와 실제 VPS/SMTP/media/backup 환경 검증이며, 이를 local 성공으로 대체하지 않는다.
