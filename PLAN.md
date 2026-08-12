# PLAN.md

## Goal

Legacy TownPet의 관찰 가능한 제품 동작을 Java 25·Spring Boot·PostgreSQL·React/Vite로 재현한다. 완료는 page/API route가 존재하는 상태가 아니라, 49개 page와 55개 API route가 의도적으로 `verified` 또는 ADR로 설명된 `excluded`가 되고, 각 `verified` 항목의 데이터·권한·상태·오류·반응형 화면이 대표 사용자 여정에서 동일하게 동작하는 상태로 정의한다.

실제 Legacy 개인정보 migration과 Kakao/Naver 인증은 현재 제품 범위가 아니며, 실제 VPS 공개 운영은 마지막 Goal로 미룬다.

## Active

### G4 - 모든 구조화 도메인과 상태 머신을 완성한다

- 순서: G3 이후
- 파일: `src/main/java/com/townpet/{localguide,welfare,lostfound,marketplace,gathering,care}/`, `frontend/src/features/{localcare,lostfound,marketplace,gathering}/`, 관련 migration/test
- 변경:
  - hospital/place review, walk·guide, adoption·volunteer, breed lounge·group buy의 구조화 필드·filter·detail을 구현한다.
  - LostFound의 alert·sighting·approximate/exact location·share SVG/PNG·resolve/close/reopen lifecycle을 완성한다.
  - Marketplace SELL/RENT/SHARE와 group buy의 조건 검증, reserve/reopen/complete/cancel 전이를 완성하고 최소 금지 품목 rule을 적용한다.
  - Gathering 정원·중복 참가·참여 취소를 constraint와 conditional update로 보호한다.
  - Care는 실행 우선순위는 낮추되 전체 클론 기준에서는 Request·Application·Assignment·Feedback 전체 여정을 구현한다. 이를 생략하면 전체 클론이 아니라 Care 제외 변형으로 판정한다.
- 검증: 각 도메인의 상태 전이·권한 integration test와 LostFound/Marketplace/Gathering/Care 대표 browser journey를 Goal 종료 시 실행한다.
- 완료: Legacy가 제공하던 구조화 게시판과 도메인별 상태 의미가 generic publication으로 대체되지 않고 실제 데이터 모델·API·화면으로 재현된다.

### G5 - Discovery·Notification·Acquisition 표면을 완성한다

- 순서: G4 이후
- 파일: `src/main/java/com/townpet/discovery/`, `notification/`, `operations/AcquisitionEventController.java`, `frontend/src/{SearchPage.tsx,BestPage.tsx,NotificationPage.tsx}`, 관련 migration/test
- 변경:
  - 일반/guest search의 title·body·구조화 field·부분/오타 검색, board·town·기간·type filter를 맞춘다.
  - home/best/personalization feed의 정렬·stable cursor·viewer-safe visibility를 완성한다.
  - notification comment/reaction/report 흐름, filter·읽음·unread count·retry 의미를 맞춘다.
  - acquisition event와 search log의 privacy·중복·실패 정책을 연결한다.
  - PostgreSQL query로 먼저 구현하고 SearchDocument/FeedDocument/event registry listener는 측정된 latency·비동기 요구가 생길 때만 추가한다.
- 검증: 검색·피드·알림 API integration test와 guest/member browser journey, 필요한 경우 query plan/latency snapshot.
- 완료: 탐색·추천·알림 관련 Legacy 화면과 API가 새로고침·필터·권한·오류 상황에서도 같은 결과를 제공한다.

### G6 - Media·Trust/Safety·Admin/Operations를 완성한다 ✅ completed (2026-08-12)

- 순서: G5 이후
- 파일: `src/main/java/com/townpet/{media,trustsafety,operations}/`, `frontend/src/Admin*.tsx`, `CorrectionCreatePage.tsx`, `deploy/`, 관련 migration/test
- 변경:
  - upload client/presign 또는 local adapter, MIME·magic byte·크기·pixel·개수 검증, finalize·publication 연결·삭제·고아 정리를 완성한다.
  - report 단건/대량 접수, 중복 방지, moderator review, visibility restriction, user hide/restore/sanction, correction을 완성한다.
  - auth audit/export, moderation log, policy·breed·personalization·ops 화면, CSP/web vital, acquisition 운영 API를 연결한다.
  - demo data·고정 계정·seed flag가 실제 application 동작과 일치하고 public 환경에 ADMIN/OPERATOR·개인정보·secret이 노출되지 않게 한다.
