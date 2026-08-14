# 공개 feed Redis 평가 - deferred

## Summary

- 실행일: 2026-08-13
- commit: `c2b5ca9`
- 상태: `accepted`
- 결론: 현재 public feed는 PostgreSQL 단독 경로가 이미 충분히 빠르다. Redis를 production path에 추가하지 않고, 실제 DB 부하 또는 반복 read 병목이 관찰될 때 다시 평가한다.

이번 단계는 Redis를 억지로 붙이는 구현 작업이 아니라, 최신 feed 변경 후 기준선이 여전히 유효한지 확인하고 도입 비용을 정당화할 병목이 있는지 결정하는 단계다.

## Environment

- machine: local Docker performance environment
- application: Spring Boot executable jar, `perf` profile, port `8081`
- database: dedicated PostgreSQL/PostGIS container, port `54331`
- load generator: k6 in Docker
- Redis: not enabled; candidate implementation was intentionally not added

## Fixture and workload

- scale: `small`
- endpoint: `GET /api/v1/discovery?limit=20` (scope 제거 전 측정 문서는 historical baseline으로 보존)
- VU: 1
- profile: `smoke` (30 seconds)
- repetitions: 3
- seed: `./scripts/performance/seed.sh small` before every run
- request checks: HTTP 200 and valid feed response

## PostgreSQL-only baseline

| run | requests | throughput | p50 | p95 | p99 | checks | errors |
|---|---:|---:|---:|---:|---:|---:|---:|
| `20260813T032512Z` | 6,911 | 230.40/s | 3.80 ms | 6.40 ms | 12.72 ms | 100% | 0 |
| `20260813T032549Z` | 8,666 | 288.80/s | 3.38 ms | 4.35 ms | 5.73 ms | 100% | 0 |
| `20260813T032626Z` | 8,348 | 278.25/s | 3.14 ms | 5.20 ms | 10.21 ms | 100% | 0 |

관측된 p95 범위는 4.35~6.40ms이고 세 실행 모두 unexpected error가 없었다. 이 조건에서는 Redis cache가 해결할 read latency 또는 DB saturation이 확인되지 않았다.

## Redis 판단

### 보류 근거

public feed 결과를 Redis에 저장하면 다음을 새로 보장해야 한다.

- 게시글 공개 범위·삭제·차단 변경 시 cache invalidation
- cursor와 animal-interest 필터별 key 분리
- Redis 장애·eviction 시 PostgreSQL fallback
- stale feed가 권한·공개범위 변경을 넘지 않는지 검증

현재 baseline은 이 복잡성을 추가해 얻을 측정 가능한 이득이 없다. 따라서 Redis 의존성·cache layer·운영 컨테이너를 추가하지 않았다. 이는 Redis가 영구적으로 불필요하다는 뜻이 아니라, 현재 규모와 workload에서 도입 조건이 충족되지 않았다는 뜻이다.

### 재평가 조건

다음 중 하나가 재현되면 동일 fixture와 동일 profile로 `postgres-only`와 `candidate-enabled`를 비교한다.

- public read에서 DB CPU·buffer read·connection 대기가 지속적으로 상승
- 동일 cursor의 반복 read가 실제 처리량 한계가 됨
- VPS 환경에서 p95가 목표를 넘고 query/index 개선으로 해결되지 않음

재평가 시에는 hit/miss, stale·invalidation, Redis 장애 fallback, rollback을 한 세트로 기록한다.

## Kafka 판단

이번 feed read 실험은 Kafka 후보와 직접 관련이 없다. 현재 Spring Modulith Event Publication Registry와 PostgreSQL에서 request 후속 작업 backlog 또는 consumer lag이 관찰되지 않았고, 핵심 transaction을 별도 broker로 옮길 근거도 없다. Kafka는 notification·projection 등 eventual consistency가 허용되는 후속 작업이 실제로 응답시간을 차지할 때 별도 write workload로 평가한다.

## Reproduction

```bash
./scripts/performance/prepare.sh small
# perf profile 애플리케이션을 8081 포트로 기동
for n in 1 2 3; do
  ./scripts/performance/seed.sh small
  ./scripts/performance/run.sh --scenario feed-read --profile smoke
done
```

원본 k6 결과는 저장소에 커밋하지 않고 다음 경로에 남는다.

```text
build/performance/revalidation/phase4/baseline/
build/performance/runs/20260813T032512Z-feed-read-smoke-f76dd66/
build/performance/runs/20260813T032549Z-feed-read-smoke-f76dd66/
build/performance/runs/20260813T032626Z-feed-read-smoke-f76dd66/
```

## Limitations

- local Docker의 1 VU smoke 결과이므로 운영 SLA가 아니다.
- Redis를 실제로 연결한 A/B 결과가 아니라, 최신 PostgreSQL 기준선에 근거한 도입 보류 결정이다.
- 다음 성능 단계는 VPS 환경에서 동일 feed workload와 DB resource를 확인하는 것이다.
