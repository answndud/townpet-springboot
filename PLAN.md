# PLAN.md

## Goal

기준 commit `7d8f6d0bd22dedd82350c05142823ab2d101574d`의 TownPet 사용자 경험을 Java 25·Spring Boot 4.1·React/Vite·PostgreSQL 18로 이전한다. 현재는 P2 domain vertical slice 단계이며 완료 이력은 Git과 [`docs/report/engineering-story.md`](docs/report/engineering-story.md)에서 확인한다.

## Active

1. P2.3 - 회원 자유글 reaction toggle 여정을 연결한다
   - 파일: `src/main/resources/db/migration/V009__engagement_reaction.sql`, `src/main/java/com/townpet/engagement/**`, `api/openapi/townpet.yaml`, `frontend/src/features/publication/PublicationDetailPage.tsx`
   - 변경: ACTIVE publication에 회원당 하나의 reaction을 생성·해제하는 API와 database unique constraint를 추가한다. 삭제 게시글·비회원 요청을 거부하고 반복 toggle 요청이 중복 row를 만들지 않게 한다.
   - 검증: `./gradlew integrationTest --tests '*Reaction*' openApiValidate && corepack pnpm -C frontend test && corepack pnpm -C frontend test:e2e:auth -- reaction-management.spec.ts`
   - 완료: 로그인 회원은 상세에서 reaction을 켜고 끌 수 있으며 새로고침 후 상태가 유지되고, 동일 회원 중복 생성·삭제 게시글 대상 변경·비회원 변경은 거부된다.

## Backlog

- Engagement·Relationship: bookmark/follow/block와 동시성·IDOR
- Publication 복구·GuestPrincipal 관리 credential과 Media lifecycle
- LostFound: PostGIS 근사 위치, 암호화한 정확 증거, 제보·해결 lifecycle
- Marketplace와 LocalGuide·Welfare·Care·Gathering 구조화 domain parity
- TrustSafety·Discovery·Notification·Operations와 전체 ETL·배포·복구 evidence
