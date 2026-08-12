# TownPet Frontend Desktop Improvement Plan

이 문서는 `frontend/`의 데스크톱 품질을 네 개의 독립 Goal로 개선하기 위한 실행 계획이다. 기존 구현을 완료로 가정하지 않고, 각 Goal마다 실제 화면·코드·측정·자동화 증거를 다시 만든다.

## 범위와 기준

- 대상: React 19 + TypeScript + Vite frontend와 Spring API에 연결된 데스크톱 사용자 여정
- 우선 viewport: 1280×900, 1440×900
- 이번 범위 제외: 모바일 레이아웃 최적화와 mobile p75 SLO 달성. 단, 데스크톱 변경으로 기존 반응형 CSS를 깨뜨리지는 않는다.
- 주요 actor: anonymous, 일반 회원 `demo-member-1@townpet.local`, 운영 관리자 `demo-moderator@townpet.local`
- 기준 문서: [`docs/PRD.md`](PRD.md), [`docs/TRD.md`](TRD.md), [`docs/parity/matrix.md`](parity/matrix.md), [`docs/parity/shell.md`](parity/shell.md)
- 현재 주요 위험: 큰 TSX 컴포넌트, route별 상태 UI 편차, 수동 검증에 의존하는 브라우저 여정, 실사용 성능 수치 부족

## 실행 규칙

1. 아래 Goal 순서와 Slice 순서를 따른다. 한 Goal이 끝날 때마다 해당 Goal 전용 commit을 만든다.
2. 기존 backend·PLAN 범위를 수정하지 않는다. API 계약 변경이 필요하면 먼저 backend owner와 계약을 확인하고 frontend에서 임의로 우회하지 않는다.
3. 기능 변경은 정상 흐름뿐 아니라 loading, empty, error, unauthorized, retry, duplicate-submit 상태를 함께 닫는다.
4. 각 Slice의 검증은 fresh 결과만 완료 근거로 인정한다. 실행하지 않은 E2E·접근성·성능 측정은 통과로 기록하지 않는다.
5. 모든 Goal 완료 후 [`docs/frontend_review.md`](frontend_review.md)의 점수표로 독립 재리뷰하고, 기준 미달 항목을 다시 수정한다.

## 기준선 기록

- 기존 검증: `corepack pnpm test`, `corepack pnpm typecheck`, `corepack pnpm build`
- 현재 bundle budget: entry JS 320KB raw / 100KB gzip, CSS 50KB
- 현재 테스트: Vitest 7 files / 22 tests
- 기준선에서 별도로 기록할 값:
  - 주요 route의 최초 content와 settled content까지의 시간
  - API request 수·중복·waterfall·실패율
  - 주요 화면의 console error/warning
  - route별 loading/empty/error/unauthorized 화면 유무
  - keyboard focus 순서와 접근성 위반 수

## Goal 3 — 제품 범위·UX 완성도

### 목표

주요 제품 여정이 화면 단위로 끊기지 않고, TownPet의 shell·문구·상태·상호작용 규칙이 일관되게 보이도록 만든다. “페이지가 열린다”가 아니라 사용자가 다음 행동을 이해하고 끝까지 완료할 수 있어야 한다.

### G3.1 — 데스크톱 route와 actor 여정 inventory 고정

- 파일: `frontend/src/App.tsx`, `frontend/e2e/*.spec.ts`, `docs/parity/matrix.md`
- 변경:
  - anonymous/member/moderator별 주요 route와 허용·차단 결과를 표로 고정한다.
  - direct URL, 새로고침, 뒤로가기, query string, 로그인 후 `next` 이동을 각각 확인한다.
  - route가 `placeholder`, dead-end, 잘못된 상대 경로, 인코딩된 slash로 API를 깨뜨리지 않는지 검사한다.
- 검증:
  - route inventory 기반 브라우저 smoke
  - `corepack pnpm test:e2e --project=chromium`
  - 실행 불가 시 in-app browser와 API curl을 대체 증거로 남기고, E2E 환경 복구를 별도 blocker로 기록한다.
- 완료:
  - 주요 public/member/admin route에 owner actor, expected heading, expected action, failure state, test id가 연결된다.

