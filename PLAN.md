# PLAN.md

## Goal

기준 commit `7d8f6d0bd22dedd82350c05142823ab2d101574d`의 TownPet 사용자 경험을 Java 25·Spring Boot 4.1·React/Vite·PostgreSQL 18로 이전한다. 현재는 P2 domain vertical slice 단계이며 완료 이력은 Git과 [`docs/report/engineering-story.md`](docs/report/engineering-story.md)에서 확인한다.

## Active

1. P2.17 - media object 검증과 orphan cleanup을 운영 흐름에 연결한다
   - 파일: `src/main/java/com/townpet/media/**`, `src/test/java/com/townpet/media/**`, `src/main/resources/db/migration/**`, `deploy/compose/**`
   - 변경: magic byte·MIME allowlist와 만료 `UPLOADING` asset cleanup command를 추가하고, object storage metadata와 `upload_asset` 대사를 반복 실행할 수 있게 한다.
   - 검증: `./gradlew integrationTest --tests '*Media*' && ./gradlew check`; missing/mismatched object와 cleanup 재실행 contract test를 통과한다.
   - 완료: 허용되지 않은 파일 형식과 고아 object가 `READY`/`ATTACHED`로 승격되지 않고, cleanup이 idempotent하게 metadata와 object를 함께 정리한다.

## Backlog

- Engagement·Relationship: follow/block와 동시성·IDOR
- Publication 복구·GuestPrincipal 관리 credential과 Media lifecycle
- LostFound: PostGIS 근사 위치, 암호화한 정확 증거, 제보·해결 lifecycle
- Marketplace와 LocalGuide·Welfare·Care·Gathering 구조화 domain parity
- TrustSafety·Discovery·Notification·Operations와 전체 ETL·배포·복구 evidence
