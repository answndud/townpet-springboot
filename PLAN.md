# PLAN.md

## Goal

기준 commit `7d8f6d0bd22dedd82350c05142823ab2d101574d`의 TownPet 사용자 경험을 Java 25·Spring Boot 4.1·React/Vite·PostgreSQL 18로 이전한다. 현재는 P2 domain vertical slice 단계이며 완료 이력은 Git과 [`docs/report/engineering-story.md`](docs/report/engineering-story.md)에서 확인한다.

## Active

1. G1-COMMUNITY - 커뮤니티 핵심 사용자 여정을 완성한다
   - 범위: discovery feed, publication 상세·작성·수정, comment/reaction/bookmark를 하나의 React 화면 흐름으로 연결한다.
   - 원칙: 기존 publication·engagement·discovery backend를 재사용하고, 필요한 계약만 추가한다. 화면만 늘리지 말고 정상·실패·권한 흐름을 함께 닫는다.
   - 완료: 비회원이 피드·상세를 보고, 로그인 사용자가 글 작성/수정과 댓글·반응·북마크를 실제로 수행하며 새로고침 후에도 상태가 유지된다.

## Backlog

2. G2-RELATIONSHIP - member profile과 follow/block 사용자 여정을 완성한다
   - profile 조회, follow/unfollow, block/unblock, 차단 시 조회·상호작용 제한, 동시성·IDOR를 한 덩어리로 닫는다.
3. G3-LOCAL-CARE - 지역 정보와 반려동물 케어 탐색을 완성한다
   - LocalGuide·Welfare·Care의 목록/검색/상세/작성 중 실제 제품 가치가 높은 흐름부터 선정해 backend와 React를 함께 구현한다.
4. G4-GATHERING - 모임 탐색과 참여 lifecycle을 완성한다
   - 목록·상세·생성·참여·취소·정원/중복 참여 규칙과 권한을 하나의 흐름으로 구현한다.
5. G5-TRUST-OPS - 신고·미디어·알림과 운영 경계를 연결한다
   - 신고/관리자 최소 흐름, media lifecycle, notification projection, GuestPrincipal·관리 credential을 실제 운영 시나리오에 맞춰 추가한다.
6. G6-MIGRATE-DEPLOY - 공개 포트폴리오 sandbox를 재현 가능하게 만든다
   - 합성 fixture/ETL, PostgreSQL·PostGIS migration, Compose/Caddy 배포, 환경변수·백업/복구·smoke evidence를 묶어 완성한다.
7. G7-PARITY-HARDEN - 전체 parity와 출시 품질을 마무리한다
   - legacy 대비 핵심 화면/상태 누락, responsive/accessibility, 성능 병목, 보안·관측성·복구 evidence를 실제 측정 결과로 정리한다.
