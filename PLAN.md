# PLAN.md

## Goal

실제 VPS·DNS·TLS·외부 object storage를 구성하기 전에 TownPet의 제품 기능과 로컬 실행 경험을 완성한다. Legacy의 49개 page·55개 API를 정상·오류·권한·상태 전이·반응형 화면까지 구현하거나 ADR로 명확히 제외하고, 새 환경에서 합성 demo 데이터로 같은 사용자 여정을 재현할 수 있으면 배포 전 개발 완료로 판정한다.

현재 G1~G8의 기본 build/test gate는 통과했지만, parity matrix의 `pending=0`만으로 의미 parity가 증명되지는 않는다. 다음 작업은 실제 구현 공백과 문서·코드 간 불일치를 먼저 닫는다. Hetzner, DNS, TLS, SMTP, 외부 object storage, offsite backup, monitoring은 이 PLAN의 범위가 아니다.

## Active

현재 실행 중인 권한 slice는 없다. 다음 기능 작업은 새 권한·parity gap이 확인될 때 이 위치에 추가한다.

## Completed

### P4 - 역할별 권한 경계를 실제 계정 조합으로 닫는다 ✅ completed (2026-08-12)

- 파일: `src/main/java/com/townpet/identity/SecurityConfig.java`, `src/main/java/com/townpet/common/MemberOnly.java`, 역할별 controller, `src/main/java/com/townpet/member/`, `frontend/src/App.tsx`, `frontend/src/ProfilePage.tsx`, `frontend/src/PublicMemberProfilePage.tsx`, `src/test/java/com/townpet/identity/IdentityMemberControllerTest.java`
- 변경:
  - Spring Method Security와 `/api/admin/**`·legacy 신고 alias matcher를 활성화해 MEMBER가 모든 관리자 surface에 접근하지 못하게 한다.
  - 일반 회원 mutation을 `MEMBER` 전용으로 제한하고 MODERATOR는 운영 검토·공개 읽기만 사용하게 한다.
  - GuestPrincipal 기반 작성·댓글·step-up도 `익명 또는 MEMBER`만 허용해 MODERATOR가 guest 경로로 우회하지 못하게 한다.
  - ViewerShell, legacy 공개 프로필, 공개 reaction visibility를 신규 profile API와 동일하게 맞춘다.
  - React moderator/member route와 detail action을 역할별로 분리하고 login `next`를 안전하게 처리한다.
- 검증: `./gradlew test --no-daemon`, `./gradlew migrationTest --no-daemon`, `cd frontend && ./node_modules/.bin/tsc --noEmit && ./node_modules/.bin/vitest run`, Docker demo MEMBER/MODERATOR API status 확인
- 완료: 관리자 API가 MEMBER에는 403, MODERATOR에는 200을 반환하고, MODERATOR의 일반·guest 작성 mutation은 403이며 화면·공개범위·legacy contract가 역할에 맞게 표시된다. 실제 Docker demo 계정 확인과 `IdentityMemberControllerTest`의 양방향 회귀 테스트를 추가했다.

### P1 - 제품 parity와 ADR 판정을 실제 근거로 닫는다 ✅ completed (2026-08-12)

- 파일: `docs/parity/matrix.yaml`, `ADR.md`, `src/main/java/com/townpet/{care,discovery,media,marketplace,localguide}/`, `frontend/src/`, `frontend/e2e/`, `src/test/java/`
- 변경:
  - matrix의 49 page·55 API를 Legacy `app/src/app`와 다시 대조하고, `verified`는 정상·오류·권한·상태 전이와 대표 frontend/backend evidence가 있는 경우에만 유지한다.
  - 근거가 부족한 항목은 임의로 `excluded`로 바꾸지 말고 실제 동작을 구현하거나, 현재 제품 범위 밖인 이유·영향·대안을 ADR에 기록한다.
  - Care의 Request·Application·Assignment·Feedback 사용자 여정을 실제 화면·API·권한·상태 전이와 browser/integration evidence로 닫고, 현재 ADR의 “care package가 비어 있음” 같은 오래된 근거를 현재 코드와 일치시킨다.
  - Search/guest search의 query·기간·scope·type 의미, best/personalization의 ranking 의미, group buy compatibility, guide slug detail, commercial route의 의미를 Legacy fixture로 대조한다.
  - production에서 의도적으로 제공하지 않을 media presign·personalization projection 등은 route와 화면에서 오해를 만들지 않도록 명시적 capability/error 정책을 정한다.
- 검증:
  - `./scripts/validate-parity-matrix.sh`
  - 관련 backend integration test와 frontend Vitest
  - 변경된 page family의 Chromium desktop/mobile Playwright journey
- 완료: Care 전체 상태 전이 evidence와 ADR-0035 근거를 추가했고, Search·Best·GroupBuy·Guide·Commercial·Media의 현재 범위·제외 이유·대표 evidence를 matrix에 연결했다. `pending=0`은 실제 근거 또는 ADR 결정으로 설명된다.

