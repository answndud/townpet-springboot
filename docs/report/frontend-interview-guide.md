# TownPet 프론트엔드 면접 복기 가이드

이 문서는 Spring Boot에 익숙하지만 React·브라우저·프론트엔드 성능에는 상대적으로 익숙하지 않은 개발자가 TownPet의 frontend 작업을 다시 이해하고 면접에서 설명할 수 있도록 만든 학습용 문서다.

일반적인 React 교과서가 아니라 현재 저장소의 코드와 실제 수정·측정 결과를 기준으로 한다. 따라서 “React는 원래 이렇게 한다”보다 “TownPet에서는 왜 이렇게 했고, 어떤 문제가 있었으며, 무엇으로 검증했는가”를 중심으로 읽는다.

관련 근거:

- 전체 설계 서사: [engineering-story.md](engineering-story.md)
- 현재 기술 개념: [technical-notes.md](technical-notes.md)
- 기존 면접 답변 압축본: [interview-prep.md](interview-prep.md)
- frontend 성능 측정 규칙과 결과: [../performance/README.md](../performance/README.md)
- SEO/OG 향후 구현 계획: [../seo-og-public-community-plan.md](../seo-og-public-community-plan.md)

---

## 1. 먼저 잡아야 할 전체 그림

Spring Boot 개발자의 관점에서 frontend를 다음처럼 대응시키면 이해하기 쉽다.

| Spring Boot 관점 | TownPet frontend 관점 |
| --- | --- |
| Controller | React route/page와 event handler |
| Request/response DTO | `frontend/src/api/client.ts`의 TypeScript type |
| Service/use case | page의 mutation 함수와 API client 함수 |
| Repository 호출 | `fetch`를 감싼 `apiFetch` |
| SecurityContext | `AuthContext`와 서버 session 조회 |
| Exception handler | `ApiError`와 `ProblemDetail` 처리 |
| Transaction 경계 | 한 API mutation이 backend에서 보장하는 상태 변경 단위 |
| DB source of truth | frontend state는 일시적인 화면 cache일 뿐, 최종 원장은 backend |
| Integration test | Vitest + Playwright + frontend/backend smoke |

중요한 차이는 frontend state가 DB처럼 영속적이지 않다는 점이다. React state는 화면을 그리기 위한 메모리 값이다. 새로고침하면 사라지고, 여러 탭과 서버의 변경을 자동으로 일치시키지 않는다. 따라서 TownPet에서는 다음 원칙을 유지한다.

```text
사용자 입력
  → React event handler
  → typed API client
  → Spring Boot session/CSRF/security
  → PostgreSQL transaction
  → JSON response
  → React state 갱신
  → 화면 재렌더링
```

화면에서 버튼을 비활성화하거나 메뉴를 숨기는 것은 보안이 아니다. 실제 권한은 Spring Security, application policy, DB constraint가 결정한다. frontend는 같은 규칙을 사용자 경험에 반영하는 계층이다.

## 2. 브라우저가 TownPet을 여는 순서

### 2.1 최초 진입과 SPA 이동은 다르다

최초 진입은 브라우저가 HTML, JS, CSS를 네트워크에서 받는 과정이다. 이후 `/feed`에서 `/boards/adoption`으로 이동할 때는 React Router가 URL을 바꾸고 필요한 component를 렌더링한다. 보통 전체 HTML 문서를 다시 받지 않는다.

```text
최초 진입
브라우저 → Vite/Caddy가 제공하는 index.html
        → JS bundle 다운로드
        → React mount
        → Router가 현재 URL에 맞는 Page 선택
        → Page가 API 호출
        → loading/data/error 화면 렌더링

SPA 이동
Link/NavLink 클릭
        → history URL 변경
        → Router route match
        → lazy chunk 필요 시 다운로드
        → Page 렌더링
        → Page API 호출
```

Spring MVC의 server-side forward와 비슷하게 생각하면 안 된다. React Router의 route 변경은 backend controller 호출을 자동으로 의미하지 않는다. 페이지가 필요로 하는 API를 component effect 또는 `useAbortableRequest`가 별도로 호출한다.

### 2.2 현재 구조

- `frontend/src/main.tsx`: React application 시작점
- `frontend/src/App.tsx`: route, 공통 shell, navigation, lazy page, route timing
- `frontend/src/api/client.ts`: fetch, DTO, CSRF, API error, GET cache
- `frontend/src/auth/AuthContext.tsx`: 현재 로그인 viewer와 auth transition
- `frontend/src/styles.css`: 공통 desktop visual system
- `frontend/src/features/`: 사용자 여정별 feature와 page

