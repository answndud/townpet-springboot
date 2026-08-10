# PLAN.md

## Goal

기준 commit `7d8f6d0bd22dedd82350c05142823ab2d101574d`의 TownPet 사용자 경험을 Java 25·Spring Boot 4.1·React/Vite·PostgreSQL 18로 이전한다. 현재는 P2 domain vertical slice 단계이며 완료 이력은 Git과 [`docs/report/engineering-story.md`](docs/report/engineering-story.md)에서 확인한다.

## Active

1. P2.1 - OAuth provider stub과 browser auth parity를 닫는다
   - 파일: `src/main/java/com/townpet/identity/**`, `api/openapi/townpet.yaml`, `frontend/src/features/auth/**`, `frontend/e2e/auth-parity.spec.ts`
   - 변경: Kakao·Naver OAuth provider stub 계약과 delivery stub을 추가한다. Demo identity 변경 금지, member/profile IDOR와 login·logout·password reset·email verification·onboarding E2E를 연결한다.
   - 검증: `./gradlew clean check migrationTest --tests '*Identity*' --tests '*Member*' && corepack pnpm -C frontend test:e2e -- auth-parity.spec.ts`
   - 완료: credentials login·logout·reset·verification·onboarding·role·IDOR·CSRF가 JDBC session과 합성 fixture에서 통과하고 실제 signup/OAuth는 꺼져 있다.

2. P2.2 - Publication·Media 작성과 상세 여정을 연결한다
   - 파일: `src/main/java/com/townpet/publication/**`, `src/main/java/com/townpet/media/**`, `src/main/resources/db/migration/V006__*.sql`, `frontend/src/features/publication/**`
   - 변경: publication lifecycle·ownership·LOCAL/GLOBAL scope와 upload/finalize/orphan lifecycle을 한 vertical slice로 구현하고 기존 작성·상세 UI를 연결한다.
   - 검증: `./gradlew integrationTest --tests '*Publication*' --tests '*Media*' && corepack pnpm -C frontend test:e2e -- publication-parity.spec.ts upload-parity.spec.ts`
   - 완료: 회원·guest 작성, 수정·삭제, direct URL, metadata와 media cleanup이 Spring API만 사용한다.

## Backlog

- Engagement·Relationship: comment/reaction/bookmark/follow/block와 동시성·IDOR
- LostFound: PostGIS 근사 위치, 암호화한 정확 증거, 제보·해결 lifecycle
- Marketplace와 LocalGuide·Welfare·Care·Gathering 구조화 domain parity
- TrustSafety·Discovery·Notification·Operations와 durable event/rebuild/demo reset
- 전체 ETL·49 page/55 API parity·성능·Hetzner 배포·backup/restore evidence
