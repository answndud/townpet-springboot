# PLAN.md

## Goal

기준 commit `7d8f6d0bd22dedd82350c05142823ab2d101574d`의 TownPet 사용자 경험을 Java 25·Spring Boot 4.1·React/Vite·PostgreSQL 18로 이전한다. 현재는 P2 domain vertical slice 단계이며 완료 이력은 Git과 [`docs/report/engineering-story.md`](docs/report/engineering-story.md)에서 확인한다.

## Active

1. G7-PARITY-HARDEN - 전체 parity와 출시 품질을 마무리한다
   - 범위: 구현된 route의 responsive/accessibility 확인, 전체 parity inventory·security·deployment evidence를 fresh run한다.
   - 완료: 구현 범위와 미구현 범위가 matrix에 정확히 표시되고, clean backend gate·frontend gate·Compose config 검증 결과를 report에서 재현할 수 있다.

## Backlog

완료한 Goal:

- G1 Community: publication·discovery·engagement의 피드/상세/작성/수정/댓글/반응/북마크 사용자 여정
- G2 Relationship: public member profile과 follow/block 상태 변경, 차단·동시성 backend policy

완료한 추가 Goal:

- G3 Local/Care: 합성 resource의 지역 가이드·복지·케어 검색/상세
- G4 Gathering: 모임 생성·목록·상세·정원 잠금·참여/취소 lifecycle
- G5 Trust/Ops: 신고 저장·중복 방지·moderator queue/review와 기존 media 운영 경계
- G6 Migration/Deploy: Flyway seed, portfolio Compose, Caddy, backup script

남은 Goal:

- 전체 legacy parity와 실제 VPS 복구 리허설
