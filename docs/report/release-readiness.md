# 재현 가능한 릴리스 근거

G1~G8을 로컬·CI에서 재현할 수 있는 현재 릴리스 상태만 기록한다. 실제 VPS 공개 운영은 G9 범위다.

## 확인된 근거

- `./gradlew clean check migrationTest`: backend unit/integration, Modulith, parity inventory, Spotless와 migration test 전체 통과
- `scripts/validate-parity-matrix.sh`: 104개 page/API 행 중 `verified=95`, `excluded=9`, `pending=0` 확인
- `frontend/node_modules/.bin/tsc -p frontend/tsconfig.json --noEmit`: 타입 검사 통과
- `frontend/node_modules/.bin/vitest run`: 7개 파일, 14개 테스트 통과
- `frontend/node_modules/.bin/vite build --config vite.config.ts`: production bundle 생성 통과
- `frontend/e2e/parity-shell.spec.ts`: Chromium desktop/mobile shell smoke 4개 통과
- `migration/fixtures/logical-fixture.yaml`: guest/member/moderator와 Care·검색·신고 대표 시나리오를 고정된 logical fixture로 연결
- `src/test/java/com/townpet/care/CareControllerTest.java`: Care Request → Application → Assignment → Feedback 전체 상태 전이 통과
- `docker compose -f deploy/compose/portfolio.yml config`: 필요한 환경 변수를 주입한 VPS용 Compose 해석 성공
- `deploy/Caddyfile`: React history fallback과 `/api` reverse proxy를 단일 public entrypoint로 구성
- `TOWNPET_DOMAIN`을 설정하면 같은 Caddy 구성이 로컬 `:80` 또는 VPS 도메인의 자동 HTTPS site address로 동작한다.
- Portfolio Compose의 PostgreSQL 초기화 script가 `APP_DB_USER/PASSWORD` role을 생성하고 public schema 권한만 부여한다. backend가 별도 app role로 접속하는 것을 임시 production stack에서 확인했다.
- production backend/frontend image build와 임시 portfolio stack 기동 후 backend·PostgreSQL health, Caddy `/api/health` JSON 응답과 SPA history fallback을 확인했다. 테스트 stack은 검증 후 제거했다.
- `deploy/backup-postgres.sh`: PostgreSQL custom-format dump 재현 명령 제공
- `deploy/restore-postgres.sh`: 명시적 `ALLOW_DESTRUCTIVE_RESTORE=YES` 없이는 실행되지 않는 복구 명령 제공
- 임시 Compose PostgreSQL에서 `backup-postgres.sh`로 dump를 만들고 `restore-postgres.sh`로 복구한 뒤 `SELECT 1` 확인까지 통과했다. 테스트 컨테이너와 volume은 제거했다.
- `docs/parity/matrix.yaml`: `pending` 0개. 현재 구현 근거가 있는 `verified` 95개와 ADR에 근거한 `excluded` 9개를 구분한다.

## 배포 전 작업 상태

| 항목 | 상태 | 배포 전 판단 |
|---|---|---|
| backend/frontend quality gate | 확인됨 | 최신 변경을 고정한 뒤 최종 1회 재실행 |
| parity inventory | 확인됨 | `pending=0`; `excluded`는 ADR 범위 밖 기능 |
| local Docker migration·health·smoke | 확인됨 | 운영 환경과 혼동하지 않음 |
| local backup/restore | 확인됨 | VPS에서 동일 절차와 외부 보관 위치를 다시 검증 |
| VPS public workload 성능 | 미실행 | 배포 전에 실행 필요 |
| DNS·TLS·secure session cookie | 미실행 | 실제 domain을 정한 뒤 실행 필요 |
| production media | 결정 필요 | object storage 연동 또는 upload 명시적 비활성화 필요 |
| demo seed·scoped reset | 부분 구현 | 공개 demo 운영 방식과 reset 주기 확정 필요 |
| email verification·password recovery | 미운영 | SMTP 연동 또는 공개 범위 명시 필요 |
| 외부 관측·알림 | 미운영 | 최소 health/log 수집과 장애 알림 결정 필요 |

현재 배포를 막는 가장 큰 기능 경계는 production media다. `application-production.yml`에서는 `UnavailableObjectStorage`가 선택되므로 실제 upload를 공개할 수 없다. 공개 sandbox에서 upload를 제공하려면 PostgreSQL metadata와 S3-compatible storage, private object policy를 하나의 vertical slice로 완성해야 한다. upload를 당분간 제공하지 않는다면 frontend와 API가 이를 명확히 거부하도록 정책을 확정해야 한다.

demo 계정은 local fixture와 production 운영 데이터가 다르다. 공개 demo를 사용할 경우 `TOWNPET_DEMO_DATA_ENABLED`, 초기 seed, demo actor 소유 콘텐츠만 지우는 reset, 관리자 credential 비공개를 배포 runbook에 명시한다. 실제 사용자의 회원가입과 개인정보 수집은 현재 제품 범위가 아니다.

## 현재 한계

- 실제 VPS에서 DNS/TLS 리허설은 아직 실행하지 않았다. 이번 복구 리허설은 임시 local Compose 환경이며, VPS 자격·DNS·TLS 상태를 대체하지 않는다.
- 배포 전 local runtime은 `SPRING_PROFILES_ACTIVE=local`에서 filesystem media adapter와 demo reset script를 사용한다. 외부 SMTP와 object storage는 G9에서 결정한다.
- `excluded` 항목은 현재 제품 범위 밖이며 ADR에 근거를 둔다.
- local Docker 성능 측정과 bundle/Web Vital 경로는 확인했지만, local 수치를 운영 SLA나 VPS 처리량으로 주장하지 않는다.

## 최종 release gate 순서

```text
production media/demo/email 정책 확정
→ 새 Docker volume에서 migration·seed·health 확인
→ VPS에서 Caddy·TLS·session cookie 확인
→ public/member/mixed 성능과 resource 측정
→ backup 외부 보관·restore·rollback 확인
→ backend/frontend/browser 전체 gate 1회
→ 공개 전 demo/개인정보/로그 점검
```

이 문서의 `확인된 근거`는 local·CI evidence이고, `배포 전 작업 상태`의 미실행 항목을 완료된 운영 경험처럼 사용하지 않는다.
