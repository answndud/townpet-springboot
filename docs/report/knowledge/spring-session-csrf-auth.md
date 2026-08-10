# Spring Session·CSRF·Identity vertical slice 면접 노트

## 인증 흐름

이 프로젝트는 JWT를 browser storage에 두지 않고 opaque session cookie와 PostgreSQL Spring Session JDBC를 사용한다. 로그인 성공 시 `changeSessionId()`로 fixation을 방어하고 SecurityContext를 session에 저장한다. member module은 credential entity를 직접 참조하지 않고 principal의 안정적인 member UUID로 현재 사용자를 찾는다.

## CSRF

CookieCsrfTokenRepository가 browser가 읽을 수 있는 `XSRF-TOKEN`을 발급하고 React client는 state-changing request에 `X-XSRF-TOKEN`을 보낸다. CSRF가 없으면 login 같은 POST도 403이 되며, 인증이 없어도 catalog GET은 공개한다. 인증 실패는 API entry point에서 401로 일관되게 반환한다.

## 면접 답변 포인트

- “세션을 쓴다”에서 끝내지 않고 session fixation, revoke, CSRF와 401/403 의미를 테스트했다.
- DB는 credential과 profile을 ID로 연결하고 cross-module JPA association은 만들지 않았다.
- 처음부터 OAuth와 guest/admin을 모두 넣지 않고 login·profile·neighborhood의 핵심 invariant를 먼저 검증했다.
- 테스트가 발견한 실제 lifecycle 오류를 security 설정과 코드에 반영했다.
