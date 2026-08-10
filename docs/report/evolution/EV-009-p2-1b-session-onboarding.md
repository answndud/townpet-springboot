# EV-009 — P2.1b 세션 폐기와 반려동물 온보딩

- 시점·범위: P2.1b 첫 hardening slice, Identity·Member, 커밋 전 검증 단계
- 출처: `implementation-discovery`

## 출발 상태와 Trigger

P2.1a는 로그인·현재 회원·동네 온보딩까지만 연결했고, `member_pet` 테이블은 만들어졌지만 애플리케이션에서 읽거나 쓰지 않았다. 또한 로그아웃 API는 있었지만 session revoke 회귀 테스트와 실제 profile UI가 없었다. 이 상태에서는 “인증이 된다”는 것과 “인증된 사용자가 자신의 온보딩 상태를 관리한다”는 것을 함께 증명할 수 없었다.

## 대안과 결정

1. 반려동물을 별도 CRUD slice로 미룬다 — 화면 parity와 onboarding transaction을 설명하기 어려워 선택하지 않았다.
2. onboarding 요청에 반려동물 목록을 포함하고 소유한 목록을 원자적으로 교체한다 — 현재 프로필 설정 범위가 작고, 단일 회원이 최대 10마리라는 경계를 API validation과 함께 검증할 수 있어 선택했다.
3. logout을 프런트에서만 처리한다 — 서버 session을 폐기하지 못하므로 선택하지 않았다.

## 구현

- `MemberPetEntity`·repository가 `member_pet`의 소유 범위를 명시한다.
- `PUT /api/v1/members/me/onboarding`은 bio·동네·반려동물 목록을 한 transaction에서 저장하고, 목록은 현재 member의 기존 행만 교체한다.
- `GET /api/v1/members/me`가 반려동물을 반환한다.
- `DELETE /api/v1/auth/sessions/current`는 SecurityContext를 지우고 Spring Session을 invalidate한다.
- CSRF endpoint는 token body와 non-HttpOnly `XSRF-TOKEN` cookie를 함께 명시해 브라우저 계약을 안정화했다.
- React `/profile`은 현재 회원과 반려동물을 표시하고 CSRF를 얻은 뒤 logout을 호출한다.

## 검증과 결과

- `./gradlew test --tests '*IdentityMemberControllerTest'`: login, onboarding pet replacement, logout revoke, CSRF, unauthorized가 통과
- `openApiValidate`와 contract test로 Pet schema·logout 계약을 검증
- 반려동물 이름·종류 길이/공백, 목록 최대 10개를 Bean Validation으로 차단

현재 남은 범위는 password reset·verification, demo seed/lifecycle, provider stub, role/IDOR matrix와 auth differential E2E다. 이번 slice는 P2.1 전체 완료가 아니다.

## 면접 답변

### 30초

처음에는 반려동물 테이블만 만들어 두고 회원 API와 연결하지 않았다. 실제 사용자 여정을 검증하려면 동네·소개·반려동물 설정이 한 번에 저장되고, 로그아웃 뒤 기존 session이 즉시 거부되어야 했다. 그래서 회원 소유 범위 안에서 onboarding 목록을 교체하는 작은 transaction과 서버 session revoke를 먼저 추가하고 MockMvc로 검증했다.

### 2분·꼬리 질문

목록 교체는 단순해서 현재는 delete-and-insert를 택했지만, 반려동물별 이력·사진·동시 편집이 생기면 ID와 optimistic version을 유지하는 command로 분리한다. 세션은 브라우저 저장 JWT가 아니라 Spring Session JDBC 원장이므로 logout에서 cookie 삭제만 하는 대신 서버 저장 session을 invalidate한다. 최대 10개와 field length는 UI가 아니라 API validation에서 강제한다.
