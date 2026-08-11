# PLAN.md

## Goal

기준 commit `7d8f6d0bd22dedd82350c05142823ab2d101574d`의 TownPet 사용자 경험을 Java 25·Spring Boot 4.1·React/Vite·PostgreSQL 18로 이전한다. 현재는 P2 domain vertical slice 단계이며 완료 이력은 Git과 [`docs/report/engineering-story.md`](docs/report/engineering-story.md)에서 확인한다.

## Active

1. P4-MARKETPLACE - listing 수정 가능 상태와 거래 조건 불변식을 연결한다
   - 파일: `src/main/java/com/townpet/marketplace/**`, `src/test/java/com/townpet/marketplace/**`
   - 변경: owner가 `AVAILABLE` listing만 version과 함께 제목·설명·가격을 수정하게 하고 `RESERVED` 이후 수정은 충돌로 거부한다.
   - 검증: `./gradlew integrationTest --tests '*MarketplaceListingControllerTest*'`
   - 완료: 타인·stale version·예약 이후 수정은 차단되고, 허용된 수정은 새 version으로 반환된다.

## Backlog

- Engagement·Relationship: follow/block와 동시성·IDOR
- Publication 복구·GuestPrincipal 관리 credential과 Media lifecycle
- LostFound: PostGIS 근사 위치, 암호화한 정확 증거, 제보·해결 lifecycle
- Marketplace와 LocalGuide·Welfare·Care·Gathering 구조화 domain parity
- TrustSafety·Discovery·Notification·Operations와 전체 ETL·배포·복구 evidence
