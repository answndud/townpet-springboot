# Feed performance evidence

## Current status

현재 API 기준으로 P3 변경 후 baseline을 수집했다. 이전 k6 시나리오의 폐기된
`/api/v1/feed` 및 `audience` query를 `/api/v1/discovery` 계약으로 교체하고,
cursor COUNT 제거 후 같은 large fixture에서 실행했다. 결과는 로컬 Docker
측정치이며 운영 SLA가 아니다.

| 지표 | 결과 |
| --- | ---: |
| p50 | 53.63ms |
| p95 | 80.85ms |
| p99 | 127.09ms |
| 처리량 | 17.26 req/s |
| 요청 수 | 2,330 |
| HTTP 실패 | 0% |
| k6 checks | 100% |

## Reproduction

```bash
./scripts/performance/validate.sh
./scripts/performance/prepare.sh large
./scripts/performance/start.sh
./scripts/performance/seed.sh large
./scripts/performance/run.sh --scenario feed-read --profile baseline
./scripts/performance/stop.sh
```

`seed.sh`는 `build/performance/seeds/<seed-run-id>/`에 fixture 분포와
PostgreSQL/PostGIS 버전을 기록한다. `run.sh`는
`build/performance/runs/<run-id>/`에 실행 commit, host/CPU/메모리, JVM 옵션,
warm-up, VU, URL, k6 summary와 DB container가 있을 때의
`EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON)`을 기록한다.

## Contract under test

- Public feed: `GET /api/v1/discovery?limit=20`
- Popular feed: `GET /api/v1/discovery/popular?limit=20`
- Fixture: `publication` 100,000건, `volunteer_opportunity`와 `trust_report`는
  seed scale 규칙에 따른 합성 데이터
- Fixture 분포: publication ACTIVE 96,552건, DELETED 3,448건, `FREE_BOARD`
  100,000건, 검색어 적중 100,000건
- Query plan: `publication_feed_cursor_ix` Index Scan, 21 rows, 5 shared hit
  blocks, actual execution 0.596ms
- 이전 V054의 scope 기반 인덱스 수치와 API는 현재 기준선으로 재사용하지 않는다.

## Raw artifacts

- run: `build/performance/runs/20260907T063822Z-feed-read-baseline-d9923c0/`
- seed: `build/performance/seeds/20260907T063817Z-large/`
- run commit: `d9923c02d02c93ff0ee37cc01d72e4832415b1c6`
- calibration: `build/performance/runs/20260907T064531Z-feed-read-calibration-d9923c0/` (failed: backend exited; not used as a performance claim)
- baseline `EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON)`: recorded in the run artifact with default planner settings.

## Limitations

측정 전에는 p50/p95/p99, 처리량, 실패율, query plan 선택을 주장하지 않는다.
실행 후에는 동일 commit·fixture·profile의 raw artifact를 근거로 수치를 갱신한다.
