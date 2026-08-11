# 출시 준비 근거

G1~G6을 큰 사용자 여정으로 연결한 뒤 현재 확인된 출시 준비 상태만 기록한다.

## 확인된 근거

- `./gradlew migrationTest`: Flyway 전체 migration과 기존 integration suite 통과
- `frontend/node_modules/.bin/tsc -p frontend/tsconfig.json --noEmit`: 타입 검사 통과
- `frontend/node_modules/.bin/vitest run`: 7개 파일, 14개 테스트 통과
- `docker compose -f deploy/compose/portfolio.yml --env-file deploy/portfolio.env.example config`: VPS용 Compose 해석 성공
- `deploy/Caddyfile`: React history fallback과 `/api` reverse proxy를 단일 public entrypoint로 구성
- `deploy/backup-postgres.sh`: PostgreSQL custom-format dump 재현 명령 제공

## 현재 한계

- 실제 VPS에서 DNS/TLS와 복구 리허설은 아직 실행하지 않았다.
- legacy 49개 page 전체 parity는 완료가 아니며 matrix의 `pending`을 유지한다.
- 성능 수치는 측정하지 않았으므로 SLA나 처리량을 주장하지 않는다.
