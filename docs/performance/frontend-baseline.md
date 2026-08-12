# Frontend desktop performance baseline

## Summary

- 실행일: 2026-08-12
- 기준 commit: 작업 실행 시점의 local working tree
- 상태: `baseline`
- 측정 명령: `(cd frontend && corepack pnpm measure:perf)`
- 환경: local Vite dev server `http://localhost:5173`, local Spring API `http://localhost:8080`, 동일 demo fixture
- 반복: route/API별 순차 3회, 첫 요청은 cold 영향이 섞일 수 있음

이 측정은 HTTP shell/API 응답의 기준선이다. 브라우저의 실제 paint, React render, LCP, INP, CLS를 측정한 결과가 아니며 production CDN·TLS·VPS SLA로 해석하지 않는다.

## Route shell response

| route | status | median | p75 | samples | bytes |
|---|---:|---:|---:|---|---:|
| `/` | 200 | 4.30ms | 37.07ms | 37.07 / 4.30 / 4.04 | 651 |
| `/feed/guest` | 200 | 4.46ms | 6.25ms | 6.25 / 3.67 / 4.46 | 651 |
| `/marketplace` | 200 | 4.18ms | 4.72ms | 3.38 / 4.18 / 4.72 | 651 |
| `/lost-found` | 200 | 3.15ms | 6.39ms | 3.15 / 2.97 / 6.39 | 651 |
| `/guides?q=산책` | 200 | 3.25ms | 5.70ms | 3.25 / 5.70 / 3.01 | 651 |
| `/gatherings` | 200 | 2.53ms | 2.85ms | 2.53 / 1.69 / 2.85 | 651 |
| `/care` | 200 | 1.72ms | 1.92ms | 1.72 / 1.92 / 1.64 | 651 |

## Public API response

API 샘플은 브라우저의 session·CSRF mutation이 아니라 public GET read 경로만 측정한다.

| endpoint | status | median | p75 | samples | bytes |
|---|---:|---:|---:|---|---:|
| `/api/v1/feed?audience=GLOBAL&limit=20&scope=ALL` | 200 | 10.20ms | 21.97ms | 21.97 / 9.69 / 10.20 | 6,908 |
| `/api/v1/marketplace/listings?limit=30` | 200 | 11.68ms | 12.74ms | 11.68 / 12.74 / 11.20 | 751 |
| `/api/v1/lost-found/alerts?limit=20` | 200 | 35.63ms | 48.17ms | 48.17 / 35.63 / 29.80 | 1,134 |
| `/api/v1/local-resources?query=산책` | 200 | 10.03ms | 11.23ms | 10.03 / 11.23 / 9.86 | 1,021 |
| `/api/v1/gatherings` | 200 | 8.42ms | 11.16ms | 11.16 / 7.73 / 8.42 | 1,528 |
| `/api/v1/care/requests` | 200 | 8.37ms | 8.53ms | 8.37 / 8.53 / 6.79 | 482 |

원본 JSON은 명령 실행 output이며, credential·session·개인정보를 저장하지 않는다. 이번 기준선은 API status, median, samples, bytes를 기록한다. 반복 수가 3회라 p75는 별도 해석하지 않는다.

## Budget for this desktop scope

| metric | budget | current evidence |
|---|---:|---|
| entry JS raw | 320KB | build budget script로 검사 |
| entry JS gzip | 100KB | build budget script로 검사 |
| entry CSS raw | 50KB | build budget script로 검사 |
| route shell median | 100ms local | 현재 1.72–4.46ms; p75 최대 37.07ms |
| API median | 250ms local | 현재 8.37–35.63ms; p75 최대 48.17ms |
| unexpected public GET 5xx | 0 | fresh run의 39개 sample이 모두 200; script도 5xx/budget 초과에서 실패 |

## Limitations and next run

- `measure:perf`는 HTTP fetch 기준이며 browser render timing을 대체하지 않는다.
- Playwright Chromium binary가 설치되지 않은 환경에서는 full E2E와 Lighthouse를 실행했다고 표현하지 않는다.
- 다음 실험에서는 production `vite preview` 또는 Spring static artifact, cache cold/warm 분리, route settle timing, request count, LCP/CLS를 함께 기록한다.
