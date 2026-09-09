# Feed performance evidence

## Current status

현재 API와 동일한 jOOQ query builder 및 재현 절차를 고정하고, clean commit
`2a9d3b8cddfc7333aa508c8b2dc148d29769b4b2`에서 100,000건 fixture baseline을
재수집했다. 결과는 합성 fixture 측정치이며 운영 SLA가 아니다.

| 지표 | 결과 |
| --- | ---: |
| p50 | 75.73ms |
| p95 | 102.20ms |
| p99 | 183.19ms |
| 처리량 | 12.28 req/s |
| 요청 수 | 1,658 |
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

모든 단계는 dirty diff와 untracked 파일이 있는 working tree를 거부한다.
`seed.sh`는 `build/performance/seeds/<seed-run-id>/`에 clean commit, fixture
분포, PostgreSQL/PostGIS 버전을 기록한다. `run.sh`는
`build/performance/runs/<run-id>/`에 commit, JAR/k6 image digest,
host/CPU/메모리, JVM 옵션, warm-up, VU, URL, k6 summary를 기록한다.
`feed-read`에서는 `explain-feed.sh`가 `CommunityFeedQuery`가 렌더링한
첫 페이지와 next cursor SQL/bind를 PostgreSQL에 그대로 실행해
`EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON)`을 저장한다. 이번 first-page
계획은 `Limit`, 21 rows, 2,612 shared hit blocks, 103.815ms actual total time이다.

## Contract under test

- Public feed: `GET /api/v1/discovery?limit=20`
- Popular feed: `GET /api/v1/discovery/popular?limit=20`
- Fixture: `publication` 100,000건, `volunteer_opportunity`와 `trust_report`는
  seed scale 규칙에 따른 합성 데이터
- Fixture 분포: publication ACTIVE 96,552건, DELETED 3,448건, `FREE_BOARD`
  100,000건, 검색어 적중 100,000건
- Query plan: `townpet_public_feed_item` view를 확장한 Append 뒤 top-N
  Sort/Limit plan, 21 rows, 2,612 shared hit blocks, actual execution
  103.815ms를 raw JSON에서 확인했다. first/next cursor 계획은 각각
  `explain-first.json`, `explain-next.json`에 보관한다.
- 이전 V054 및 이전 local run 수치는 historical reference일 뿐 현재 baseline이나
  개선률 계산에 재사용하지 않는다.

## Raw artifacts

- run: `build/performance/runs/20260909T014104Z-feed-read-baseline-2a9d3b8/`
- seed: `build/performance/seeds/20260909T014056Z-large/`
- 공개 artifact: `portfolio/evidence/artifacts/feed/` 아래의
  `summary.json`, `metadata.txt`, `query.sql`, `binds.txt`,
  `query-plan-metadata.txt`, `explain-first.json`, `explain-next.json`, `distribution.tsv`와
  `checksums.sha256`. raw host log·JVM dump·절대 경로·secret은 포함하지 않았다.
- before/after 동일 조건 측정이 없으므로 개선률은 작성하지 않는다.

## VPS public-read workload (separate evidence)

2026-09-09 netcup VPS에서 production public read endpoint만 대상으로 별도 ramp를
실행했다. local synthetic Feed fixture/query-plan baseline과 섞지 않는다.

| 조건 | 결과 |
| --- | ---: |
| profile | 15s@1 → 5m@10 → 5m@20 → 5m@40 |
| duration | 15m15s |
| maximum VU | 40 |
| requests | 62,346 |
| throughput | 68.12 req/s |
| p95 / p99 | 44.26ms / 48.91ms |
| HTTP failure | 0% |
| checks | 100% |

측정 중 backend는 healthy/restart 0을 유지했고 메모리는 약 397MiB에서 468MiB,
PostgreSQL은 약 68MiB에서 74MiB 범위였다. 이 실행은 read-only synthetic workload이며
고동시성 최대치·장기 soak·SLA·DB saturation을 증명하지 않는다. VPS k6 console 결과는
확인했지만 bind mount 권한 문제로 summary JSON raw artifact는 생성되지 않았으므로,
summary 파일이 있는 것처럼 주장하지 않는다.

## Limitations

측정 전에는 p50/p95/p99, 처리량, 실패율, query plan 선택을 주장하지 않는다.
실행 후에는 동일 commit·fixture·profile의 raw artifact와 checksum을 근거로
수치를 갱신한다. 현재 구현과 동일 조건의 before/after가 없으면 개선률을
작성하지 않는다.
