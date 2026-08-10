# PLAN.md

## Goal

기준 commit `7d8f6d0bd22dedd82350c05142823ab2d101574d`의 TownPet 사용자 경험을 Java 25·Spring Boot 4.1·React/Vite·PostgreSQL 18로 이전한다. 현재는 P2 domain vertical slice 단계이며 완료 이력은 Git과 [`docs/report/engineering-story.md`](docs/report/engineering-story.md)에서 확인한다.

## Active

1. P2.1 - 회원 자유글 수정·삭제 여정을 연결한다
   - 파일: `src/main/java/com/townpet/publication/**`, `api/openapi/townpet.yaml`, `frontend/src/features/publication/**`, `frontend/e2e/publication-management.spec.ts`
   - 변경: authenticated author ownership과 optimistic version을 검사하는 제목·본문·범위 수정 및 lifecycle 삭제 API를 추가하고 상세·수정 화면에 연결한다.
   - 검증: `./gradlew integrationTest --tests '*Publication*' && corepack pnpm -C frontend test && corepack pnpm -C frontend test:e2e:auth -- publication-management.spec.ts`
   - 완료: 작성자는 자기 글을 수정·삭제하고, 다른 회원과 stale version 요청은 거부되며 삭제 글은 상세·피드에서 보이지 않는다.

## Backlog

- Engagement·Relationship: comment/reaction/bookmark/follow/block와 동시성·IDOR
- Publication 복구·GuestPrincipal 관리 credential과 Media lifecycle
- LostFound: PostGIS 근사 위치, 암호화한 정확 증거, 제보·해결 lifecycle
- Marketplace와 LocalGuide·Welfare·Care·Gathering 구조화 domain parity
- TrustSafety·Discovery·Notification·Operations와 전체 ETL·배포·복구 evidence