### P2 - 배포 없이도 완결되는 local/portfolio runtime을 만든다 ✅ completed (2026-08-12)

- 파일: `src/main/java/com/townpet/operations/`, `src/main/java/com/townpet/media/`, `src/main/java/com/townpet/identity/`, `src/main/resources/`, `deploy/compose/local.yml`, `deploy/portfolio.env.example`, `migration/`
- 변경:
  - `TOWNPET_DEMO_DATA_ENABLED`를 실제 application gate로 연결하고, 합성 demo actor·권한·콘텐츠만 대상으로 하는 idempotent seed/reset 명령을 만든다. reset은 다른 데이터와 개인정보를 삭제하지 않는다.
  - local profile에서 재시작 후에도 확인 가능한 filesystem/MinIO adapter와 production fail-closed 정책을 정리한다. 외부 object storage를 도입하지 않더라도 upload가 왜 동작하거나 제한되는지 UI·API·문서가 일치해야 한다.
  - SMTP 없이도 local/test에서 email verification·password reset을 재현할 수 있는 capture adapter와 production에서의 명확한 비활성화 응답을 분리한다.
  - 공개되지 않는 ADMIN/OPERATOR 자격, secret, 정확 위치, raw token이 seed·응답·로그에 나타나지 않는지 점검한다.
  - `deploy/compose/local.yml`과 기본 실행 명령을 clean PostgreSQL에서 한 번에 기동·초기화·reset할 수 있도록 정리한다. 실제 VPS 설정은 추가하지 않는다.
- 검증:
  - clean Docker PostgreSQL에서 migration → seed → reset → 재실행을 반복하고 row scope·idempotency를 SQL로 확인
  - demo flag on/off, local email capture, upload success/failure, 권한 거부 integration test
  - `./scripts/frontend-backend-smoke.sh`
- 완료: `local` profile의 filesystem media adapter, demo credential gate, scoped/idempotent demo reset script, local Compose backend, Care runtime을 추가했다. 외부 서비스 계정 없이 합성 demo 사용자와 local DB로 핵심 흐름을 재현할 수 있다.

### P3 - 배포 전 release candidate evidence를 고정한다 ✅ completed (2026-08-12)

- 파일: `.github/workflows/`, `scripts/`, `migration/fixtures/`, `docs/parity/`, `docs/report/`, `frontend/e2e/`, `README.md`
- 변경:
  - logical fixture에 guest/member/moderator와 대표 Care·Search·Marketplace·LostFound·Notification·Admin 시나리오를 연결하고, 실제 test ID·expected error·권한·상태를 기록한다.
  - 대표 정상·오류·권한·동시성 journey를 backend integration, frontend Vitest, browser E2E로 연결한다. 단순 route 존재 확인 test는 완료 근거로 세지 않는다.
  - CI가 parity validator, backend clean check/migrationTest, frontend typecheck/test/build, browser smoke를 실행하고 실패 원인을 구분하도록 유지한다.
  - `docs/report/`에는 실제로 구현한 경계·실패 원인·trade-off·검증 명령만 선별해 기록하고, 측정하지 않은 성능·가용성·복구 수치는 주장하지 않는다.
  - README와 report의 “완료” 표현을 `배포 전 local/CI release candidate` 범위로 맞춘다.
- 검증:
  - `./gradlew clean check migrationTest --no-daemon`
  - `cd frontend && ./node_modules/.bin/tsc --noEmit && ./node_modules/.bin/vitest run && ./node_modules/.bin/vite build`
  - `./scripts/validate-parity-matrix.sh`
  - `cd frontend && ./node_modules/.bin/playwright test --config=playwright.config.ts`
  - `./scripts/frontend-backend-smoke.sh`
- 완료: logical fixture에 대표 Care·검색·신고 시나리오를 연결하고, release-candidate validator가 parity matrix·fixture·shell script를 검사한다. backend/frontend/browser gate와 함께 기능·권한·오류·화면·migration evidence를 재현할 수 있다.

## Backlog

- G9 - 사용자가 배포를 시작할 때 Hetzner VPS, DNS/TLS/Caddy, 외부 object storage/SMTP, offsite backup·restore·rollback, monitoring·alerting, 실제 공개 URL smoke와 비용을 구성한다.
- 실제 Legacy 개인정보 migration, Kakao/Naver OAuth, 결제·정산·환불·private chat은 현재 제품 범위에서 제외한다.
- 검색 corpus와 실제 latency가 기준을 넘을 때만 `SearchDocument`/GIN·trigram을 별도 ADR로 결정한다.
- 실제 ranking 품질·freshness·query latency 요구가 생길 때만 FeedDocument와 versioned personalization을 도입한다.

## 완료 판정

P1~P3을 모두 통과하면 “TownPet Spring Boot 포트폴리오 프로젝트의 배포 전 개발 완료”라고 표현한다. 이 상태에서도 실제 공개 운영·TLS·외부 저장소·복구 SLA까지 완료했다고 표현하지 않으며, 그것은 G9에서 별도로 검증한다.
