# PLAN.md

## Goal

기준 commit `7d8f6d0bd22dedd82350c05142823ab2d101574d`의 TownPet 사용자 경험을 Java 25·Spring Boot 4.1·React/Vite·PostgreSQL 18로 이전한다. 현재는 P2 domain vertical slice 단계이며 완료 이력은 Git과 [`docs/report/engineering-story.md`](docs/report/engineering-story.md)에서 확인한다.

## Active

1. P4-MARKETPLACE - classified listing 상태 lifecycle과 소유권을 연결한다
   - 파일: `src/main/java/com/townpet/marketplace/**`, `src/main/resources/db/migration/**`, `src/test/java/com/townpet/marketplace/**`
   - 변경: `AVAILABLE/RESERVED`에서 허용된 `RESERVED/AVAILABLE/COMPLETED/CANCELLED` 전이를 version·owner와 함께 처리하고 status history를 저장한다.
   - 검증: `./gradlew integrationTest --tests '*MarketplaceListingControllerTest*'`
   - 완료: 금지된 전이·stale version·타인 변경은 거부되고, `COMPLETED/CANCELLED` listing은 공개 상세에서 404가 된다.

## Backlog

- Engagement·Relationship: follow/block와 동시성·IDOR
- Publication 복구·GuestPrincipal 관리 credential과 Media lifecycle
- LostFound: PostGIS 근사 위치, 암호화한 정확 증거, 제보·해결 lifecycle
- Marketplace와 LocalGuide·Welfare·Care·Gathering 구조화 domain parity
- TrustSafety·Discovery·Notification·Operations와 전체 ETL·배포·복구 evidence
