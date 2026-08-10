# React·Vite parity shell 면접 노트

## 왜 shell을 먼저 만들었나

backend 교체와 UI 교체를 동시에 하면 실패 원인이 API인지 layout인지 알기 어렵다. 공개 shell을 먼저 고정하면 사용자가 인지하는 브랜드·navigation·responsive 기준을 작은 smoke로 보호하면서 domain vertical slice를 독립적으로 추가할 수 있다.

## Vite를 선택한 이유

Next.js server runtime을 제거한다는 결정에 맞춰 Vite는 정적 SPA 개발 서버와 production asset build만 담당한다. Spring Boot가 API와 필요하면 HTML entry를 소유하고, Vite는 `/api` proxy로 local 개발 경험을 제공한다. SSR/서버 전용 query를 frontend에 다시 넣지 않는다.

## 면접 답변 포인트

- 기존 코드를 전부 복사하지 않고 관찰 가능한 parity 기준(logo, palette, header, CTA)을 먼저 추출했다.
- 의미 기반 Playwright selector를 사용해 CSS class 변경과 기능 회귀를 분리했다.
- generated OpenAPI client와 feature component 사이에 transport seam을 두어 API contract drift를 막는다.
- visual baseline은 fixture·Spring endpoint가 준비된 다음 단계에서 승인해 false positive를 줄인다.
