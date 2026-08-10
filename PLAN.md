# PLAN.md

## Goal

기준 commit `7d8f6d0bd22dedd82350c05142823ab2d101574d`의 TownPet 사용자 경험을 Java 25·Spring Boot 4.1·React/Vite·PostgreSQL 18로 이전한다. 현재는 P2 domain vertical slice 단계이며 완료 이력은 Git과 [`docs/report/engineering-story.md`](docs/report/engineering-story.md)에서 확인한다.

## Active

1. P2.15 - media upload asset lifecycle 경계를 추가한다
   - 파일: `src/main/java/com/townpet/media/**`, `src/main/java/com/townpet/media/api/**`, `src/test/java/com/townpet/media/**`, `src/main/resources/db/migration/**`
   - 변경: upload asset의 owner·object key·상태·만료를 publication과 분리된 media write owner로 정의하고, finalize 전 asset을 publication에 연결할 수 없도록 최소 API 계약을 추가한다.
   - 검증: `./gradlew integrationTest --tests '*Media*' --tests '*Publication*'`
   - 완료: media 상태 전이와 publication 연결 실패가 독립된 오류·transaction 계약으로 검증되고, 삭제된 publication 복구가 media 원장을 임의로 변경하지 않는다.

## Backlog

- Engagement·Relationship: follow/block와 동시성·IDOR
- Publication 복구·GuestPrincipal 관리 credential과 Media lifecycle
- LostFound: PostGIS 근사 위치, 암호화한 정확 증거, 제보·해결 lifecycle
- Marketplace와 LocalGuide·Welfare·Care·Gathering 구조화 domain parity
- TrustSafety·Discovery·Notification·Operations와 전체 ETL·배포·복구 evidence