### G3.2 — loading·empty·error·unauthorized 상태를 여정별로 통일

- 파일: `frontend/src/hooks/useAbortableRequest.ts`, `frontend/src/App.tsx`, `frontend/src/features/**`, `frontend/src/Admin*.tsx`, `frontend/src/styles.css`
- 변경:
  - 모든 목록·상세·mutation에 명시적인 loading, empty, error, retry, success 상태를 둔다.
  - 권한 만료와 네트워크 오류를 같은 “데이터 없음” 화면으로 숨기지 않는다.
  - submit 중 버튼 잠금, 중복 클릭 방지, 실패 후 재시도, 입력값 보존 규칙을 통일한다.
  - 화면 전환 중 stale response가 새 route의 데이터를 덮지 않도록 abort와 route key를 점검한다.
- 검증:
  - API 401/403/404/409/500 mock test
  - 각 feature의 정상·빈 결과·실패·재시도 Vitest
  - 브라우저에서 느린 응답과 새로고침을 포함한 desktop smoke
- 완료:
  - 사용자가 현재 상태와 다음 행동을 모든 주요 여정에서 알 수 있고, console error 없이 복구할 수 있다.

### G3.3 — 게시판 상호작용을 제품 규칙에 맞게 완성

- 파일: `frontend/src/features/publication/PublicationDetailPage.tsx`, `frontend/src/features/publication/PublicationFeedPage.tsx`, `frontend/src/api/client.ts`, `frontend/src/PublicationFlows.test.tsx`
- 변경:
  - 댓글의 답글 작성기는 해당 댓글 바로 아래에 삽입하고, 다른 댓글·페이지 하단으로 이동하지 않도록 유지한다.
  - 댓글/답글 등록·취소·삭제, 좋아요·북마크의 optimistic/pending/error 상태와 상태 재조회 규칙을 통일한다.
  - 목록→상세→수정→삭제→목록 복귀와 로그인 요구 링크의 `next`/anchor를 보존한다.
  - 게시글이 삭제·비공개·권한 없음일 때 제품 문구와 복귀 링크를 일관되게 보여준다.
- 검증:
  - 댓글·답글을 실제 fixture 계정으로 등록 후 정리하는 browser flow
  - 답글 form의 DOM 위치 assertion
  - reaction/bookmark toggle의 on→off 회귀 test
- 완료:
  - 게시판의 대표 정상·실패·권한 흐름이 하나의 일관된 interaction model을 사용한다.

### G3.4 — 거래·분실·돌봄·모임·지역정보·운영 화면의 제품 마감

- 파일: `frontend/src/features/marketplace/**`, `frontend/src/features/lostfound/**`, `frontend/src/features/care/**`, `frontend/src/features/gathering/**`, `frontend/src/features/localcare/**`, `frontend/src/Admin*.tsx`
- 변경:
  - 각 feature의 list/detail/create/edit 흐름에서 제목, 상태 chip, 날짜, owner action, back link, empty/error copy를 공통 규칙으로 맞춘다.
  - 사용자가 수행할 수 없는 action은 숨기거나 disabled 이유를 제공한다.
  - 관리자 queue/action 결과에 성공·실패·재조회 상태를 명시한다.
  - backend가 지원하지 않는 기능을 성공처럼 보이는 버튼이나 빈 placeholder로 남기지 않는다.
- 검증:
  - member/moderator fixture 기반 route·action matrix
  - 주요 화면 screenshot 비교와 console log 확인
  - 각 feature의 최소 1개 정상 write와 1개 권한/validation 실패
- 완료:
  - 주요 도메인에 dead button, dead-end route, 의미 없는 빈 성공 상태가 없다.

### G3.5 — Goal 3 gate

- 검증: `corepack pnpm test`, `corepack pnpm typecheck`, `corepack pnpm build`, desktop browser smoke, `git diff --check`
- 완료: route matrix의 P0/P1 기능 공백 0건, 주요 여정에서 console error 0건, Goal 3 commit 생성

## Goal 4 — 코드 유지보수성과 리팩토링

### 목표

기능을 추가할 때 한 파일의 거대한 JSX와 화면별 복사 코드에 의존하지 않도록, 변경 지점을 예측할 수 있는 feature 구조와 작은 상태·API 단위를 만든다. 추상화를 늘리는 것이 목적이 아니라 중복과 책임 혼합을 줄이는 것이 목적이다.