`App.tsx`에는 많은 page가 `lazy(() => import(...))`로 선언되어 있다. 이는 최초 bundle에 모든 관리자·거래·분실·돌봄 화면을 넣지 않고, 해당 route를 사용할 때 chunk를 불러오기 위한 선택이다.

### 2.3 lazy와 Suspense

Spring에서 controller class를 필요할 때 bean으로 로딩하는 개념과 정확히 같지는 않지만, 사용자에게는 “화면 코드가 아직 다운로드되지 않은 동안 fallback을 보여준다”는 의미다.

```tsx
const PublicationDetailPage = lazy(() => import("./features/publication/PublicationDetailPage"));

<Suspense fallback={<main role="status">화면을 준비하는 중...</main>}>
  <Routes>...</Routes>
</Suspense>
```

lazy loading이 API data loading을 해결하는 것은 아니다.

- lazy loading: page JavaScript chunk가 아직 없음
- data loading: page는 실행됐지만 backend response가 아직 없음
- 각각 별도의 fallback과 error 처리가 필요함

## 3. React component를 읽는 순서

익숙하지 않은 TSX를 볼 때 처음부터 JSX 전체를 읽지 않는다.

1. 어떤 route에서 렌더링되는가?
2. 어떤 props와 URL parameter를 받는가?
3. state가 무엇이고 각각 어떤 사용자 행동을 표현하는가?
4. API 호출이 어디에서 시작되고 어떤 dependency에 반응하는가?
5. 정상·loading·empty·error 상태가 모두 있는가?
6. mutation 후 어떤 state/query를 다시 읽거나 갱신하는가?
7. 권한별로 어떤 action이 보이고 숨겨지는가?

게시글 상세를 예로 들면 다음처럼 나눈다.

```text
route parameter: publicationId
remote data: publication, comments, stats, reaction, bookmark, relationship
ui state: loading, error, reply target, editing/deleting state, input value
mutation: comment/reply, like, bookmark, delete, report
```

`publication`은 backend에서 온 원격 상태이고, `replyTargetId`는 현재 어느 댓글에 답하는지 나타내는 화면 상태다. 이 둘을 하나의 거대한 state object로 합치지 않는 것이 유지보수에 유리하다.

## 4. useEffect, dependency, stale response

effect는 “render가 끝난 뒤 외부 세계와 동기화하는 작업”이다. API 호출, document title 변경, event listener, PerformanceObserver가 대표적이다.

```tsx
useEffect(() => {
  const controller = new AbortController();
  publicationApi.detail(publicationId, controller.signal)
    .then(setPublication)
    .catch(setError);
  return () => controller.abort();
}, [publicationId]);
```

`[publicationId]`는 publicationId가 바뀔 때 다시 실행하라는 뜻이다. cleanup은 다음 route로 이동하거나 component가 사라질 때 실행된다.

빠른 route 이동에서는 다음 경쟁이 생길 수 있다.

```text
A 요청 시작
B 요청 시작
B 응답 도착 → B 렌더링
A 응답 도착 → 오래된 A가 B 화면을 덮음
```

이를 stale response 또는 race condition이라고 한다. `useAbortableRequest`는 요청마다 `AbortController`를 만들고 이전 요청을 취소하며, 현재 controller와 같은 요청의 결과만 state에 반영한다.

Spring의 `@Transactional`이 두 HTTP 요청 사이의 화면 경쟁을 해결해주지는 않는다. backend transaction consistency와 frontend request lifecycle consistency는 별개의 문제다.

## 5. API client를 Spring Controller 계약과 연결하기

`frontend/src/api/client.ts`는 단순한 fetch 모음이 아니라 transport seam이다.

- TypeScript request/response type
- `credentials: "include"`로 session cookie 전송
- CSRF cookie 읽기와 `X-XSRF-TOKEN` header 설정
- response body parsing
- Spring의 ProblemDetail을 `ApiError`로 변환
- 개발 환경 API timing 기록
- 안전한 GET의 짧은 메모리 cache와 request deduplication
- AbortSignal 전달

### 5.1 apiFetch 흐름

