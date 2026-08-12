# 재현 가능한 릴리스 근거

G1~G8을 로컬·CI에서 재현할 수 있는 현재 릴리스 상태만 기록한다. 실제 VPS 공개 운영은 G9 범위다.

## 확인된 근거

- `./gradlew clean check migrationTest`: backend unit/integration, Modulith, parity inventory, Spotless와 migration test 전체 통과
- `scripts/validate-parity-matrix.sh`: 104개 page/API 행 중 `verified=95`, `excluded=9`, `pending=0` 확인
- `frontend/node_modules/.bin/tsc -p frontend/tsconfig.json --noEmit`: 타입 검사 통과
- `frontend/node_modules/.bin/vitest run`: 7개 파일, 14개 테스트 통과
- `frontend/node_modules/.bin/vite build --config vite.config.ts`: production bundle 생성 통과
- `frontend/e2e/parity-shell.spec.ts`: Chromium desktop/mobile shell smoke 4개 통과
- `docker compose -f deploy/compose/portfolio.yml config`: 필요한 환경 변수를 주입한 VPS용 Compose 해석 성공
- `deploy/Caddyfile`: React history fallback과 `/api` reverse proxy를 단일 public entrypoint로 구성
- `TOWNPET_DOMAIN`을 설정하면 같은 Caddy 구성이 로컬 `:80` 또는 VPS 도메인의 자동 HTTPS site address로 동작한다.
- Portfolio Compose의 PostgreSQL 초기화 script가 `APP_DB_USER/PASSWORD` role을 생성하고 public schema 권한만 부여한다. backend가 별도 app role로 접속하는 것을 임시 production stack에서 확인했다.
- production backend/frontend image build와 임시 portfolio stack 기동 후 backend·PostgreSQL health, Caddy `/api/health` JSON 응답과 SPA history fallback을 확인했다. 테스트 stack은 검증 후 제거했다.
- `deploy/backup-postgres.sh`: PostgreSQL custom-format dump 재현 명령 제공
- `deploy/restore-postgres.sh`: 명시적 `ALLOW_DESTRUCTIVE_RESTORE=YES` 없이는 실행되지 않는 복구 명령 제공
- 임시 Compose PostgreSQL에서 `backup-postgres.sh`로 dump를 만들고 `restore-postgres.sh`로 복구한 뒤 `SELECT 1` 확인까지 통과했다. 테스트 컨테이너와 volume은 제거했다.
- `docs/parity/matrix.yaml`: `pending` 0개. 현재 구현 근거가 있는 `verified` 95개와 ADR에 근거한 `excluded` 9개를 구분한다.

## 현재 한계

- 실제 VPS에서 DNS/TLS 리허설은 아직 실행하지 않았다. 이번 복구 리허설은 임시 local Compose 환경이며, VPS 자격·DNS·TLS 상태를 대체하지 않는다.
- `excluded` 항목은 현재 제품 범위 밖이며 ADR에 근거를 둔다.
- 성능 수치는 bundle 생성과 Web Vital 수집 경로만 확인했으며, SLA나 처리량을 주장하지 않는다.
