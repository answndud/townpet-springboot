# EV-008 · P2.1a Identity·Member·Catalog thin vertical slice

## 범위와 출발점

P1까지는 계약·shell·검증 기반만 있었고 실제 회원 흐름은 없었다. 혼자 개발하는 프로젝트에서 17개 bounded context의 모든 기능을 한 번에 구현하면 설계가 코드보다 앞서는 위험이 있어, 로그인·세션·동네·프로필을 하나의 얇은 slice로 먼저 연결했다.

## Trigger와 한계

첫 인증 테스트에서 session fixation 방어를 호출할 세션이 아직 없어서 `request.changeSessionId()`가 실패했고, 비인증 요청은 기대한 401 대신 403으로 내려갔다. 이는 security rule만 선언하고 실제 request lifecycle을 확인하지 않았을 때 생기는 환경 차이였다.

## 결정과 구현

PostgreSQL Flyway `V002`에 `member_account`, `identity_credential`, `neighborhood`, `member_profile`, `member_pet` 소유 테이블과 FK·unique·check constraint를 추가했다. Spring Security DaoAuthenticationProvider와 BCrypt, Spring Session JDBC, Cookie CSRF를 연결하고 `/api/v1/auth/sessions`, `/api/v1/auth/csrf`, `/api/v1/members/me`, `/api/v1/members/me/onboarding`, `/api/v1/catalog/neighborhoods`를 구현했다. frontend에는 CSRF-aware API client와 login form을 연결했다.

## 검증

- `./gradlew clean check migrationTest`: Spring context, architecture, OpenAPI, parity inventory와 V002 Testcontainers migration 통과
- `IdentityMemberControllerTest`: login/session, current member, CSRF 누락 403, unauthenticated 401, public catalog, CSRF cookie 통과
- frontend: typecheck, Vitest 4개, production build, Chromium/mobile E2E 4개 통과
- `./scripts/frontend-backend-smoke.sh`: Spring health와 Vite preview marker 통과

## 남은 범위

OAuth link, password reset/verification, guest step-up, staff role, real onboarding pet form과 legacy differential row는 아직 `P2.1b` 범위다. 이번 slice가 전체 Identity 요구사항 완료를 의미하지 않도록 PLAN에서 분리해 기록한다.
