# PLAN.md

## Goal

기준 commit `7d8f6d0bd22dedd82350c05142823ab2d101574d`의 TownPet 사용자 경험을 Java 25·Spring Boot 4.1·React/Vite·PostgreSQL 18로 이전한다. 현재는 P2 domain vertical slice 단계이며 완료 이력은 Git과 [`docs/report/engineering-story.md`](docs/report/engineering-story.md)에서 확인한다.

## Active

1. P2.2 - 회원 자유글 댓글 작성·조회·삭제 여정을 연결한다
   - 파일: `src/main/resources/db/migration/V008__engagement_comment.sql`, `src/main/java/com/townpet/engagement/**`, `api/openapi/townpet.yaml`, `frontend/src/features/publication/PublicationDetailPage.tsx`
   - 변경: ACTIVE publication에 회원 댓글을 작성하고 stable 순서로 조회하며 작성자만 lifecycle 삭제하는 API를 추가한다. 삭제 게시글에는 새 댓글을 거부하고 댓글 작성자 ID를 request에서 받지 않는다.
   - 검증: `./gradlew integrationTest --tests '*Comment*' openApiValidate && corepack pnpm -C frontend test && corepack pnpm -C frontend test:e2e:auth -- comment-management.spec.ts`
   - 완료: 로그인 회원은 공개 가능한 글의 댓글을 작성·조회·삭제하고, 비회원 작성과 타인 삭제 및 삭제 게시글 대상 작성은 거부되며 새로고침 후에도 결과가 유지된다.

## Backlog

- Engagement·Relationship: reaction/bookmark/follow/block와 동시성·IDOR
- Publication 복구·GuestPrincipal 관리 credential과 Media lifecycle
- LostFound: PostGIS 근사 위치, 암호화한 정확 증거, 제보·해결 lifecycle
- Marketplace와 LocalGuide·Welfare·Care·Gathering 구조화 domain parity
- TrustSafety·Discovery·Notification·Operations와 전체 ETL·배포·복구 evidence
