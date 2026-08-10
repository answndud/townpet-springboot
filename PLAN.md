# PLAN.md

## Goal

기준 commit `7d8f6d0bd22dedd82350c05142823ab2d101574d`의 TownPet 사용자 경험을 Java 25·Spring Boot 4.1·React/Vite·PostgreSQL 18로 이전한다. 현재는 P2 domain vertical slice 단계이며 완료 이력은 Git과 [`docs/report/engineering-story.md`](docs/report/engineering-story.md)에서 확인한다.

## Active

1. P2.14 - publication 복구와 media lifecycle을 분리한다
   - 파일: `src/main/java/com/townpet/publication/**`, `src/main/java/com/townpet/media/**`, `src/test/java/com/townpet/publication/**`, `docs/report/technical-notes.md`
   - 변경: 삭제된 publication의 복구 가능 상태와 media 참조 정리 정책을 먼저 테스트 가능한 application boundary로 나눈다.
   - 검증: `./gradlew integrationTest --tests '*Publication*' --tests '*Media*'`
   - 완료: publication lifecycle과 media lifecycle의 소유권·실패 응답·복구 전이가 각각 독립된 테스트 계약으로 드러난다.

## Backlog

- Engagement·Relationship: follow/block와 동시성·IDOR
- Publication 복구·GuestPrincipal 관리 credential과 Media lifecycle
- LostFound: PostGIS 근사 위치, 암호화한 정확 증거, 제보·해결 lifecycle
- Marketplace와 LocalGuide·Welfare·Care·Gathering 구조화 domain parity
- TrustSafety·Discovery·Notification·Operations와 전체 ETL·배포·복구 evidence
