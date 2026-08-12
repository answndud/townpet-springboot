# TownPet 성능 검증 아카이브

이 폴더는 TownPet 백엔드의 성능을 **측정하고, 병목을 수정하고, 같은 조건에서 다시 비교한 근거**를 보관한다. 계획 문서와 결과 문서를 분리하며, 아직 실행하지 않은 부하·처리량·SLA를 성과처럼 기록하지 않는다.

## 현재 상태

- 상태: **측정 전 계획 확정 단계**
- 백엔드 release candidate gate: 통과
- PostgreSQL `EXPLAIN (ANALYZE, BUFFERS)` 대표 queue 검증: 통과
- PostgreSQL 동시 조회수 upsert 160건 검증: 통과
- HTTP 부하 테스트와 p50/p95/p99 처리량 측정: 아직 없음
- Redis·Kafka 도입 비교: 기준선 측정 후 판단

현재 확인된 query-plan·동시성 테스트와 실제 HTTP 부하 테스트를 혼동하지 않는다. 기존 근거는 [ReleaseCandidateQueryPlanTest.java](../../src/test/java/com/townpet/performance/ReleaseCandidateQueryPlanTest.java), 설계 설명은 [technical-notes.md](../report/technical-notes.md)에 있다.

## 읽는 순서

1. [methodology.md](methodology.md): 측정 규칙과 재현 조건
2. [scenarios.md](scenarios.md): 사용자·관리자·경합 시나리오
3. [redis-kafka-evaluation.md](redis-kafka-evaluation.md): 기준선 이후 확장 기술 판단 규칙
4. [results/README.md](results/README.md): 실행 결과 기록 형식
5. `results/<date>-<scenario>-<commit>.md`: 실제 실행 결과

## 문서 규칙

- 계획 문서에는 결과 수치를 작성하지 않는다.
- 결과 문서에는 commit SHA, fixture 규모, 부하 단계, 환경, 명령, 원본 산출물 위치를 함께 적는다.
- 동일 조건의 before/after만 비교한다. 데이터 규모·VU·duration·warm-up이 다르면 별도 실험으로 취급한다.
- 예상된 401/403/404/409는 기능 시나리오에 따라 별도 집계하고, 예상하지 못한 5xx·timeout·데이터 불변식 위반은 실패로 집계한다.
- local Docker 결과를 운영 SLA로 표현하지 않는다. VPS 실험은 네트워크 포함 시간과 서버 처리시간을 분리한다.
- Redis·Kafka는 “사용하면 빠르다”는 가정으로 추가하지 않는다. 병목과 일관성·운영 비용을 함께 기록하고, 기준선과 동일 조건으로 재측정한다.

## 결과 상태

결과 문서의 상태는 다음 중 하나만 사용한다.

| 상태 | 의미 |
|---|---|
| `planned` | 시나리오와 실행 조건만 정의됨 |
| `baseline` | 최적화 전 기준선 측정 완료 |
| `regression` | 기준선 대비 성능 저하 확인 |
| `improved` | 동일 조건에서 개선과 trade-off 확인 |
| `accepted` | 현재 규모와 목표에 충분하며 남은 한계를 기록함 |
| `blocked` | 환경·데이터·도구 문제로 재현할 수 없음 |
