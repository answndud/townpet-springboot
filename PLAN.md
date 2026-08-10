# PLAN.md

## Goal

기준 commit `7d8f6d0bd22dedd82350c05142823ab2d101574d`의 TownPet 사용자 경험을 Java 25·Spring Boot 4.1·React/Vite·PostgreSQL 18로 이전한다. 현재는 P2 domain vertical slice 단계이며 완료 이력은 Git과 [`docs/report/engineering-story.md`](docs/report/engineering-story.md)에서 확인한다.

## Active

1. P2.16 - object storage adapter와 finalize 검증을 연결한다
   - 파일: `src/main/java/com/townpet/media/**`, `src/main/java/com/townpet/media/api/**`, `src/test/java/com/townpet/media/**`, `deploy/compose/**`
   - 변경: presigned upload·object metadata 조회를 storage port로 분리하고, finalize가 실제 object 존재·MIME·magic byte·checksum을 확인하도록 local test adapter를 연결한다.
   - 검증: `./gradlew integrationTest --tests '*Media*' && ./gradlew check`; storage adapter contract test에서 missing/mismatched object를 거부한다.
   - 완료: storage provider 교체 없이 media service가 검증된 object만 `READY`로 전환하고, 실패 시 metadata 상태가 `UPLOADING`에 남는다.

## Backlog

- Engagement·Relationship: follow/block와 동시성·IDOR
- Publication 복구·GuestPrincipal 관리 credential과 Media lifecycle
- LostFound: PostGIS 근사 위치, 암호화한 정확 증거, 제보·해결 lifecycle
- Marketplace와 LocalGuide·Welfare·Care·Gathering 구조화 domain parity
- TrustSafety·Discovery·Notification·Operations와 전체 ETL·배포·복구 evidence
