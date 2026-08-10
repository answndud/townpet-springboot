# PLAN.md

## Goal

기준 commit `7d8f6d0bd22dedd82350c05142823ab2d101574d`의 TownPet 사용자 경험을 Java 25·Spring Boot 4.1·React/Vite·PostgreSQL 18로 이전한다. 현재는 P2 domain vertical slice 단계이며 완료 이력은 Git과 [`docs/report/engineering-story.md`](docs/report/engineering-story.md)에서 확인한다.

## Active

1. P2.10 - block 해제 후 engagement 복구 흐름을 검증한다
   - 파일: `src/main/java/com/townpet/engagement/**`, `src/main/java/com/townpet/relationship/**`, `src/test/java/com/townpet/engagement/**`, `src/test/java/com/townpet/relationship/**`
   - 변경: block 해제 후 새 댓글·reaction·bookmark가 정상 생성되는지, 차단 중 기존 원장과 해제 후 새 원장의 상태가 viewer별로 일관되는지 회귀 테스트를 추가한다.
   - 검증: `./gradlew integrationTest --tests '*Engagement*' --tests '*Relationship*' && corepack pnpm -C frontend test:e2e:auth -- comment-management.spec.ts reaction-management.spec.ts bookmark-management.spec.ts`
   - 완료: block 해제 전에는 engagement가 거부되고 해제 후에는 정상 허용되며, 기존 차단 회원·비회원·작성자 principal의 상태가 서로 섞이지 않는다.

## Backlog

- Engagement·Relationship: follow/block와 동시성·IDOR
- Publication 복구·GuestPrincipal 관리 credential과 Media lifecycle
- LostFound: PostGIS 근사 위치, 암호화한 정확 증거, 제보·해결 lifecycle
- Marketplace와 LocalGuide·Welfare·Care·Gathering 구조화 domain parity
- TrustSafety·Discovery·Notification·Operations와 전체 ETL·배포·복구 evidence
