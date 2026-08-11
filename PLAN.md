# PLAN.md

## Goal

기준 commit `7d8f6d0bd22dedd82350c05142823ab2d101574d`의 TownPet 사용자 경험을 Java 25·Spring Boot 4.1·React/Vite·PostgreSQL 18로 이전한다. 현재는 P2 domain vertical slice 단계이며 완료 이력은 Git과 [`docs/report/engineering-story.md`](docs/report/engineering-story.md)에서 확인한다.

## Active

1. P3-LOSTFOUND - alert 상세에 목격 제보 목록을 연결한다
   - 파일: `src/main/java/com/townpet/lostfound/**`, `src/test/java/com/townpet/lostfound/**`
   - 변경: alert별 sighting을 목격 시각순으로 공개 조회하고 limit을 적용한다. 응답은 근사 위치만 포함한다.
   - 검증: `./gradlew integrationTest --tests '*LostFoundSightingControllerTest*'`
   - 완료: 존재하지 않는 alert는 404이고, 제보 목록은 안정적인 `seen_at·id` 순서로 반환되며 exact 위치는 노출되지 않는다.

## Backlog

- Engagement·Relationship: follow/block와 동시성·IDOR
- Publication 복구·GuestPrincipal 관리 credential과 Media lifecycle
- LostFound: PostGIS 근사 위치, 암호화한 정확 증거, 제보·해결 lifecycle
- Marketplace와 LocalGuide·Welfare·Care·Gathering 구조화 domain parity
- TrustSafety·Discovery·Notification·Operations와 전체 ETL·배포·복구 evidence