```text
apiFetch(path, init)
  → method 확인
  → XSRF-TOKEN cookie 확인
  → credentials include로 fetch
  → response text 수신
  → !ok이면 ProblemDetail parse 후 ApiError
  → 성공이면 JSON parse
  → 개발 환경에서 duration/status 기록
```

성공·실패 응답을 모두 text로 받은 뒤 JSON parse를 시도하는 이유는 backend가 JSON이 아닌 오류 body를 반환해도 안전하게 fallback하기 위해서다.

### 5.2 DTO를 직접 맞추는 이유

현재는 OpenAPI generated client를 사용하지 않는다. Spring controller DTO와 frontend type을 직접 맞추되 다음으로 계약을 검증한다.

- backend controller/integration test
- frontend typecheck
- frontend flow test
- frontend/backend smoke
- Playwright browser journey

API가 커지고 외부 client가 생기거나 여러 팀이 독립적으로 계약을 소비하면 OpenAPI generator를 재평가할 수 있다. 현재는 generated code의 동기화 비용보다 직접 읽을 수 있는 작은 client가 유리하다.

## 6. 인증·세션·CSRF를 브라우저에서 이해하기

### 6.1 로그인 상태는 React state만이 아니다

로그인 성공 후 React의 `viewer` state만 바꾸면 새로고침 때 로그인이 사라진다. 진짜 로그인 상태는 Spring Session JDBC와 session cookie에 있다.

```text
로그인 POST
  → Spring Security credential 검증
  → SESSION cookie 발급
  → JDBC session 저장
  → React AuthContext가 session/member 조회
  → 화면에 viewer 표시
```

로그인 후 route 이동으로 로그인이 풀리는 문제는 “페이지가 바뀌어서 session이 없어졌다”가 아니라 auth state 초기화·redirect·session 조회 lifecycle이 잘못 연결된 문제로 진단해야 했다. 실제 browser session cookie로 login → route 이동 → protected API 재호출을 검증해야 한다.

### 6.2 CSRF는 API client의 공통 관심사

브라우저는 session cookie를 자동으로 보내므로 다른 사이트가 사용자의 browser를 이용해 mutation을 보내지 못하도록 CSRF token이 필요하다.

```text
GET /api/v1/auth/csrf
  → XSRF-TOKEN cookie와 token 반환

POST/PUT/PATCH/DELETE
  → SESSION cookie 자동 전송
  → X-XSRF-TOKEN header 추가
  → Spring Security가 검증
```

mutation마다 CSRF를 따로 받으면 request waterfall이 생긴다. 현재 client는 cookie가 있으면 즉시 사용하고, 없을 때만 요청하며 진행 중인 CSRF 요청을 공유한다.

### 6.3 frontend가 하면 안 되는 것

- localStorage에 browser JWT를 새로 저장하기
- 화면에서 role을 바꿔 권한을 얻었다고 가정하기
- 401과 403을 같은 로그인 실패로 처리하기
- logout 시 React state만 초기화하고 서버 session을 남기기
- request body의 memberId를 신뢰해 내 데이터를 조회하기

## 7. 게시글·댓글 UI를 복기하는 방법

### 7.1 댓글 답글 위치 문제

답글 composer가 댓글 바로 아래가 아니라 페이지 맨 아래에 나타나는 문제는 API 문제가 아니라 UI state와 DOM 위치의 결합 문제다.

잘못된 구조:

```text
댓글 목록
댓글 A
댓글 B
댓글 C
공통 reply form
```

state는 A를 기억해도 form의 DOM 위치가 list footer라면 항상 맨 아래에 나타난다. 개선된 구조는 댓글 tree를 렌더링하는 함수가 현재 댓글 바로 뒤에 해당 form을 조건부로 삽입하는 것이다.

```text
댓글 A
  └─ A 답글 작성 form (replyTargetId === A)
댓글 B
댓글 C
```

즉 “어느 댓글에 답하는가”는 state로 관리하되, “form이 어디에 나타나는가”는 해당 comment node를 렌더링하는 위치에서 결정한다. `PublicationCommentThread.tsx`로 comment thread를 추출한 이유도 tree·reply·delete·composer 책임을 상세 page에서 분리하기 위해서다.

### 7.2 좋아요·저장 버튼의 통일성

좋아요와 저장은 backend에서 서로 다른 원장이다. 하지만 사용자에게는 같은 engagement action group으로 보인다.

