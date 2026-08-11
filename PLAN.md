# PLAN.md

## Goal

기준 commit `7d8f6d0bd22dedd82350c05142823ab2d101574d`의 TownPet 사용자 경험을 Java 25·Spring Boot 4.1·React/Vite·PostgreSQL 18로 이전한다. 현재는 P2 domain vertical slice 단계이며 완료 이력은 Git과 [`docs/report/engineering-story.md`](docs/report/engineering-story.md)에서 확인한다.

## Active

1. G3-LOCAL-CARE - 지역 정보와 반려동물 케어 탐색을 완성한다
   - 범위: LocalGuide·Welfare·Care의 목록/검색/상세/작성 중 실제 제품 가치가 높은 흐름부터 backend와 React를 함께 구현한다.
   - 원칙: 기존 TownPet의 정보 탐색 경험을 유지하되, 우선 읽기 중심의 검색·상세 여정을 닫고 운영자가 관리할 수 있는 최소 데이터 경계를 둔다.
   - 완료: 비회원이 지역·복지·케어 정보를 검색하고 상세를 확인하며, 공개 데이터의 출처·갱신 시각·빈 상태가 명확히 표시된다.

## Backlog

완료한 Goal:

- G1 Community: publication·discovery·engagement의 피드/상세/작성/수정/댓글/반응/북마크 사용자 여정
- G2 Relationship: public member profile과 follow/block 상태 변경, 차단·동시성 backend policy

다음 Goal:

2. G4-GATHERING - 모임 탐색과 참여 lifecycle을 완성한다
   - 목록·상세·생성·참여·취소·정원/중복 참여 규칙과 권한을 하나의 흐름으로 구현한다.
3. G5-TRUST-OPS - 신고·미디어·알림과 운영 경계를 연결한다
   - 신고/관리자 최소 흐름, media lifecycle, notification projection, GuestPrincipal·관리 credential을 실제 운영 시나리오에 맞춰 추가한다.
4. G6-MIGRATE-DEPLOY - 공개 포트폴리오 sandbox를 재현 가능하게 만든다
   - 합성 fixture/ETL, PostgreSQL·PostGIS migration, Compose/Caddy 배포, 환경변수·백업/복구·smoke evidence를 묶어 완성한다.
5. G7-PARITY-HARDEN - 전체 parity와 출시 품질을 마무리한다
   - legacy 대비 핵심 화면/상태 누락, responsive/accessibility, 성능 병목, 보안·관측성·복구 evidence를 실제 측정 결과로 정리한다.
