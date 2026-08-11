# PLAN.md

## Goal

기준 commit `7d8f6d0bd22dedd82350c05142823ab2d101574d`의 TownPet 사용자 경험을 Java 25·Spring Boot 4.1·React/Vite·PostgreSQL 18로 이전한다. 현재는 P2 domain vertical slice 단계이며 완료 이력은 Git과 [`docs/report/engineering-story.md`](docs/report/engineering-story.md)에서 확인한다.

## Active

1. P3-LOSTFOUND - 정확 위치 evidence를 owner-only 조회와 audit로 보호한다
   - 파일: `src/main/java/com/townpet/lostfound/**`, `src/main/resources/db/migration/**`, `src/test/java/com/townpet/lostfound/**`
   - 변경: sighting에 선택적 정확 위치와 공개 범위를 저장하고, alert 작성자 전용 exact-location endpoint 및 접근 audit row를 연결한다. 공개 조회 응답에는 exact 좌표를 절대 포함하지 않는다.
   - 검증: `./gradlew integrationTest --tests '*LostFoundExactLocationControllerTest*'`
   - 완료: 타인·비회원의 정확 위치 조회는 거부되고, 허용된 조회만 audit에 기록되며, exact 좌표 없는 제보도 기존 공개 흐름을 유지한다.

## Backlog

- Engagement·Relationship: follow/block와 동시성·IDOR
- Publication 복구·GuestPrincipal 관리 credential과 Media lifecycle
- LostFound: PostGIS 근사 위치, 암호화한 정확 증거, 제보·해결 lifecycle
- Marketplace와 LocalGuide·Welfare·Care·Gathering 구조화 domain parity
- TrustSafety·Discovery·Notification·Operations와 전체 ETL·배포·복구 evidence