- 같은 높이·radius·padding
- 활성/비활성 색상 규칙 통일
- icon만으로 의미를 숨기지 않고 accessible label 제공
- count와 현재 active 상태를 함께 표시
- mutation 중 중복 클릭 방지
- 성공 후 서버가 반환한 상태를 source of truth로 사용

데이터 계약은 분리하되 visual component와 action group은 재사용하는 것이 적절한 절충이다.

### 7.3 삭제 버튼의 의미

“작성자만 삭제”처럼 어색한 문구는 권한 policy와 UI copy가 그대로 노출된 사례다.

```text
권한 판단: backend가 결정
표시 여부: frontend가 viewer와 author를 비교해 보조적으로 결정
최종 실패 처리: 403/404/409 ApiError를 화면에 표시
```

UI에서 숨겼다고 authorization이 끝난 것이 아니며, “작성자만 삭제”를 버튼에 그대로 쓰는 것도 좋은 UX가 아니다. “삭제” action, 확인 modal, 실패 메시지가 사용자 언어에 가깝다.

## 8. desktop 디자인 개선을 기술적으로 설명하기

이번 작업의 핵심은 색상 변경이 아니라 page들이 하나의 제품처럼 보이도록 공통 시각 언어를 정리한 것이다.

### 관찰한 문제

- 게시판 목록 border와 row가 지나치게 두꺼움
- 검색창이 다른 control과 다른 radius·height·focus style을 사용
- 홈/게시판 이동 link의 위치와 hierarchy가 불명확
- `COMMUNITY` 같은 개발용 label이 사용자 UI에 남음
- engagement action이 서로 다른 버튼 모양으로 보임
- desktop content width가 header와 page별로 다름
- 댓글 reply composer가 의미상 target과 떨어짐

### 개선 원칙

1. page마다 임의의 값 대신 공통 surface/card/control 규칙 사용
2. border는 정보 구획에 필요한 최소 무게만 사용
3. primary/secondary/quiet action hierarchy를 크기·배치로 구분
4. 제품 용어와 내부 분류 용어 분리
5. desktop 우선이더라도 keyboard focus와 accessible name 유지
6. screenshot보다 실제 interaction과 state 전환을 먼저 확인

1280px·1440px desktop screenshot baseline, browser E2E, hover/focus/click/keyboard interaction을 함께 확인한다. visual regression에서 픽셀이 다르다고 무조건 실패시키지 않고, 의도한 디자인 변경은 baseline을 갱신하며 overflow·layout shift·숨은 control은 원인을 조사한다.

## 9. 프론트엔드 성능을 Spring 개발자 언어로 번역하기

### 9.1 접속 속도와 페이지 이동 속도

```text
접속 속도
= HTML 수신 + JS/CSS 다운로드 + JS parse/execute
+ React mount + 첫 API response + 첫 의미 있는 화면

페이지 이동 속도
= route match + lazy chunk 다운로드
+ page API latency + render/paint
```

backend API p95가 좋아도 JS bundle이 크거나 page가 API를 순차 대기하면 느리다. route가 빨라도 backend가 2초면 data 화면은 늦다. 측정 지점을 분리해야 한다.

### 9.2 적용한 개선

#### A. route lazy loading과 preload

관리자·상세·form page를 lazy import해 최초 bundle에 모두 포함하지 않는다. 대신 navigation mouseenter/focus에서 자주 쓰는 route chunk를 제한적으로 preload한다.

trade-off는 처음 해당 route로 이동할 때 chunk 다운로드 지연이 생길 수 있다는 점이다. 모든 route를 미리 받으면 초기 접속이 다시 느려지므로 사용자 의도가 보이는 순간에만 preload한다.

#### B. GET cache와 request deduplication

동네·품종 catalog처럼 자주 변하지 않는 GET은 짧은 TTL 메모리 cache를 사용한다. 같은 key 요청이 동시에 들어오면 여러 fetch를 보내지 않고 하나의 Promise를 공유한다.

```text
첫 요청 → network
동시 두 번째 요청 → 첫 Promise 공유
TTL 안의 다음 요청 → memory cache
mutation 성공 → 관련 cache invalidate
```

이 cache는 새로고침 후 사라지고 여러 tab과 공유되지 않는다. PostgreSQL source of truth를 대체하지 않으며 stale 허용 범위가 작은 읽기에만 적용한다.

#### C. AbortController