### G4.1 — PublicationDetailPage를 책임 단위로 분리

- 파일: `frontend/src/features/publication/PublicationDetailPage.tsx`, 신규 `frontend/src/features/publication/components/**`, 신규 `frontend/src/features/publication/hooks/**`
- 변경:
  - detail header, author actions, reaction bar, comments, comment form, reply form, relationship actions, media preview를 독립 컴포넌트로 분리한다.
  - fetch/submit/retry/abort 상태를 hook 또는 작은 command 함수로 이동한다.
  - 부모는 route data와 use-case orchestration만 소유한다.
  - 댓글 tree의 parent/reply 관계와 form anchor를 타입으로 표현한다.
- 검증: 기존 `PublicationFlows.test.tsx` 전부 통과, 댓글·답글 DOM 위치 회귀 test, typecheck
- 완료: `PublicationDetailPage.tsx`가 화면 orchestration 수준으로 줄고 각 child가 단독 테스트 가능한 입력을 가진다.

### G4.2 — App shell과 route guard를 읽기 쉽게 정리

- 파일: `frontend/src/App.tsx`, `frontend/src/auth/AuthContext.tsx`, 신규 `frontend/src/routing/**`, 신규 `frontend/src/components/shell/**`
- 변경:
  - route table, shell navigation, guard, auth transition을 별도 책임으로 나눈다.
  - 로그인 성공 직후·로그아웃 직후·세션 만료·권한 변경의 상태 전이를 하나의 명시적인 모델로 만든다.
  - route path 문자열과 feature label의 중복 정의를 줄이되, 실제 URL 계약은 한 곳에서 확인 가능하게 한다.
- 검증: member/moderator login redirect, logout, refresh, direct URL, unauthorized route browser test
- 완료: 인증 경합과 route guard redirect loop가 재현되지 않고, 신규 route 추가 위치가 명확하다.

### G4.3 — API query/mutation 패턴과 오류 처리를 정리

- 파일: `frontend/src/api/client.ts`, `frontend/src/hooks/useAbortableRequest.ts`, feature API 호출부
- 변경:
  - GET cache, abort, invalidation, mutation pending, `ApiError` mapping의 책임을 명확히 나눈다.
  - 같은 endpoint를 feature에서 반복 조합하지 않고 API client가 URL·DTO·cache key를 소유하게 한다.
  - mutation 후 관련 cache만 invalidate하고 전체 reload로 대체하지 않는다.
  - query key와 route key에 raw user input이 무분별하게 들어가지 않도록 normalize한다.
- 검증: API client unit test, duplicate request/cache hit test, abort stale response test, typecheck
- 완료: feature 코드가 fetch 세부사항을 몰라도 되고, 오류 메시지와 retry 정책이 예측 가능하다.

### G4.4 — 반복 UI와 스타일 토큰을 정리

- 파일: `frontend/src/styles.css`, feature page files, 신규 `frontend/src/components/ui/**`
- 변경:
  - button, field, status chip, card, empty/error state, page heading의 반복 markup과 CSS를 최소 공통 컴포넌트로 정리한다.
  - 색상·간격·radius·shadow·focus ring을 CSS custom property로 모으고 임의 값 사용을 줄인다.
  - 한 줄 JSX와 복잡한 inline handler를 읽기 가능한 함수/markup으로 바꾼다.
- 검증: visual smoke, typecheck, CSS diff review, no unused component check
- 완료: 공통 UI 수정이 한 곳에서 전파되고, feature별 스타일 편차가 의도적인 차이로만 남는다.

### G4.5 — Goal 4 gate

- 검증: `corepack pnpm test`, `corepack pnpm typecheck`, `corepack pnpm build`, E2E/smoke, 변경 전후 파일 책임 review
- 완료: 신규 기능을 추가할 때 수정해야 할 파일 수와 책임이 줄고, 기존 behavior regression 0건, Goal 4 commit 생성

## Goal 5 — 접근성·디자인 검증

### 목표

