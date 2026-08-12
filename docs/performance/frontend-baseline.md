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

| route | status | median | samples | bytes |
|---|---:|---:|---|---:|
| `/` | 200 | 5.09ms | 32.73 / 5.09 / 2.69 | 651 |
| `/feed/guest` | 200 | 5.30ms | 5.46 / 5.30 / 3.86 | 651 |
| `/marketplace` | 200 | 4.50ms | 5.61 / 4.50 / 4.22 | 651 |
| `/lost-found` | 200 | 5.99ms | 5.60 / 6.41 / 5.99 | 651 |
| `/guides?q=산책` | 200 | 10.81ms | 10.81 / 11.33 / 5.68 | 651 |
| `/gatherings` | 200 | 4.13ms | 4.13 / 3.33 / 4.91 | 651 |
| `/care` | 200 | 1.93ms | 1.75 / 1.93 / 2.13 | 651 |

## Public API response

API 샘플은 브라우저의 session·CSRF mutation이 아니라 public GET read 경로만 측정한다.

| endpoint | status | median | samples | bytes |
|---|---:|---:|---|---:|
| `/api/v1/feed?audience=GLOBAL&limit=20&scope=ALL` | 200 | 12.16ms | 23.40 / 11.41 / 12.16 | 6,908 |
| `/api/v1/marketplace/listings?limit=30` | 200 | 11.90ms | 7.94 / 11.90 / 14.38 | 751 |
| `/api/v1/lost-found/alerts?limit=20` | 200 | 7.87ms | 11.74 / 7.81 / 7.87 | 1,134 |
| `/api/v1/local-resources?query=산책` | 200 | 6.65ms | 6.65 / 9.30 / 6.51 | 1,021 |
| `/api/v1/gatherings` | 200 | 13.68ms | 14.70 / 13.68 / 10.10 | 1,528 |
| `/api/v1/care/requests` | 200 | 7.43ms | 8.46 / 6.87 / 7.43 | 482 |

원본 JSON은 명령 실행 output이며, credential·session·개인정보를 저장하지 않는다. 이번 기준선은 API status, median, samples, bytes를 기록한다. 반복 수가 3회라 p75는 별도 해석하지 않는다.

## Budget for this desktop scope

| metric | budget | current evidence |
|---|---:|---|
| entry JS raw | 320KB | build budget script로 검사 |
| entry JS gzip | 100KB | build budget script로 검사 |
| entry CSS raw | 50KB | build budget script로 검사 |
| route shell median | 100ms local | 현재 1.93–10.81ms |
| unexpected public GET 5xx | 0 | fresh run의 모든 route/API sample이 200; script도 5xx에서 실패 |

## Limitations and next run

- `measure:perf`는 HTTP fetch 기준이며 browser render timing을 대체하지 않는다.
- Playwright Chromium binary가 설치되지 않은 환경에서는 full E2E와 Lighthouse를 실행했다고 표현하지 않는다.
- 다음 실험에서는 production `vite preview` 또는 Spring static artifact, cache cold/warm 분리, route settle timing, request count, LCP/CLS를 함께 기록한다.
