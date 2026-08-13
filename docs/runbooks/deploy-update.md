# 배포·업데이트

현재는 실제 VPS에 실행하지 않는 rehearsal 절차다. production secret은 shell history·Git·문서에 남기지 않는다.

## 사전 확인

```bash
cd /Users/alex/project/townpet-springboot
# 실제 secret이 들어간 별도 env-file을 사용한다. Git의 example 파일을 그대로 쓰지 않는다.
docker compose --env-file /secure/path/townpet.portfolio.env \
  -f deploy/compose/portfolio.yml config
./gradlew clean check migrationTest
(cd frontend && corepack pnpm install --frozen-lockfile && corepack pnpm typecheck && corepack pnpm build)
```

필수 환경변수가 빠진 `portfolio.yml`은 의도적으로 `config` 단계에서 실패해야 한다. SMTP local 검증은 base compose와 overlay를 함께 지정한다.

## 초기 volume

1. PostgreSQL·MinIO만 시작한다.
2. health가 `healthy`가 될 때까지 기다린다.
3. PostgreSQL bootstrap init script가 `postgis`·`citext`를 만들고 `APP_DB_USER` 권한을 부여했는지 확인한다. 이 전제가 없으면 Flyway V001이 의도적으로 중단된다.
4. backend를 내부 네트워크에서 시작해 Flyway를 적용한다.
5. `scripts/sanitize-production-demo.sh`를 dry-run으로 먼저 실행한다.
6. 대상이 새 production DB인지 확인한 뒤 `TOWNPET_PRODUCTION_SANITIZE_APPLY=YES`로 1회 적용한다.
7. `scripts/bootstrap-private-moderator.sh`로 비공개 moderator를 생성한다.
8. backend health와 DB readiness를 확인한 뒤 web/Caddy를 시작한다.

## 기존 데이터 업데이트

1. paired backup을 생성한다.
2. image tag와 현재 commit을 기록한다.
3. `docker compose pull` 또는 새 image build 후 backend를 먼저 교체한다.
4. Flyway가 실패하면 web을 노출하지 않고 로그와 migration 상태를 확인한다.
5. health가 회복된 뒤 web을 교체하고 익명 공개 화면·로그인·moderator health를 확인한다.

## 성공 기준

- backend readiness `UP`
- Flyway migration 완료
- MinIO private bucket 접근 가능
- 공개 demo 계정·콘텐츠 없음
- SMTP 설정 누락 시 시작 또는 account delivery가 조용히 성공하지 않음
- `TOWNPET_EMAIL_ENABLED`가 production env-file에서 명시돼 있으며, 공개 recovery를 사용할 때는 `true`다.
- web과 media domain이 예상 host로만 응답
