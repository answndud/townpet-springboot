# React·Vite shell parity 기준

P1.5는 기존 Next.js server runtime을 복제하지 않고, 사용자가 먼저 보는 공개 shell의 관찰 가능한 기준을 고정한다.

## 이관한 기준

| 기준 | Legacy 근거 | Vite 위치 |
|---|---|---|
| TownPet 로고와 공개 헤더 | `townpet/app/src/components/navigation/app-shell-header.tsx` | `frontend/src/App.tsx` |
| 배경 격자·blue palette·card shadow | `townpet/app/src/app/globals.css` | `frontend/src/styles.css` |
| 홈 제목·설명·primary CTA | `townpet/app/src/app/page.tsx` | `frontend/src/App.tsx` |
| 공개 주요 이동 `/feed/guest`, `/login` | header/page route contract | `frontend/e2e/parity-shell.spec.ts` |

## 검증 규칙

- desktop Chromium과 mobile Pixel 5에서 동일한 primary journey가 열려야 한다.
- 로고 alt, heading, CTA href와 공개 로그인 test id는 의미 기반 selector로 검증한다.
- visual baseline은 실제 Spring API·fixture가 연결되는 P1.6에서 승인한다. P1.5에서는 shell smoke를 먼저 닫아 화면 이질감이 생기는 원인을 분리한다.
- feature API URL은 `frontend/src/api/client.ts` transport seam을 거쳐야 하며 component에서 fetch URL을 조합하지 않는다.