데스크톱 사용자가 마우스 없이도 주요 흐름을 완료할 수 있고, 화면의 의미·focus·오류·상태 변화가 보조기술에 전달되도록 한다. 동시에 parity 기준의 TownPet 시각 언어를 화면 전체에 일관되게 적용한다.

### G5.1 — 의미 구조와 이름(name) 완성

- 파일: `frontend/src/App.tsx`, `frontend/src/AuthPageLayout.tsx`, 모든 feature page, `frontend/src/styles.css`
- 변경:
  - heading hierarchy, landmark(main/nav/header/footer), form label/id, button/link 의미를 route별로 정리한다.
  - 아이콘·기호만 있는 reaction/menu/close control에 accessible name과 pressed/expanded state를 넣는다.
  - table/list/card가 시각적으로만 구분되지 않도록 list semantics와 status text를 보완한다.
  - 날짜·상태 chip·오류 메시지의 읽기 순서를 확인한다.
- 검증: axe 또는 동등한 automated accessibility scan, DOM semantic assertions, typecheck
- 완료: 주요 route에 unlabeled control, broken label, heading skip, interactive div가 없다.

### G5.2 — keyboard·focus·menu·form 오류 흐름

- 파일: `frontend/src/App.tsx`, `frontend/src/LoginPage.tsx`, form pages, comment/reply components, `frontend/src/styles.css`
- 변경:
  - header dropdown을 Tab/Enter/Escape로 열고 닫으며 focus가 메뉴 밖으로 유실되지 않게 한다.
  - route 전환·inline reply open·dialog/confirm·validation error 뒤 focus 위치를 정의한다.
  - `aria-expanded`, `aria-controls`, `aria-live`, `aria-busy`, `aria-invalid`, `aria-describedby`를 실제 상태와 연결한다.
  - focus ring이 배경과 충분히 대비되고 hover만으로 의미가 전달되지 않게 한다.
- 검증: Playwright keyboard-only flow, focus target assertion, reduced-motion browser check
- 완료: 로그인, 게시글 탐색, 댓글/답글, 검색, 관리자 queue를 키보드만으로 완료할 수 있다.

### G5.3 — TownPet parity와 desktop visual consistency

- 파일: `frontend/src/styles.css`, `frontend/src/App.tsx`, `frontend/src/features/**`, `docs/parity/shell.md`, screenshot fixtures
- 변경:
  - shell header, page hero, CTA hierarchy, card density, chip, date/meta, form spacing을 parity 기준과 비교한다.
  - route별 임의 palette·spacing·button tone을 token 기반으로 통일한다.
  - loading/empty/error/permission 화면도 정상 화면과 같은 visual language를 사용한다.
  - 댓글·답글 nested indentation, inline composer, action placement를 데스크톱 기준으로 고정한다.
- 검증: 1280×900/1440×900 screenshot baseline과 review, visual diff threshold 기록
- 완료: 정상·빈·오류·권한 화면이 서로 다른 제품처럼 보이지 않고, parity 차이는 문서화된 의도적 차이만 남는다.

### G5.4 — Goal 5 gate

- 검증: automated accessibility scan, keyboard E2E, screenshot review, `corepack pnpm test`, `corepack pnpm build`
- 완료: P0/P1 accessibility issue 0건, 주요 desktop route keyboard flow 통과, Goal 5 commit 생성

## Goal 6 — 실측 기반 성능 개선

### 목표

접속 속도와 route 이동 속도를 감으로 개선하지 않고, 동일한 데이터·viewport·네트워크 조건에서 baseline과 after 값을 기록한다. 이 Goal은 mobile SLO 달성을 주장하지 않으며 desktop 성능 회귀를 막는 데 집중한다.

### G6.1 — 측정 harness와 성능 예산 고정

- 파일: `frontend/src/utils/performance.ts`, `frontend/scripts/check-bundle.mjs`, 신규 `frontend/scripts/measure-performance.mjs`; 프론트엔드 evidence는 백엔드 기록과 분리된 `docs/frontend-performance/baseline-YYYY-MM-DD.md`에 기록한다.
- 변경:
  - route resolve time, API duration/count, JS/CSS transfer, first content, settled content를 동일 schema로 기록한다.
  - public home/feed, login→profile, marketplace, post detail, admin을 대표 route로 고정한다.
  - desktop budget을 문서화한다: initial entry JS gzip, CSS, route transition p75, API read/write p75, long task count.
  - user input·credential·session·정확 위치가 측정 payload/log에 들어가지 않게 한다.
