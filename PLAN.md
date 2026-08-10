# PLAN.md

## Goal

기준 commit `7d8f6d0bd22dedd82350c05142823ab2d101574d`의 TownPet 사용자 경험을 Java 25·Spring Boot 4.1·React/Vite·PostgreSQL 18로 이전한다. 현재는 P2 domain vertical slice 단계이며 완료 이력은 Git과 [`docs/report/engineering-story.md`](docs/report/engineering-story.md)에서 확인한다.

## Active

1. P2.9 - 차단 상태 변경의 engagement race와 캐시 경계를 검증한다
   - 파일: `src/main/java/com/townpet/engagement/**`, `src/main/java/com/townpet/relationship/**`, `src/test/java/com/townpet/engagement/**`, `src/test/java/com/townpet/relationship/**`
   - 변경: block 해제·engagement mutation이 동시에 발생할 때 허용/거부 결과가 transaction 시점의 일관된 정책을 따르는지 검증하고, 차단 상태를 캐시할 경우 무효화 없이 stale engagement가 노출되지 않는 회귀 테스트를 추가한다.
   - 검증: `./gradlew integrationTest --tests '*Engagement*' --tests '*Relationship*' && corepack pnpm -C frontend test:e2e:auth -- comment-management.spec.ts reaction-management.spec.ts bookmark-management.spec.ts`
   - 완료: block 전환 경합에서 차단된 engagement가 새로 생성되지 않고, 해제 후에는 정상 접근되며, stale cache나 principal 혼동으로 다른 회원 상태가 노출되지 않는다.

## Backlog

- Engagement·Relationship: follow/block와 동시성·IDOR
- Publication 복구·GuestPrincipal 관리 credential과 Media lifecycle
- LostFound: PostGIS 근사 위치, 암호화한 정확 증거, 제보·해결 lifecycle
- Marketplace와 LocalGuide·Welfare·Care·Gathering 구조화 domain parity
- TrustSafety·Discovery·Notification·Operations와 전체 ETL·배포·복구 evidence
