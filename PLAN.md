# PLAN.md

## Goal

기준 commit `7d8f6d0bd22dedd82350c05142823ab2d101574d`의 TownPet 사용자 경험을 Java 25·Spring Boot 4.1·React/Vite·PostgreSQL 18로 이전한다. 현재는 P2 domain vertical slice 단계이며 완료 이력은 Git과 [`docs/report/engineering-story.md`](docs/report/engineering-story.md)에서 확인한다.

## Active

1. P2.6 - block 정책을 publication 조회 경계에 연결한다
   - 파일: `src/main/java/com/townpet/relationship/**`, `src/main/java/com/townpet/publication/**`, `api/openapi/townpet.yaml`, `frontend/src/features/publication/PublicationFeedPage.tsx`
   - 변경: 현재 회원이 차단한 작성자의 자유글을 `VIEWER` feed와 상세에서 제외하고 `GLOBAL` guest/public feed에는 기존 공개 정책을 유지한다. feed read path가 relationship 모듈 내부 entity를 직접 읽지 않도록 공개 조회 API를 추가한다.
   - 검증: `./gradlew integrationTest --tests '*Relationship*' --tests '*Publication*' openApiValidate && corepack pnpm -C frontend test && corepack pnpm -C frontend test:e2e:auth -- relationship-management.spec.ts publication-management.spec.ts`
   - 완료: 차단 전후 회원 feed·상세 결과가 일관되고, 비회원과 다른 회원의 공개 feed는 오염되지 않으며, 차단 관계를 우회한 IDOR 조회가 허용되지 않는다.

## Backlog

- Engagement·Relationship: follow/block와 동시성·IDOR
- Publication 복구·GuestPrincipal 관리 credential과 Media lifecycle
- LostFound: PostGIS 근사 위치, 암호화한 정확 증거, 제보·해결 lifecycle
- Marketplace와 LocalGuide·Welfare·Care·Gathering 구조화 domain parity
- TrustSafety·Discovery·Notification·Operations와 전체 ETL·배포·복구 evidence
