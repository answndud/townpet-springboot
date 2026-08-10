# PLAN.md

## Goal

기준 commit `7d8f6d0bd22dedd82350c05142823ab2d101574d`의 TownPet 사용자 경험을 Java 25·Spring Boot 4.1·React/Vite·PostgreSQL 18로 이전한다. 현재는 P2 domain vertical slice 단계이며 완료 이력은 Git과 [`docs/report/engineering-story.md`](docs/report/engineering-story.md)에서 확인한다.

## Active

1. P2.1 - React 작성·상세 화면을 Spring API에 연결한다
   - 파일: `frontend/src/features/publication/**`, `frontend/src/App.tsx`, `frontend/e2e/publication-parity.spec.ts`
   - 변경: 기존 TownPet의 글 작성·상세 구조와 responsive 감각을 유지하면서 FREE_BOARD 회원 여정을 공통 API client에 연결한다.
   - 검증: `corepack pnpm -C frontend typecheck && corepack pnpm -C frontend test && corepack pnpm -C frontend test:e2e:auth -- publication-parity.spec.ts`
   - 완료: demo 회원이 desktop·mobile에서 글을 작성하고 생성된 direct URL을 새로 열어 같은 내용을 확인한다.

## Backlog

- Engagement·Relationship: comment/reaction/bookmark/follow/block와 동시성·IDOR
- Publication 작성자 수정·삭제·복구와 GuestPrincipal 관리 credential
- Media presign·finalize·attach·orphan cleanup과 Publication image 연결
- LostFound: PostGIS 근사 위치, 암호화한 정확 증거, 제보·해결 lifecycle
- Marketplace와 LocalGuide·Welfare·Care·Gathering 구조화 domain parity
- TrustSafety·Discovery·Notification·Operations와 전체 ETL·배포·복구 evidence
