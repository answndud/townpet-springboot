# TownPet Frontend Review, Scoring, and Rework Protocol

이 문서는 [`frontend_plan.md`](frontend_plan.md)의 네 Goal을 모두 구현한 뒤, 결과를 독립적으로 평가하고 부족한 항목을 다시 수정하기 위한 기준이다. 구현자의 “완료” 주장을 그대로 통과시키지 않고, fresh evidence와 실제 desktop user flow를 기준으로 판정한다.

## 리뷰 원칙

- 평가는 코드 설명이 아니라 실행 결과·diff·화면·측정값으로 한다.
- 검증하지 않은 항목은 0점 또는 `미검증`으로 기록한다.
- mobile은 이번 점수에 포함하지 않는다. desktop 범위를 mobile 개선 완료로 표현하지 않는다.
- P0/P1 기능·접근성·보안·데이터 손상 문제는 총점과 관계없이 통과할 수 없다.
- 기존 baseline보다 나빠진 수치는 개선으로 인정하지 않는다. trade-off가 있으면 근거와 승인된 예외를 기록한다.

## 100점 평가표

### G3 — 제품 범위·UX 완성도: 25점

| 항목 | 점수 | 통과 증거 |
|---|---:|---|
| 주요 route·actor matrix coverage | 5 | anonymous/member/moderator direct URL·refresh·next 이동 결과 |
| loading/empty/error/unauthorized/retry | 5 | route별 상태 screenshot 또는 browser assertion |
| 게시판 댓글·답글·reaction·bookmark 여정 | 5 | 실제 fixture write/cleanup와 DOM 위치 assertion |
| 거래·분실·돌봄·모임·관리자 여정 | 5 | 정상 write와 권한/validation 실패 evidence |
| dead-end·dead-button·문구·복귀 일관성 | 5 | route inventory와 수동 review 결과 |

### G4 — 코드 유지보수성과 리팩토링: 25점

| 항목 | 점수 | 통과 증거 |
|---|---:|---|
| 큰 컴포넌트의 책임 분리 | 5 | component/hook 구조와 변경 전후 파일 책임 비교 |
| shell·auth guard·route 책임 분리 | 5 | login/logout/refresh/expiry regression test |
| API/cache/abort/mutation 추상화 | 5 | duplicate request·stale response·invalidation test |
| 공통 UI·style token 재사용 | 5 | 반복 CSS/markup 감소와 visual regression |
| 타입·테스트·변경 용이성 | 5 | typecheck/test green, 신규 작은 변경의 영향 범위 설명 |

### G5 — 접근성·디자인 검증: 25점

| 항목 | 점수 | 통과 증거 |
|---|---:|---|
| semantic HTML·label·accessible name | 5 | automated scan과 DOM assertions |
| keyboard-only 주요 여정 | 5 | Tab/Enter/Escape/focus browser flow |
| focus·live region·form error 상태 | 5 | focus target, aria state, error announcement assertions |
| TownPet desktop visual parity | 5 | 1280×900/1440×900 screenshot review |
| 상태 화면과 nested comment composer 품질 | 5 | loading/empty/error/reply 위치 screenshot 및 review |

### G6 — 실측 기반 성능: 25점

| 항목 | 점수 | 통과 증거 |
|---|---:|---|
| 동일 조건 baseline/after 측정 | 5 | 3회 이상 p50/p75 결과와 환경 기록 |
| request waterfall·중복·stale response | 5 | request trace/count와 abort/cache test |
| bundle·render·asset 비용 | 5 | build report와 profiler/performance trace |
| desktop route/API budget | 5 | 대표 route별 budget 판정표 |
| CI regression gate와 재현성 | 5 | clean command와 실패 fixture output |

## 점수 판정

- 90–100: 현재 desktop 범위에서 release candidate 수준. 남은 P2만 backlog로 이동 가능
- 80–89: 기능은 유효하지만 재리뷰 후 개선이 필요. 완료 주장 금지
- 70–79: 포트폴리오 demo는 가능하나 제품 품질 목표 미달
- 0–69: 핵심 범위 또는 검증 증거가 부족해 Goal을 닫지 않음