검색어·route·filter가 바뀔 때 이전 request를 취소한다. 취소된 request를 오류 화면에 표시하지 않고 현재 요청만 state에 반영한다. 서버 transaction이 반드시 취소된다는 뜻은 아니며, browser response lifecycle 최적화다.

#### D. CSRF request waterfall 제거

각 mutation 전에 CSRF endpoint를 순차 호출하지 않는다. cookie가 있으면 즉시 사용하고, 동시에 필요한 요청은 하나의 Promise를 공유한다.

#### E. date formatter 재사용

render마다 `Intl.DateTimeFormat`을 새로 만들지 않고 formatter를 재사용한다. 작은 최적화지만 많은 row가 반복 렌더링될 때 불필요한 객체 생성을 줄인다.

### 9.3 instrumentation

`frontend/src/utils/performance.ts`는 개발 환경에서 다음을 기록한다.

- API path, status, duration
- route path, transition duration
- Largest Contentful Paint (LCP)
- Cumulative Layout Shift (CLS)

ID와 query를 그대로 기록하지 않도록 path를 normalize하고 최근 항목만 유지한다. 이는 local 개발·preview route의 진단 도구이지 production RUM은 아니다. production으로 확대하려면 privacy, sampling, release version, 실제 사용자 환경을 별도 설계해야 한다.

### 9.4 bundle budget

build 뒤 bundle 크기를 검사하는 script는 dependency 추가로 초기 다운로드가 커지는 회귀를 막는 guardrail이다. “현재 화면이 빠르다”는 최종 증명은 아니며, route/API timing과 browser measurement를 함께 봐야 한다.

## 10. backend 성능과 frontend 성능을 섞지 않는 법

| 질문 | 주로 보는 근거 |
| --- | --- |
| DB query가 느린가? | PostgreSQL EXPLAIN, k6 API p95 |
| API 응답이 느린가? | apiFetch timing, backend request timing, k6 |
| route chunk가 큰가? | Vite build output, bundle budget |
| 첫 화면이 늦은가? | LCP/FCP, browser measurement |
| 화면이 흔들리는가? | CLS, screenshot/visual regression |
| stale data가 보이는가? | Abort/cache tests, browser journey |

public feed DB index가 p95를 67.13ms에서 5.01ms로 낮춘 것은 backend/query 근거다. 이것만으로 “홈 화면이 13배 빨라졌다”고 말하면 안 된다. 홈에는 JS parse, route render, 다른 API, browser paint가 포함된다.

volunteer capacity contention p95 약 238.98ms도 backend mutation 비용이다. 버튼 클릭부터 toast까지는 CSRF, network, render, error handling을 별도로 측정해야 한다.

## 11. 테스트 피라미드와 frontend 검증

### 11.1 단위·component test

- `normalize`처럼 volatile field를 비교하는 순수 함수
- error state와 permission rendering
- reply composer가 target comment 아래 나타나는지
- API client가 CSRF header와 ProblemDetail을 처리하는지

### 11.2 flow test

`PublicationFlows.test.tsx`, `FeedFlows.test.tsx`, `AuthFlows.test.tsx` 등은 작은 사용자 여정을 검증한다.

```text
로그인 → 홈 feed → 게시글 상세
→ 댓글/답글 → 좋아요/저장
→ 뒤로가기 또는 다른 route
```

### 11.3 browser E2E

실제 browser에서 session cookie, CSRF cookie/header, lazy chunk, route fallback, keyboard focus, desktop overflow, 401/403/404/409 사용자 메시지를 확인한다.

### 11.4 parity test

Legacy와 새 frontend의 DOM이 같은지 비교하는 것이 아니라 같은 actor·state에서 관찰되는 의미를 비교한다. UUID, timestamp, signed URL 같은 volatile 값만 normalize하고 status·permission·business field는 그대로 비교한다.

## 12. 실제 실패에서 배운 frontend 교훈

### 실패 1. 로그인했는데 route 이동 후 풀림

- 원인 후보: React auth state 초기화, redirect/session 조회 timing
- 해결: 서버 session을 source of truth로 두고 AuthContext가 현재 session을 조회, login redirect 중 viewer를 초기화하지 않음
- 검증: 실제 browser session cookie로 login → route 이동 → protected API 재호출

### 실패 2. hover 시 메뉴가 안 열림

