# TownPet frontend performance

이 디렉터리는 React·Vite 프론트엔드의 데스크톱 성능 기록 전용이다. 백엔드 성능 기록 디렉터리인 [`docs/performance`](../performance/)와 분리하며, 백엔드 query plan·load test·JVM 수치는 이곳에 복사하지 않는다.

## 기록 목록

- [2026-08-12 desktop baseline](baseline-2026-08-12.md)

## 측정 도구

- HTTP route/API: [`frontend/scripts/measure-performance.mjs`](../../frontend/scripts/measure-performance.mjs)
- Browser FCP/LCP/INP/CLS·route settle: [`frontend/scripts/measure-browser-performance.mjs`](../../frontend/scripts/measure-browser-performance.mjs)
- bundle budget: [`frontend/scripts/check-bundle.mjs`](../../frontend/scripts/check-bundle.mjs)

모바일 성능은 현재 제품 우선순위에서 제외한다. 새로운 수치는 동일한 viewport·서버 상태·반복 수를 유지하고 날짜별 baseline 파일로 추가한다.
