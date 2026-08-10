# PLAN.md

## Goal

기준 commit `7d8f6d0bd22dedd82350c05142823ab2d101574d`의 TownPet 사용자 경험을 Java 25·Spring Boot 4.1·React/Vite·PostgreSQL 18로 이전한다. 현재는 P2 domain vertical slice 단계이며 완료 이력은 Git과 [`docs/report/engineering-story.md`](docs/report/engineering-story.md)에서 확인한다.

## Active

1. P2.13 - relationship 동시성·IDOR 경계를 확장한다
   - 파일: `src/main/java/com/townpet/relationship/**`, `src/test/java/com/townpet/relationship/**`, `frontend/e2e/relationship-management.spec.ts`
   - 변경: follow/block 전환을 동시에 반복해도 원장 unique 상태가 유지되고, authenticated principal과 대상 author가 바뀐 요청이 다른 회원의 관계 상태를 읽거나 변경하지 못하도록 회귀 테스트를 추가한다.
   - 검증: `./gradlew integrationTest --tests '*Relationship*' && TOWNPET_PNPM_BIN=/path/to/pnpm ./scripts/auth-browser-e2e.sh -- relationship-management.spec.ts`
   - 완료: 병렬 relationship mutation이 중복 row를 만들지 않고, 자기 상태·대상 상태·직접 URL 상태가 principal별로 격리된다.

## Backlog

- Engagement·Relationship: follow/block와 동시성·IDOR
- Publication 복구·GuestPrincipal 관리 credential과 Media lifecycle
- LostFound: PostGIS 근사 위치, 암호화한 정확 증거, 제보·해결 lifecycle
- Marketplace와 LocalGuide·Welfare·Care·Gathering 구조화 domain parity
- TrustSafety·Discovery·Notification·Operations와 전체 ETL·배포·복구 evidence
