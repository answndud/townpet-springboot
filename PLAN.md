# PLAN.md

## Goal

기준 commit `7d8f6d0bd22dedd82350c05142823ab2d101574d`의 TownPet 사용자 경험을 Java 25·Spring Boot 4.1·React/Vite·PostgreSQL 18로 이전한다. 현재는 P2 domain vertical slice 단계이며 완료 이력은 Git과 [`docs/report/engineering-story.md`](docs/report/engineering-story.md)에서 확인한다.

## Active

1. P3-LOSTFOUND - 활성 alert 공개 목록에 PostGIS 반경 검색을 연결한다
   - 파일: `src/main/java/com/townpet/lostfound/**`, `src/test/java/com/townpet/lostfound/**`
   - 변경: 기존 kind·limit 목록에 latitude·longitude·radiusMeters 선택 조건을 추가하고 `ST_DWithin`으로 근처 활성 alert를 조회한다.
   - 검증: `./gradlew integrationTest --tests '*LostFoundAlertControllerTest*'`
   - 완료: 반경 조건은 세 좌표 파라미터가 모두 있을 때만 적용되고, 일부 누락·범위 초과 요청은 400으로 거부된다.

## Backlog

- Engagement·Relationship: follow/block와 동시성·IDOR
- Publication 복구·GuestPrincipal 관리 credential과 Media lifecycle
- LostFound: PostGIS 근사 위치, 암호화한 정확 증거, 제보·해결 lifecycle
- Marketplace와 LocalGuide·Welfare·Care·Gathering 구조화 domain parity
- TrustSafety·Discovery·Notification·Operations와 전체 ETL·배포·복구 evidence
