# PLAN.md

## Goal

기준 commit `7d8f6d0bd22dedd82350c05142823ab2d101574d`의 TownPet 사용자 경험을 Java 25·Spring Boot 4.1·React/Vite·PostgreSQL 18로 이전한다. 현재는 P2 domain vertical slice 단계이며 완료 이력은 Git과 [`docs/report/engineering-story.md`](docs/report/engineering-story.md)에서 확인한다.

## Active

1. P3-LOSTFOUND - 활성 alert 공개 목록과 kind filter를 연결한다
   - 파일: `src/main/java/com/townpet/lostfound/**`, `src/test/java/com/townpet/lostfound/**`
   - 변경: 종료되지 않은 alert를 최근 목격 시각순으로 조회하고 `LOST/FOUND` 선택 filter와 limit을 제공한다. 공개 응답은 기존처럼 근사 위치만 포함한다.
   - 검증: `./gradlew integrationTest --tests '*LostFoundAlertControllerTest*'`
   - 완료: `ACTIVE` alert만 목록에 나오고 kind·limit이 적용되며, 종료된 alert와 정확 위치는 공개 목록에서 제외된다.

## Backlog

- Engagement·Relationship: follow/block와 동시성·IDOR
- Publication 복구·GuestPrincipal 관리 credential과 Media lifecycle
- LostFound: PostGIS 근사 위치, 암호화한 정확 증거, 제보·해결 lifecycle
- Marketplace와 LocalGuide·Welfare·Care·Gathering 구조화 domain parity
- TrustSafety·Discovery·Notification·Operations와 전체 ETL·배포·복구 evidence
