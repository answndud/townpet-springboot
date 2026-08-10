# PLAN.md

## Goal

기준 commit `7d8f6d0bd22dedd82350c05142823ab2d101574d`의 TownPet 사용자 경험을 Java 25·Spring Boot 4.1·React/Vite·PostgreSQL 18로 이전한다. 현재는 P2 domain vertical slice 단계이며 완료 이력은 Git과 [`docs/report/engineering-story.md`](docs/report/engineering-story.md)에서 확인한다.

## Active

1. P2.7 - relationship 경합과 조회 IDOR 회귀를 검증한다
   - 파일: `src/main/java/com/townpet/relationship/**`, `src/main/java/com/townpet/publication/**`, `src/test/java/com/townpet/relationship/**`, `src/test/java/com/townpet/publication/**`
   - 변경: follow/block 동시 요청에서 unique 제약과 상태 전이가 깨지지 않는지 검증하고, 차단·팔로우 주체/대상 UUID를 바꾼 feed·상세·relationship 요청이 다른 회원 상태를 노출하지 않는 회귀 테스트를 추가한다.
   - 검증: `./gradlew integrationTest --tests '*Relationship*' --tests '*Publication*' && corepack pnpm -C frontend test:e2e:auth -- relationship-management.spec.ts`
   - 완료: 반복·동시 mutation이 중복 원장이나 상충 상태를 만들지 않고, 모든 관계·publication 조회 결과가 authenticated principal 기준으로 격리된다.

## Backlog

- Engagement·Relationship: follow/block와 동시성·IDOR
- Publication 복구·GuestPrincipal 관리 credential과 Media lifecycle
- LostFound: PostGIS 근사 위치, 암호화한 정확 증거, 제보·해결 lifecycle
- Marketplace와 LocalGuide·Welfare·Care·Gathering 구조화 domain parity
- TrustSafety·Discovery·Notification·Operations와 전체 ETL·배포·복구 evidence
