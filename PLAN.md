# PLAN.md

## Goal

기준 commit `7d8f6d0bd22dedd82350c05142823ab2d101574d`의 TownPet 사용자 경험을 Java 25·Spring Boot 4.1·React/Vite·PostgreSQL 18로 이전한다. 현재는 P2 domain vertical slice 단계이며 완료 이력은 Git과 [`docs/report/engineering-story.md`](docs/report/engineering-story.md)에서 확인한다.

## Active

1. P3-LOSTFOUND - 분실·발견 alert의 작성·공개 조회·소유자 lifecycle을 안전한 vertical slice로 연결한다
   - 파일: `src/main/java/com/townpet/lostfound/**`, `src/main/resources/db/migration/**`, `src/test/java/com/townpet/lostfound/**`
   - 변경: `LOST/FOUND` alert를 PostgreSQL/PostGIS에 저장하고 공개 근사 위치 조회와 작성자 전용 `RESOLVED/CLOSED` 전이를 연결한다. 정확 위치 evidence와 sighting은 다음 slice로 미룬다.
   - 검증: `./gradlew integrationTest --tests '*LostFoundAlertControllerTest*'`
   - 완료: 유효하지 않은 위치·상태는 거부되고, 공개 응답에는 근사 위치만 포함되며, 타인 alert lifecycle 변경은 차단된다.

## Backlog

- Engagement·Relationship: follow/block와 동시성·IDOR
- Publication 복구·GuestPrincipal 관리 credential과 Media lifecycle
- LostFound: PostGIS 근사 위치, 암호화한 정확 증거, 제보·해결 lifecycle
- Marketplace와 LocalGuide·Welfare·Care·Gathering 구조화 domain parity
- TrustSafety·Discovery·Notification·Operations와 전체 ETL·배포·복구 evidence
