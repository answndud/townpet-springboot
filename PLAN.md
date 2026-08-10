# PLAN.md

## Goal

기준 commit `7d8f6d0bd22dedd82350c05142823ab2d101574d`의 TownPet 사용자 경험을 Java 25·Spring Boot 4.1·React/Vite·PostgreSQL 18로 이전한다. 현재는 P2 domain vertical slice 단계이며 완료 이력은 Git과 [`docs/report/engineering-story.md`](docs/report/engineering-story.md)에서 확인한다.

## Active

1. P2.4 - 회원 자유글 bookmark toggle 여정을 연결한다
   - 파일: `src/main/resources/db/migration/V010__engagement_bookmark.sql`, `src/main/java/com/townpet/engagement/**`, `api/openapi/townpet.yaml`, `frontend/src/features/publication/PublicationDetailPage.tsx`
   - 변경: ACTIVE publication에 회원당 하나의 bookmark를 저장·해제하는 API와 database unique constraint를 추가한다. 삭제 게시글·비회원 요청을 거부하고 내 저장 목록의 최소 조회 상태를 제공한다.
   - 검증: `./gradlew integrationTest --tests '*Bookmark*' openApiValidate && corepack pnpm -C frontend test && corepack pnpm -C frontend test:e2e:auth -- bookmark-management.spec.ts`
   - 완료: 로그인 회원은 상세에서 bookmark를 켜고 끌 수 있으며 새로고침 후 상태가 유지되고, 동일 회원 중복 생성·삭제 게시글 대상 변경·비회원 변경은 거부된다.

## Backlog

- Engagement·Relationship: follow/block와 동시성·IDOR
- Publication 복구·GuestPrincipal 관리 credential과 Media lifecycle
- LostFound: PostGIS 근사 위치, 암호화한 정확 증거, 제보·해결 lifecycle
- Marketplace와 LocalGuide·Welfare·Care·Gathering 구조화 domain parity
- TrustSafety·Discovery·Notification·Operations와 전체 ETL·배포·복구 evidence