- 원인: hover target과 open state의 DOM 경계, pointer-events/z-index 문제
- 해결: hover뿐 아니라 focus/click/keyboard 경로 제공, submenu를 같은 interactive region에 배치
- 검증: mouse, keyboard focus, Escape, route 이동 후 close

### 실패 3. 답글 form이 맨 아래에 나타남

- 원인: state는 A를 기억하지만 form DOM은 list footer에 고정
- 해결: comment node 바로 아래에 조건부 composer 삽입
- 검증: 첫 댓글·마지막 댓글·중첩 답글·취소·route 이동

### 실패 4. 디자인이 기능마다 따로 놂

- 원인: page별 CSS 누적과 내부 label의 사용자 UI 노출
- 해결: 공통 control/surface/action hierarchy와 desktop width 정리
- 검증: 1280/1440 screenshot과 실제 interaction

### 실패 5. 테스트 오류를 성능 저하로 오해함

k6 write가 403이거나 moderator가 401이면 먼저 session cookie와 fixture hash를 확인해야 한다. harness/fixture 오류와 application latency를 분리하고 유효한 run만 채택한다.

## 13. 면접 질문 답변 뼈대

### Q1. 왜 Next.js/SSR로 바꾸지 않았나?

현재 기술 경계가 React + Vite이고 production Node server를 사용하지 않기 때문이다. 로그인·개인화·운영 화면은 CSR로 충분하고, 공개 community SEO/OG는 별도 계획으로 분리했다. 공개 운영에서 검색 유입이 중요해지면 Spring Boot가 Vite build artifact에 metadata를 주입하는 선택적 접근부터 검토한다.

### Q2. React에서 API 호출을 어디에 두었나?

page의 사용자 여정과 loading/error state는 feature component가 소유하고 transport 공통 규칙은 `api/client.ts`가 소유한다. 모든 API를 전역 store에 넣지 않아 page data ownership을 명확히 했다.

### Q3. frontend cache가 stale data를 만들지 않나?

그렇다. catalog와 짧은 수명의 반복 GET에만 적용하고 TTL을 둔다. mutation 성공 후 관련 key를 invalidate하며 게시글·권한·보안 상태의 최종 판단은 backend에 둔다.

### Q4. AbortController가 backend 요청도 취소하나?

항상 그렇지 않다. browser fetch와 response 처리를 취소하고 stale render를 막지만, 이미 서버에서 실행 중인 transaction이 반드시 rollback되는 것은 아니다. frontend cancellation과 backend cancellation을 구분한다.

### Q5. frontend 권한 처리는 안전한가?

frontend는 UX 보조일 뿐이다. 실제 authorization은 Spring Security, application/domain policy, DB constraint에서 수행한다. frontend는 401/403을 구분하고 action을 적절히 숨기지만 backend는 숨겨진 endpoint도 거부한다.

### Q6. 성능 개선을 어떻게 증명했나?

route/API/browser metric을 분리하고 deterministic fixture와 반복 script를 만들었다. frontend에서는 bundle budget, route timing, LCP/CLS를 기록했다. backend feed index는 별도 PostgreSQL/k6 결과로 p95 67.13ms→5.01ms를 검증했지만 전체 화면 속도 개선으로 과장하지 않는다.

### Q7. 왜 Redux나 React Query를 쓰지 않았나?

현재 규모에서는 API client의 짧은 GET cache, AbortSignal, page-local state로 충분했다. 전역 server-state invalidation과 pagination cache가 실제 복잡도를 넘으면 도입을 재평가한다. 라이브러리를 안 쓰는 것이 목표가 아니라 현재 복잡도에 맞춘 선택이다.

## 14. 아직 부족하거나 다음에 개선할 것

현재 desktop 주요 흐름은 많이 정리됐지만 “완벽하다”고 말할 수는 없다.

- 공개 커뮤니티 운영을 위한 SSR/SEO/OG는 아직 구현하지 않았고 계획만 있다.
- frontend measurement는 개발·preview 중심이며 실제 사용자 RUM과 production CDN 수치는 없다.
- route별 chunk split과 preload를 운영 환경에서 장기 cache까지 검증하지 않았다.
- GET memory cache는 tab 간 공유·server revalidation·mutation fan-out을 제공하지 않는다.
- 이미지 최적화, virtualized long feed, offline/poor network UX는 데이터 규모가 커질 때 재평가한다.
- mobile은 현재 우선순위가 낮으므로 desktop과 동일하게 검증됐다고 말하면 안 된다.
- 실제 public deployment의 Caddy/TLS/CDN/cache header와 crawler 동작은 별도 검증 대상이다.

