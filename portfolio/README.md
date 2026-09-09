# TownPet portfolio evidence

TownPet은 기존 반려동물 커뮤니티의 관찰 가능한 사용자 흐름을 Spring Boot·PostgreSQL 기반 모듈형 모놀리스로 재구현한 개인 프로젝트다.

## 먼저 읽을 순서

1. 저장소 [README](../README.md)에서 제품 범위와 실행 방법을 확인한다.
2. [architecture.md](architecture.md)에서 모듈 경계와 데이터 소유권을 확인한다.
3. [demo-guide.md](demo-guide.md)에서 공개 sandbox의 확인 순서와 데이터 경계를 확인한다.
4. 아래 대표 사례에서 문제 → 선택 → 구현 → 검증 → 한계를 읽는다.

## 대표 사례

- [피드 조회 성능](evidence/feed-performance.md): 현재 API 기준 cursor 조회, fixture, k6 baseline과 query plan.
- [모임 정원 경합](evidence/gathering-concurrency.md): 부모 row lock과 participant unique constraint의 역할 분리.
- [알림 전달·재처리](evidence/notification-delivery.md): at-least-once event 재전달, atomic dedup과 bounded recovery.
- [백업·복구](evidence/backup-restore.md): 운영 DB·media paired backup, disposable fresh restore와 signed media GET.

## 검증 범위

- CI: backend 전체 게이트, frontend typecheck/test/build, 보안·dependency 검사, frontend container smoke, ephemeral PostgreSQL 기반 live critical browser E2E(게시글 lifecycle·권한·모임 정원).
- 배포: CI promotion manifest의 immutable image digest를 netcup VPS에 배포하고 외부 health·discovery·raw HTML·asset 응답과 VPS live critical browser E2E 3개를 확인했다.
- 복구: 실제 운영 backup을 새 PostgreSQL·MinIO 환경에 복원했다. provider 전체 서버 재구축, WAL/PITR과 운영 트래픽 SLA는 검증 범위가 아니다.
- 성능: 공개된 수치는 로컬 Docker 합성 fixture baseline이며 운영 SLA가 아니다. release 판단은 VPS smoke/live E2E와 CI gate를 기준으로 한다.

## 공개 데모 경계

공개 환경은 합성 계정·콘텐츠만 사용하는 portfolio sandbox다. 실제 개인정보 수집과 공개 가입을 제공하지 않으며, 외부 사용량·성장률·상용 가용성을 주장하지 않는다.