- 검증: authorization/security integration test, admin browser journey, upload/cleanup/backup script의 dry-run 및 restore rehearsal.
- 완료: 일반 사용자의 신고부터 Moderator 처리·복구까지와 운영자 진단·repair가 권한 분리와 audit evidence를 갖고 동작한다.

### G7 - React/Vite 화면을 시각·접근성·성능 기준으로 수렴한다 ✅ completed (2026-08-12)

- 순서: G2~G6의 기능이 안정된 뒤 묶어서 수행
- 파일: `frontend/src/`, `frontend/e2e/`, `frontend/vite.config.ts`, `styles.css`, 정적 asset
- 변경:
  - Legacy의 layout, typography, color, spacing, responsive breakpoint, loading/empty/error state, metadata와 공유 화면을 page family별로 비교한다.
  - 모바일·desktop 대표 viewport에서 navigation, form, modal, list/detail, admin 화면의 키보드·screen reader·focus·contrast를 맞춘다.
  - Vite asset hash, route fallback, API proxy, bundle·image loading과 주요 Web Vital을 측정해 회귀를 막는다.
- 검증: `corepack pnpm -C frontend typecheck`, `corepack pnpm -C frontend test`, `corepack pnpm -C frontend build`, 대표 Playwright visual/browser smoke와 성능 snapshot.
- 완료: 기능이 같은 것뿐 아니라 주요 page family의 시각·반응형·접근성·직접 URL 동작이 Legacy와 비교 가능한 수준이다. 공통 skip link·focus-visible·reduced-motion, route metadata, Vite hashed asset output을 적용했고 frontend typecheck/test/build가 통과했다. Playwright shell smoke는 로컬 Playwright 브라우저 바이너리 미설치로 실행 불가했다.

### G8 - 데이터·문서·품질 gate를 갖춘 재현 가능한 릴리스를 만든다

- 순서: G1~G7 이후
- 파일: `src/main/resources/db/migration/`, `migration/`, `.github/workflows/`, `docs/parity/`, `docs/report/`, `README.md`, `deploy/`
- 변경:
  - Flyway migration, synthetic fixture, seed/reset, schema constraint와 legacy mapping을 clean database에서 재현한다. 실제 개인정보는 옮기지 않는다.
  - 대표 정상·오류·권한·동시성·보안 journey의 테스트와 parity evidence를 정리하고, 실제로 바뀐 중요한 선택만 report에 기록한다.
  - backend/frontend build, migration, browser, smoke, backup/restore를 변경 위험에 맞는 단계별 CI gate로 묶는다.
  - 실행하지 않은 성능·가용성·복구 수치를 주장하지 않고, 최종 범위와 의도적 제외를 문서화한다.
- 검증: 변경 영향에 맞는 근접 검증 후 릴리스 시 fresh `./gradlew clean check migrationTest`, frontend typecheck/test/build/e2e, `./scripts/frontend-backend-smoke.sh`, migration·restore rehearsal.
- 완료: 새 환경에서 같은 명령으로 TownPet Springboot를 재현하고, 49개 page·55개 API의 verified/excluded 판정과 대표 journey evidence를 면접·코드 리뷰에서 재현할 수 있다.

### G9 - 실제 공개 운영을 시작할 때만 배포한다

- 순서: 사용자가 VPS 배포를 시작하기로 결정한 뒤
- 파일: `deploy/compose/`, `deploy/Caddyfile`, `deploy/backup-postgres.sh`, `deploy/restore-postgres.sh`, 운영 runbook
- 변경: Hetzner·DNS·TLS·Caddy·PostgreSQL offsite backup/restore·object storage·health/metrics·demo reset·비용을 실제 환경에서 구성한다.
- 검증: clean deploy, rollback, restore drill, 공개 도메인 browser smoke와 월 비용 확인.
- 완료: 실제 URL에서 보안·복구·운영 절차를 재현할 수 있을 때만 production parity를 주장한다.

## 완료 판정

“Spring Boot + React/Vite로 클론 완료”는 G1~G8을 모두 통과한 뒤에만 주장한다. G4의 Care를 생략하거나 G1의 표면을 `deferred`로 남기면 해당 기능을 제외한 변형으로 표현해야 한다. G9를 하지 않은 상태에서는 코드·로컬/CI 수준의 재현 가능한 완성으로 표현하고, 실제 공개 서비스 운영까지 완료했다고 표현하지 않는다.

## 영구 제외

- Kakao/Naver OAuth와 social account link/unlink
- 실제 Legacy 개인정보 migration
- 결제·정산·환불·private chat
- 근거 없는 Redis·Kafka·Elasticsearch·Kubernetes·microservice 도입
