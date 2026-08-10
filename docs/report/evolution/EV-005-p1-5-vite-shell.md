# EV-005 · P1.5 React·Vite shell과 UI parity smoke

## 문제와 한계

Next.js UI를 바로 Spring MVC template로 다시 만들면 server runtime과 화면 구조가 함께 바뀌어 parity 원인을 분리하기 어렵다. 반대로 React·Vite shell 없이 API부터 옮기면 기존 사용자가 보는 로고·header·CTA·responsive 동작을 회귀시킬 수 있다.

## 선택과 구현

React 19·Vite 6·React Router 기반의 독립 frontend를 만들고, 기존 TownPet에서 공개 header·logo·blue palette·grid background·home CTA만 선별 이식했다. Vite dev server는 `/api`를 Spring Boot로 proxy하고, `src/api/client.ts`는 이후 OpenAPI generated client를 주입할 transport seam으로 둔다. Playwright는 desktop Chromium과 mobile Pixel 5에서 같은 shell journey를 검사한다.

## 검증 결과

`corepack pnpm typecheck`, `corepack pnpm test`, `corepack pnpm build`, `corepack pnpm test:e2e`가 통과했다. E2E는 2개 journey를 2개 viewport에서 총 4개 통과했다. 첫 실행에서 브라우저가 없어 설치가 필요했고, Node 20.13 경고를 피하기 위해 Vite 6으로 고정했다.

## 다음에 측정할 것

P1.6에서 49개 URL inventory와 Spring/legacy dual target을 연결하고, 실제 fixture 화면의 screenshot baseline과 승인 threshold를 추가한다.