신뢰도 높은 답변은 “다 고쳤다”가 아니라 다음처럼 말한다.

> 현재 desktop 주요 사용자 여정은 browser E2E와 visual baseline으로 검증했고, route/API timing과 bundle budget도 회귀 gate로 만들었습니다. 다만 실제 사용자 환경의 RUM, production CDN, 공개 SEO/OG, 대규모 feed virtualization은 아직 운영 전 검증 범위입니다.

## 15. 1주 복기 학습 순서

### 1일차: 브라우저와 React

- `main.tsx` → `App.tsx` → route → page 흐름 그리기
- 최초 진입과 SPA 이동 설명
- `useState`, `useEffect`, `useRef`가 현재 코드에서 쓰인 이유 찾기

### 2일차: API client와 인증

- `apiFetch`, `apiMutate`, `getCsrfToken`, `AuthContext` 읽기
- login → session cookie → current member → logout을 Network 관점에서 설명
- 401과 403 처리 차이 확인

### 3일차: 게시글과 댓글

- `PublicationDetailPage`, `PublicationCommentThread` 읽기
- comment tree와 reply target 그리기
- like/bookmark/delete의 UI와 backend contract 비교

### 4일차: 디자인과 접근성

- `styles.css`의 surface/control/action 규칙 찾기
- 1280/1440 screenshot과 browser 비교
- mouse, focus, keyboard, Escape, accessible name 확인

### 5일차: 성능

- `utils/performance.ts`, `client.ts` cache, `useAbortableRequest`, lazy route 읽기
- LCP, CLS, route timing, API timing, bundle size 구분
- backend p95와 화면 체감 속도를 섞지 않기

### 6일차: 검증과 실패

- `App.test.tsx`, `PublicationFlows.test.tsx`, `FeedFlows.test.tsx` 읽기
- auth·reply·hover·visual regression·browser smoke 연결
- 실패 사례 하나를 상황→선택→구현→검증→한계로 2분 답변

### 7일차: 모의 면접

1. SPA route 이동 시 로그인 상태는 어디에 있는가?
2. `useEffect` cleanup과 AbortController가 필요한 이유는?
3. 좋아요와 저장을 같은 UI로 만들되 backend는 왜 분리하는가?
4. 댓글 reply composer를 올바른 위치에 렌더링하려면?
5. lazy loading과 API loading의 차이는?
6. frontend cache의 stale 문제를 어떻게 제한했는가?
7. LCP가 나쁘면 backend부터 고칠 것인가?
8. 실제로 측정한 성능과 아직 측정하지 않은 성능은?
9. frontend에서 관리자 버튼을 숨기면 충분한가?
10. 어떤 조건에서 SSR을 도입할 것인가?

## 16. 최종 답변 템플릿

모든 frontend 질문은 다음 5문장으로 시작하면 된다.

1. **상황:** 사용자에게 어떤 문제가 보였는가?
2. **원인:** browser/React/API/backend 중 어느 경계의 문제였는가?
3. **선택:** 어떤 대안을 검토하고 무엇을 선택했는가?
4. **검증:** 어떤 test·browser journey·측정으로 확인했는가?
5. **한계:** 현재 환경에서 아직 증명하지 못한 것은 무엇인가?

예시:

> 댓글 답글이 맨 아래에 나타나는 문제가 있었습니다. reply target state는 올바랐지만 composer DOM이 목록 footer에 고정되어 있었기 때문에 위치가 분리된 것이 원인이었습니다. 댓글 node를 렌더링하는 자리에서 target일 때만 composer를 삽입하도록 comment thread component를 분리했습니다. 첫 댓글·마지막 댓글·중첩 답글과 desktop browser flow로 확인했습니다. 다만 현재 검증은 desktop 우선이며, 대규모 댓글 tree의 virtualization은 아직 범위가 아닙니다.

프론트엔드는 “화면을 그리는 부분”만이 아니다. URL, browser lifecycle, session cookie, API contract, asynchronous state, accessibility, performance budget, 사용자에게 보이는 오류를 연결하는 실행 계층이다. 이 연결을 실제 TownPet 코드와 검증 결과로 설명하면 Spring 중심 경력도 frontend 협업 역량으로 확장해서 보여줄 수 있다.

