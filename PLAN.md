# PLAN.md

## Goal

기준 commit `7d8f6d0bd22dedd82350c05142823ab2d101574d`의 TownPet 사용자 경험을 Java 25·Spring Boot 4.1·React/Vite·PostgreSQL 18로 이전한다. 현재는 P2 domain vertical slice 단계이며 완료 이력은 Git과 [`docs/report/engineering-story.md`](docs/report/engineering-story.md)에서 확인한다.

## Active

1. P5-LOSTFOUND - LostFound backend hot path를 React 화면과 연결한다
   - 파일: `frontend/src/features/lostfound/**`, `frontend/src/api/client.ts`, `frontend/src/App.tsx`, `frontend/src/styles.css`
   - 변경: alert 목록·반경 filter·상세·sighting 목록·작성·owner-only exact location·lifecycle action을 모바일 화면으로 연결한다.
   - 검증: frontend typecheck/build와 LostFound 관련 Vitest·integration test
   - 완료: 공개 사용자는 목록·상세·근사 정보·sighting을 보고, 로그인 사용자는 alert/sighting 작성과 자신의 lifecycle/exact location action을 수행한다.

## Backlog

- Engagement·Relationship: follow/block와 동시성·IDOR
- Publication 복구·GuestPrincipal 관리 credential과 Media lifecycle
- LostFound: PostGIS 근사 위치, 암호화한 정확 증거, 제보·해결 lifecycle
- Marketplace와 LocalGuide·Welfare·Care·Gathering 구조화 domain parity
- TrustSafety·Discovery·Notification·Operations와 전체 ETL·배포·복구 evidence
