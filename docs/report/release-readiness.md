# 출시 준비 근거

G1~G6을 큰 사용자 여정으로 연결한 뒤 현재 확인된 출시 준비 상태만 기록한다.

## 확인된 근거

- `./gradlew clean check migrationTest`: backend unit/integration, Modulith, parity inventory, Spotless와 migration test 전체 통과
- `frontend/node_modules/.bin/tsc -p frontend/tsconfig.json --noEmit`: 타입 검사 통과
- `frontend/node_modules/.bin/vitest run`: 7개 파일, 14개 테스트 통과
- `frontend/node_modules/.bin/vite build --config vite.config.ts`: production bundle 생성 통과
- `docker compose -f deploy/compose/portfolio.yml config`: 필요한 환경 변수를 주입한 VPS용 Compose 해석 성공
- `deploy/Caddyfile`: React history fallback과 `/api` reverse proxy를 단일 public entrypoint로 구성
- `deploy/backup-postgres.sh`: PostgreSQL custom-format dump 재현 명령 제공
- `deploy/restore-postgres.sh`: 명시적 `ALLOW_DESTRUCTIVE_RESTORE=YES` 없이는 실행되지 않는 복구 명령 제공
- 임시 Compose PostgreSQL에서 `backup-postgres.sh`로 dump를 만들고 `restore-postgres.sh`로 복구한 뒤 `SELECT 1` 확인까지 통과했다. 테스트 컨테이너와 volume은 제거했다.
- `docs/parity/matrix.yaml`: `pending`·`adapter` 0개. `verified` 97개와 ADR에 근거한 `excluded` 7개를 구분한다.

## 현재 한계

- 실제 VPS에서 DNS/TLS 리허설은 아직 실행하지 않았다. 이번 복구 리허설은 임시 local Compose 환경이며, VPS 자격·DNS·TLS 상태를 대체하지 않는다.
- `excluded` 항목은 현재 제품 범위 밖이며 ADR에 근거를 둔다.
- 성능 수치는 측정하지 않았으므로 SLA나 처리량을 주장하지 않는다.
