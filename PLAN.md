# PLAN.md

## Goal

기준 commit `7d8f6d0bd22dedd82350c05142823ab2d101574d`의 TownPet 사용자 경험을 Java 25·Spring Boot 4.1·React/Vite·PostgreSQL 18로 이전한다. 현재는 P2 domain vertical slice 단계이며 완료 이력은 Git과 [`docs/report/engineering-story.md`](docs/report/engineering-story.md)에서 확인한다.

## Active

1. P2.1 - 공개·회원 Publication 피드를 연결한다
   - 파일: `src/main/java/com/townpet/publication/**`, `api/openapi/townpet.yaml`, `frontend/src/features/publication/**`, `frontend/e2e/feed-parity.spec.ts`
   - 변경: `ACTIVE` 전체 글과 로그인 회원 대표 동네 글을 stable cursor로 조회하고 기존 TownPet 카드 밀도의 `/feed`, `/feed/guest` 화면에 연결한다.
   - 검증: `./gradlew integrationTest --tests '*Publication*' && corepack pnpm -C frontend test && corepack pnpm -C frontend test:e2e:auth -- feed-parity.spec.ts`
   - 완료: 방문자는 전체 글, 회원은 전체·자기 동네 글을 desktop·mobile 피드에서 보고 다음 cursor로 중복 없이 이동한다.

## Backlog

- Engagement·Relationship: comment/reaction/bookmark/follow/block와 동시성·IDOR
- Publication 작성자 수정·삭제·복구와 GuestPrincipal 관리 credential
- Media presign·finalize·attach·orphan cleanup과 Publication image 연결
- LostFound: PostGIS 근사 위치, 암호화한 정확 증거, 제보·해결 lifecycle
- Marketplace와 LocalGuide·Welfare·Care·Gathering 구조화 domain parity
- TrustSafety·Discovery·Notification·Operations와 전체 ETL·배포·복구 evidence
