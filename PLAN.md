# PLAN.md

## Goal

기준 commit `7d8f6d0bd22dedd82350c05142823ab2d101574d`의 TownPet 사용자 경험을 Java 25·Spring Boot 4.1·React/Vite·PostgreSQL 18로 이전한다. 현재는 P2 domain vertical slice 단계이며 완료 이력은 Git과 [`docs/report/engineering-story.md`](docs/report/engineering-story.md)에서 확인한다.

## Active

1. P2.11 - engagement 상태와 원장 row의 일관성을 검증한다
   - 파일: `src/main/java/com/townpet/engagement/**`, `src/test/java/com/townpet/engagement/**`, `frontend/e2e/**`
   - 변경: 댓글·reaction·bookmark의 생성·비활성화·삭제 전이가 source row와 목록/상태 요약에 같은 의미로 반영되는 회귀 테스트를 보강한다.
   - 검증: `./gradlew integrationTest --tests '*Engagement*' && corepack pnpm -C frontend test:e2e:auth -- comment-management.spec.ts reaction-management.spec.ts bookmark-management.spec.ts`
   - 완료: 각 engagement API의 응답 상태, 원장 row 개수, viewer별 active/count 값이 전이마다 일치하고 새로고침 후에도 유지된다.

## Backlog

- Engagement·Relationship: follow/block와 동시성·IDOR
- Publication 복구·GuestPrincipal 관리 credential과 Media lifecycle
- LostFound: PostGIS 근사 위치, 암호화한 정확 증거, 제보·해결 lifecycle
- Marketplace와 LocalGuide·Welfare·Care·Gathering 구조화 domain parity
- TrustSafety·Discovery·Notification·Operations와 전체 ETL·배포·복구 evidence
