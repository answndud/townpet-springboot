# PLAN.md

## Goal

기준 commit `7d8f6d0bd22dedd82350c05142823ab2d101574d`의 TownPet 사용자 경험을 Java 25·Spring Boot 4.1·React/Vite·PostgreSQL 18로 이전한다. 현재는 P2 domain vertical slice 단계이며 완료 이력은 Git과 [`docs/report/engineering-story.md`](docs/report/engineering-story.md)에서 확인한다.

## Active

1. P2.8 - 댓글·reaction·bookmark에도 block 정책을 확장한다
   - 파일: `src/main/java/com/townpet/engagement/**`, `src/main/java/com/townpet/relationship/api/**`, `api/openapi/townpet.yaml`, `src/test/java/com/townpet/engagement/**`
   - 변경: 차단 작성자의 publication에 대한 댓글 작성·reaction·bookmark 변경을 일관되게 거부하고, 기존 원장 상태 조회도 viewer policy와 맞춘다. engagement가 relationship 내부 구현 대신 공개 block API만 사용하도록 유지한다.
   - 검증: `./gradlew integrationTest --tests '*Engagement*' --tests '*Relationship*' openApiValidate && corepack pnpm -C frontend test:e2e:auth -- comment-management.spec.ts reaction-management.spec.ts bookmark-management.spec.ts`
   - 완료: 차단 전후 모든 engagement mutation과 상태 조회가 같은 authorization policy를 적용하고, 비회원·다른 회원·GLOBAL 공개 경로에는 기존 의미가 보존된다.

## Backlog

- Engagement·Relationship: follow/block와 동시성·IDOR
- Publication 복구·GuestPrincipal 관리 credential과 Media lifecycle
- LostFound: PostGIS 근사 위치, 암호화한 정확 증거, 제보·해결 lifecycle
- Marketplace와 LocalGuide·Welfare·Care·Gathering 구조화 domain parity
- TrustSafety·Discovery·Notification·Operations와 전체 ETL·배포·복구 evidence
