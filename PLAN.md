# PLAN.md

## Goal

기준 commit `7d8f6d0bd22dedd82350c05142823ab2d101574d`의 TownPet 사용자 경험을 Java 25·Spring Boot 4.1·React/Vite·PostgreSQL 18로 이전한다. 현재는 P2 domain vertical slice 단계이며 완료 이력은 Git과 [`docs/report/engineering-story.md`](docs/report/engineering-story.md)에서 확인한다.

## Active

1. P2.5 - 회원 자유글 follow/block 정책의 첫 경계를 연결한다
   - 파일: `src/main/resources/db/migration/V011__relationship_follow_block.sql`, `src/main/java/com/townpet/relationship/**`, `api/openapi/townpet.yaml`, `frontend/src/features/publication/PublicationDetailPage.tsx`
   - 변경: 회원 간 follow와 block 원장을 분리하고 자기 자신 대상·중복 요청·비회원 변경을 거부한다. 상세 화면에서는 작성자에 대한 follow/block 상태만 제공하고 publication 조회 정책과 연결할 최소 공개 API를 만든다.
   - 검증: `./gradlew integrationTest --tests '*Relationship*' openApiValidate && corepack pnpm -C frontend test && corepack pnpm -C frontend test:e2e:auth -- relationship-management.spec.ts`
   - 완료: 로그인 회원은 다른 회원을 follow/block할 수 있고 새로고침 후 상태가 유지되며, 자기 자신·중복·비회원·삭제 대상 요청이 명확한 오류로 거부된다.

## Backlog

- Engagement·Relationship: follow/block와 동시성·IDOR
- Publication 복구·GuestPrincipal 관리 credential과 Media lifecycle
- LostFound: PostGIS 근사 위치, 암호화한 정확 증거, 제보·해결 lifecycle
- Marketplace와 LocalGuide·Welfare·Care·Gathering 구조화 domain parity
- TrustSafety·Discovery·Notification·Operations와 전체 ETL·배포·복구 evidence