추가 통과 조건:

- P0: 0건
- P1: 0건
- 주요 desktop route console error: 0건
- 실행하지 않은 필수 검증: 0건
- 어느 한 Goal도 20/25 미만이 아님

## 결함 등급

- P0: 로그인 불가, 권한 우회, 데이터 손상, 주요 write 불가, 화면 전체 crash
- P1: 핵심 여정 중단, 잘못된 사용자/권한 결과, keyboard 접근 불가, 반복되는 console error, 성능 budget 심각 초과
- P2: 일관성·가독성·비핵심 상태·중간 수준 성능 문제
- P3: polish, 문구, 미세한 시각 차이, 향후 mobile 범위

P0/P1은 점수와 무관하게 먼저 수정한다. P2는 총점 90점 미만이면 다시 수정하고, P3는 별도 backlog로 남길 수 있다.

## 실제 리뷰 절차

### 1. 기준선과 변경 범위 확인

- `git diff`와 Goal별 commit을 확인한다.
- 계획에 있는 Slice가 실제 파일·테스트·evidence로 연결됐는지 체크한다.
- 코드에만 존재하고 사용자 화면·검증으로 연결되지 않은 작업은 완료로 세지 않는다.

### 2. fresh automated gate 실행

```bash
cd /Users/alex/project/townpet-springboot/frontend
corepack pnpm install --frozen-lockfile
corepack pnpm typecheck
corepack pnpm test
corepack pnpm build
corepack pnpm test:e2e --project=chromium
```

E2E binary가 없는 경우에는 in-app browser와 API 직접 검증을 사용하되, E2E 미실행을 점수표에 명시한다. 대체 검증은 E2E 통과로 위장하지 않는다.

### 3. desktop user journey review

다음 순서로 fresh session에서 확인한다.

1. anonymous: home → public feed → search → post detail → login redirect
2. member: login → profile → feed → post create/edit/delete → comment/reply → reaction/bookmark → logout
3. member: marketplace, lost-found, care, gathering, local guide의 list/detail/create 또는 권한 차단
4. moderator: login → admin home → report/case queue → direct moderation → moderation logs
5. 각 단계에서 direct URL, 새로고침, 뒤로가기, network failure, empty result, validation failure 확인

각 여정의 기록 항목은 `route`, `actor`, `expected`, `observed`, `console`, `network`, `screenshot`, `issue id`다.

### 4. accessibility와 visual review

- 키보드만으로 header menu, login, search, form, comments, admin action을 수행한다.
- focus가 사라지는 지점, 잘못된 heading/label, 오류 전달 누락을 기록한다.
- 1280×900과 1440×900에서 shell·hero·card·form·reply composer·empty/error 화면을 비교한다.
- screenshot 차이는 정상적인 동적 값과 실제 regression을 분리해 기록한다.

### 5. performance review

- 동일 backend/fixture, 동일 viewport, 동일 cache 상태를 사용한다.
- 각 대표 route를 최소 3회 실행해 p50/p75를 기록한다.
- request count, duplicate request, longest task, bundle transfer, route settle time을 함께 본다.
- 숫자가 개선돼도 UX가 깜빡이거나 stale data가 보이면 성능 개선으로 승인하지 않는다.

## 재개선 루프

1. 점수표를 채우고 P0/P1/P2 issue를 심각도·route·owner·재현 command와 함께 기록한다.
2. P0/P1을 먼저 고친다.
3. 한 번의 commit은 하나의 Goal 또는 하나의 명확한 cross-cutting regression만 포함한다.
4. 수정된 Goal의 인접 테스트와 browser evidence를 다시 실행한다.
5. 전체 gate를 다시 실행하고 점수표를 갱신한다.
6. 총점 90점 이상, Goal별 20점 이상, P0/P1 0건이 될 때까지 반복한다.

