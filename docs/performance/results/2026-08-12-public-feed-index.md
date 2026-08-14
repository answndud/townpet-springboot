# 공개 feed cursor 인덱스 - improved

> Historical baseline: 이 측정은 publication에 GLOBAL/LOCAL scope와 전용 인덱스가 있던 V054 시점의 증거다. V062에서 해당 모델을 제거했으므로 아래 SQL과 수치는 과거 개선 근거로만 보존하고, 현재 API 재현에는 사용하지 않는다.

## Summary

- 실행일: 2026-08-12
- 상태: `improved`
- 결론: 100,000건의 합성 publication에서 `ACTIVE + GLOBAL + created_at/id DESC LIMIT 21` 경로가 병렬 순차 스캔·top-N 정렬에서 복합 인덱스 scan으로 바뀌었다. 같은 1 VU·30초 smoke에서 p95가 67.13ms에서 5.01ms로 감소했고, 처리량은 19.40 req/s에서 420.78 req/s로 증가했다.
- 변경: `V054__publication_global_feed_cursor_index.sql`의 `(lifecycle, scope, created_at DESC, id DESC)` 인덱스

## Environment

- machine: 로컬 macOS + Docker Desktop
- application: Spring Boot perf profile, `localhost:8081`
- database: PostgreSQL 18.4 + PostGIS, 전용 `townpet-postgres-perf` 컨테이너, host port `54331`
- load tool: `grafana/k6:0.52.0`
- fixture: publication 100,000 / volunteer 10,000 / trust report 10,000
- scale command: `./scripts/performance/seed.sh large`

## Workload

- scenario: `discovery-read` — `GET /api/v1/discovery?audience=GLOBAL&limit=20`
- VU: 1
- stages: 30초 동안 1 VU
- warm-up: 별도 warm-up 없이 smoke profile 실행
- before: DB에서 `publication_global_feed_cursor_ix`를 제거한 상태
- after: 동일 DB·fixture에 인덱스를 다시 생성한 상태

## HTTP result

| run | requests | throughput | p50 | p95 | p99 | HTTP 실패 |
|---|---:|---:|---:|---:|---:|---:|
| before `20260812T132447Z-feed-read-smoke-ee02e93` | 582 | 19.40 req/s | 48.85ms | 67.13ms | N/A | 0 |
| after `20260812T132702Z-feed-read-smoke-ee02e93` | 12,622 | 420.78 req/s | 1.67ms | 5.01ms | N/A | 0 |

`p99`는 현재 k6 summary 설정으로 생성하지 않았으므로 추정하지 않았다. 원본 JSON은 저장소에 넣지 않고 `build/performance/runs/<run-id>/summary.json`에서 재생성한다.

## Database evidence

### Before

`EXPLAIN (ANALYZE, BUFFERS)`는 `Parallel Seq Scan`과 `top-N heapsort`를 선택했고 실행시간은 95.784ms였다.

### After

같은 쿼리는 `publication_global_feed_cursor_ix`를 사용한 `Index Scan`으로 바뀌었고, 21행을 5 buffer hit로 읽어 실행시간은 1.849ms였다.

```sql
EXPLAIN (ANALYZE, BUFFERS)
SELECT id, author_member_id, type, scope, neighborhood_id,
       title, body, lifecycle, created_at, updated_at, version
FROM publication
WHERE lifecycle = 'ACTIVE' AND scope = 'GLOBAL'
ORDER BY created_at DESC, id DESC
LIMIT 21;
```

## Integrity and limitations

- k6 checks: before/after 모두 100% 통과
- 데이터 무결성: fixture 재주입 후 publication 100,000건 확인
- CPU/RSS/GC/Hikari/DB connection: 이번 실행에서는 별도 수집하지 않아 `N/A`
- 1 VU 로컬 실험이므로 운영 SLA나 다중 인스턴스 처리량으로 해석하지 않는다.
- before/after는 인덱스 유무 외 조건을 같게 맞췄지만, 완전한 통계적 benchmark가 아니므로 다음 단계에서 baseline profile 반복 측정으로 확인한다.

## Reproduction

```bash
./scripts/performance/prepare.sh large
./scripts/performance/start.sh
./scripts/performance/seed.sh large
./scripts/performance/run.sh --scenario feed-read --profile smoke
```

before 재현이 필요하면 전용 perf DB에서만 `DROP INDEX publication_global_feed_cursor_ix`를 실행하고 같은 명령을 수행한다. 이후 인덱스를 복원하거나 perf DB를 재생성한다.
