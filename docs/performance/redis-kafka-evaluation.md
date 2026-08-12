# Redis·Kafka 도입 평가 계획

Redis와 Kafka는 성능을 위해 미리 넣는 인프라가 아니다. 현재 PostgreSQL 단일 source of truth와 Spring Modulith event publication이 충분한지 먼저 측정하고, 병목이 확인된 한 지점에만 실험적으로 적용한다.

## 평가 순서

```text
PostgreSQL 기준선
→ 병목 분류
→ 단일 후보 적용
→ 동일 fixture·동일 VU로 재측정
→ consistency·장애·운영 비용 확인
→ 채택/보류 결정
```

한 실험에서 Redis와 Kafka를 동시에 추가하지 않는다. 그래야 개선 원인과 부작용을 분리할 수 있다.

## 병목 분류

| 관찰 결과 | 우선 후보 | 먼저 확인할 것 |
|---|---|---|
| 동일 public read가 반복되고 DB read가 병목 | Redis cache | cache hit/miss·stale·invalidation |
| rate limit counter가 DB write를 늘림 | Redis counter | atomic increment·expiry·장애 fallback |
| request 후속 notification이 응답을 지연 | Kafka 또는 기존 event 비동기화 | 사용자 응답시간과 최종 처리시간 |
| 대량 fan-out·retry backlog가 발생 | Kafka | consumer lag·중복·순서·재처리 |
| 단순 query가 느림 | Redis/Kafka 아님 | query plan·index·projection 우선 |
| 단일 DB lock이 병목 | Redis lock을 즉시 도입하지 않음 | transaction 범위·index·row lock 재검토 |

## Redis 실험

### 후보 1 - 읽기 cache

대상은 mutation 빈도가 낮고 반복 조회가 많은 public catalog·local guide 등으로 제한한다. 개인화 feed나 정확한 권한 결과를 먼저 cache하지 않는다.

비교 항목:

- PostgreSQL 직접 조회 대비 p50/p95/p99
- DB CPU·query 수·buffer hit 변화
- cache hit/miss
- TTL 만료 직후 latency
- update 후 stale response 지속시간
- Redis unavailable/eviction 시 PostgreSQL fallback

채택 조건:

- 동일 기능 계약을 유지한다.
- p95 또는 DB 부하가 실제로 개선된다.
- invalidation 누락으로 privacy·권한·상태 오류가 발생하지 않는다.
- Redis 없이도 안전하게 degraded 동작한다.

### 후보 2 - rate limit/counter

조회수나 rate limit처럼 원자 counter가 적합한지 별도 평가한다. 최종 영속 값과 집계 지연 허용 여부를 먼저 정의하지 않으면 Redis counter를 추가하지 않는다.

## Kafka 실험

### 후보 1 - 후속 notification

요청 transaction에서 반드시 끝나야 하는 member/publication 상태 변경은 Kafka로 옮기지 않는다. 알림·검색 projection·analytics처럼 eventual consistency가 허용되는 후속 작업만 후보로 둔다.

비교 항목:

- 동기 처리 대비 HTTP p95
- 이벤트 발행 성공·DB commit 순서
- producer retry와 중복 event
- consumer lag와 end-to-end 완료시간
- consumer 재시작 후 재처리 결과
- broker 장애·backlog 복구

채택 조건:

- source transaction의 사실과 event의 관계가 명확하다.
- consumer idempotency와 retry가 테스트된다.
- 사용자가 즉시 봐야 하는 데이터가 stale 상태로 노출되지 않는다.
- Kafka 운영 비용이 현재 단일 VPS 목표와 맞는다.

## 결과 기록 표준

Redis·Kafka 실험은 아래 네 조건을 한 세트로 기록한다.

1. `postgres-only`: 현재 구조 기준선
2. `candidate-enabled`: 후보 기술을 추가한 상태
3. `failure-mode`: 후보 기술 장애 또는 지연 상태
4. `rollback`: 후보 제거 후 원래 경로 복구

각 상태에 대해 다음을 기록한다.

- 동일 commit 또는 변경 diff
- fixture scale·VU·duration·warm-up
- p50/p95/p99·처리량·오류율
- DB·Redis·Kafka CPU/memory/connection/lag
- stale·duplicate·lost update 여부
- 운영 구성과 비용 증가
- 최종 `accepted` 또는 `deferred` 판단

## 현재 판단

S0~S8 측정 이후에도 Redis와 Kafka는 `deferred`다. 100,000건 public feed의 V054 index after p95가 5.01ms였고, mixed baseline p95 20.95ms·30분 soak p95 18.03ms에서 cache가 해결할 DB read bottleneck이 확인되지 않았다. Kafka 역시 write·mixed 요청에서 후속 작업 backlog나 consumer lag을 측정해야 할 필요가 나타나지 않았다.

따라서 지금은 후보 기술을 production path에 추가하지 않는다. 이후 VPS에서 DB CPU·connection/lock wait 또는 notification/projection backlog가 실제로 확인되면, 그때 같은 fixture와 동일 VU로 `postgres-only`와 `candidate-enabled`를 비교하고 failure-mode·rollback까지 기록한다. 현재 결과는 “Redis/Kafka가 불필요하다”가 아니라 “현재 증거로는 비용을 정당화할 병목이 없다”는 판단이다.
