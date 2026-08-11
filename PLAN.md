# PLAN.md

## Goal

기준 commit `7d8f6d0bd22dedd82350c05142823ab2d101574d`의 TownPet 사용자 경험을 Java 25·Spring Boot 4.1·React/Vite·PostgreSQL 18로 이전한다. 현재는 P2 domain vertical slice 단계이며 완료 이력은 Git과 [`docs/report/engineering-story.md`](docs/report/engineering-story.md)에서 확인한다.

## Active

1. P3-LOSTFOUND - alert lifecycle 재개와 상태 이력을 연결한다
   - 파일: `src/main/java/com/townpet/lostfound/**`, `src/main/resources/db/migration/**`, `src/test/java/com/townpet/lostfound/**`
   - 변경: `RESOLVED/CLOSED → ACTIVE` 재개를 owner-only로 허용하고 reopen reason과 모든 상태 전이를 history table에 기록한다.
   - 검증: `./gradlew integrationTest --tests '*LostFoundAlertControllerTest*'`
   - 완료: 재개 사유 없는 요청은 거부되고, 타인 재개는 차단되며, 상태 전이마다 actor·이전/다음 상태·사유가 저장된다.

## Backlog

- Engagement·Relationship: follow/block와 동시성·IDOR
- Publication 복구·GuestPrincipal 관리 credential과 Media lifecycle
- LostFound: PostGIS 근사 위치, 암호화한 정확 증거, 제보·해결 lifecycle
- Marketplace와 LocalGuide·Welfare·Care·Gathering 구조화 domain parity
- TrustSafety·Discovery·Notification·Operations와 전체 ETL·배포·복구 evidence
