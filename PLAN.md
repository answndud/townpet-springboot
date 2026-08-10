# PLAN.md

## Goal

기준 commit `7d8f6d0bd22dedd82350c05142823ab2d101574d`의 TownPet 사용자 경험을 Java 25·Spring Boot 4.1·React/Vite·PostgreSQL 18로 이전한다. 현재는 P2 domain vertical slice 단계이며 완료 이력은 Git과 [`docs/report/engineering-story.md`](docs/report/engineering-story.md)에서 확인한다.

## Active

1. P2.12 - engagement read model과 삭제 lifecycle 경계를 검증한다
   - 파일: `src/main/java/com/townpet/engagement/**`, `src/test/java/com/townpet/engagement/**`, `frontend/e2e/**`
   - 변경: 댓글·reaction·bookmark 조회가 삭제·비활성화된 source row를 다시 노출하지 않고, 새로고침 후에도 viewer별 active/count 요약을 유지하는 회귀 테스트를 보강한다.
   - 검증: `./gradlew integrationTest --tests '*Engagement*' && corepack pnpm -C frontend test:e2e:auth -- comment-management.spec.ts reaction-management.spec.ts bookmark-management.spec.ts`
   - 완료: 댓글 목록과 세 engagement 상태 요약이 source row lifecycle/활성 상태와 일치하며, 전이 후 직접 조회와 브라우저 새로고침 결과가 동일하다.

## Backlog

- Engagement·Relationship: follow/block와 동시성·IDOR
- Publication 복구·GuestPrincipal 관리 credential과 Media lifecycle
- LostFound: PostGIS 근사 위치, 암호화한 정확 증거, 제보·해결 lifecycle
- Marketplace와 LocalGuide·Welfare·Care·Gathering 구조화 domain parity
- TrustSafety·Discovery·Notification·Operations와 전체 ETL·배포·복구 evidence