같은 원인으로 두 번 실패하면 범위를 넓히지 말고 root cause를 문서에 추가한다. 세 번째 리뷰에서도 같은 blocker가 남으면 해당 항목을 `blocked`로 기록하고, 사용자에게 필요한 외부 환경이나 제품 결정을 요청한다.

## 최종 기록 양식

| Goal | 점수 | P0 | P1 | P2 | 증거 | 재리뷰 상태 |
|---|---:|---:|---:|---:|---|---|
| G3 제품·UX | 24/25 | 0 | 0 | 0 | route·actor 점검, direct guide search, topic link, domain write/error journeys, publication E2E | 재리뷰 통과; 비핵심 mutation 조합은 backlog |
| G4 유지보수성 | 23/25 | 0 | 0 | 0 | comment thread 추출, auth transition regression, typecheck/test | 통과 |
| G5 접근성·디자인 | 25/25 | 0 | 0 | 0 | DOM semantics, header keyboard, reply focus return, 1280/1440 visual diff | 통과 |
| G6 성능 | 24/25 | 0 | 0 | 0 | 3회 HTTP baseline, p50/p75, bundle budget, preview browser vitals harness | 성능 보고서 작성은 backend handoff 후 |
| 합계 | 96/100 | 0 | 0 | 0 | 아래 fresh gate와 Chromium 26/26 통과 | desktop release-candidate 기준 충족 |

## 2026-08-12 fresh review result

### Evidence

- Goal commits: `918ddb7`, `50fdbbf`, `75fd7d4`, `7fa2c4d`, `01638c3`, `599b0b1`, `db6ea04`, `a59a13c`, `c13ba95`, `7540ad5`, `0e336af`
- `corepack pnpm install --frozen-lockfile`: pass
- `corepack pnpm typecheck`: pass
- `corepack pnpm test`: 8 files / 24 tests pass
- `corepack pnpm build`: pass; entry JS 280.78KB raw / 86.59KB gzip, CSS 35.24KB
- `corepack pnpm test:e2e --project=chromium`: 26/26 pass after installing the missing local Chromium binary
- `e2e/desktop-visual.spec.ts`: exact 1280×900·1440×900 home/feed/form/reply screenshots 8/8 pass
- `e2e/domain-error-journeys.spec.ts`: marketplace·lost-found·care·gathering·moderator normal/error write journeys 9/9 pass
- `corepack pnpm measure:browser` on Vite preview: 7 representative routes measured for FCP/LCP/INP/CLS and route settle; CLS was 0 for all routes
- `node scripts/measure-performance.mjs`: 39 route/API samples, all 200; route median 1.72–4.46ms, API median 8.37–35.63ms; route p75 max 37.07ms, API p75 max 48.17ms
- In-app desktop review: topic routes, direct guide query, hospital review page, direct moderation queue, public feed and reply composer semantics checked; no visible error state or route dead-end found in the reviewed flows

### Rework completed from the review loop

- Fixed the direct moderation queue URL encoding boundary.
- Fixed the successful login transition race that returned users to `/login`.
- Replaced three dead home topic destinations and preserved `guides?q=` on direct navigation.
- Extracted the nested publication comment thread and restored focus to the reply trigger after cancel.
- Stabilized browser review fixtures: exact pathname login assertion, same-origin contexts, repeat-safe duplicate-title locators, and explicit comment DELETE response assertion.
- Added p75 output and route/API budget failures to the repeatable performance command.
- Added fixed desktop visual snapshots and removed auto-focus scroll nondeterminism from the reply screenshot.
- Added normal/error write coverage for marketplace, lost-found, care, gathering, and moderator direct actions.

### Remaining backlog

- Frontend performance records are maintained in `docs/frontend-performance/`; `docs/performance/` remains backend-only and is not used for frontend evidence.
- P2: extend coverage to every non-core mutation combination if the product scope expands.
- P3: mobile polish remains explicitly out of scope for this cycle.

최종 문서에는 실제 점수, 발견한 결함, 수정 commit, 재실행한 명령, 남은 P2/P3 backlog만 적는다. “검증 예정”, “동작할 것으로 예상”은 완료 근거가 아니다.
