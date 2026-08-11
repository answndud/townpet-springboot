# PLAN.md

## Goal

기준 commit `7d8f6d0bd22dedd82350c05142823ab2d101574d`의 TownPet 사용자 경험을 Java 25·Spring Boot 4.1·React/Vite·PostgreSQL 18로 이전한다. 현재는 P2 domain vertical slice 단계이며 완료 이력은 Git과 [`docs/report/engineering-story.md`](docs/report/engineering-story.md)에서 확인한다.

## Active

1. G7-PARITY-HARDEN - 전체 parity와 출시 품질을 마무리한다
   - 현재: matrix의 `pending`과 `adapter`는 0개이며, moderator 운영 case queue·legacy board feed contract·댓글 lifecycle·correction API를 반영했다. ADR-0040 범위의 인증 경로 7개는 의도적으로 제외했다.
   - 남은 범위: 실제 VPS 복구·DNS·TLS 리허설을 수행한다.
   - 완료: 구현 범위와 미구현 범위가 matrix에 정확히 표시되고, clean backend gate·frontend gate·Compose config·복구 evidence를 report에서 재현할 수 있다.

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