- 검증: controlled local run 3회 이상, 결과 median/p75 기록, bundle script
- 완료: baseline과 after를 동일 조건으로 비교할 수 있고, 측정값 없는 성능 주장을 하지 않는다.

### G6.2 — request waterfall·cache·stale response 제거

- 파일: `frontend/src/api/client.ts`, `frontend/src/auth/AuthContext.tsx`, feature data hooks
- 변경:
  - route 진입 시 순차 요청을 병렬화하고, auth/catalog/shared GET의 중복 요청을 dedupe한다.
  - mutation 후 필요한 key만 invalidate하고, abort된 route의 stale response를 화면에 반영하지 않는다.
  - prefetch는 hover/focus/intent가 명확한 route에만 적용하고 불필요한 eager fetch를 제거한다.
- 검증: request count/waterfall assertion, slow API navigation, abort test, API timing snapshot
- 완료: 대표 route에 불필요한 순차 fetch가 없고, 이동 중 이전 route 데이터가 깜빡이거나 덮어쓰지 않는다.

### G6.3 — render·bundle·asset 비용 개선

- 파일: `frontend/src/App.tsx`, `frontend/src/features/**`, `frontend/src/styles.css`, `frontend/vite.config.ts`
- 변경:
  - route lazy chunk, heavy component render, long list, unstable object/function 전달을 측정 후 필요한 곳만 개선한다.
  - 이미지·폰트·SVG·CSS import 비용과 cache header를 확인한다.
  - 근거 없이 memoization을 전체 적용하지 않고 profiler/measurement가 있는 component만 최적화한다.
- 검증: production build, bundle budget, React profiler 또는 browser performance trace, desktop route p75
- 완료: 초기 bundle과 대표 route transition이 baseline보다 악화되지 않고, 개선된 비용의 원인이 설명 가능하다.

### G6.4 — 성능 회귀를 CI에 연결

- 파일: `frontend/package.json`, `frontend/scripts/check-bundle.mjs`, `.github/workflows/**`; 최종 프론트엔드 수치는 `docs/frontend-performance/`의 날짜별 baseline에 추가한다. `docs/performance/`는 백엔드 성능 기록 전용으로 유지한다.
- 변경:
  - build 시 bundle budget을 유지하고, 측정 script가 실행 가능한 환경에서는 route/API budget을 검증한다.
  - 실패 output에 초과 asset·route·request와 재현 command를 표시한다.
  - noisy한 네트워크 측정은 hard gate와 report-only를 구분해 flaky green을 만들지 않는다.
- 검증: clean install/build, CI-equivalent command, budget exceed fixture
- 완료: bundle/route 성능 회귀가 PR에서 조기에 드러나고, 실패 원인을 재현할 수 있다.

### G6.5 — Goal 6 gate

- 검증: `corepack pnpm test`, `corepack pnpm typecheck`, `corepack pnpm build`, controlled desktop performance run. 결과 문서는 `docs/frontend-performance/`에만 작성하고, `docs/performance/`에는 프론트엔드 결과를 추가하지 않는다.
- 완료: 대표 desktop route의 baseline/after p50·p75가 기록되고, budget 초과 0건, Goal 6 commit 생성

## 최종 통합 gate

1. `corepack pnpm install --frozen-lockfile`
2. `corepack pnpm typecheck`
3. `corepack pnpm test`
4. `corepack pnpm build`
5. `corepack pnpm test:e2e --project=chromium`
6. backend가 켜진 상태에서 demo member/moderator desktop smoke
7. keyboard/accessibility scan과 screenshot review
8. 성능 measurement 3회와 `docs/frontend_review.md` 점수화
9. P0/P1 재수정 후 위 관련 gate 재실행

최종 완료는 모든 Goal의 완료 조건, desktop console error 0건, P0/P1 issue 0건, 평가 점수 90점 이상, 각 Goal commit과 evidence 문서가 존재할 때만 표현한다. 모바일 완료나 전체 서비스 release 완료를 의미하지 않는다.
