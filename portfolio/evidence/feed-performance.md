# Feed performance evidence

## Current status

현재 API와 동일한 jOOQ query builder 및 재현 절차를 고정하고, clean commit
`0546dfbc024708dffc3194e639d6cd75a36556e7`에서 100,000건 fixture baseline을
수집했다. 결과는 로컬 Docker의 합성 fixture 측정치이며 운영 SLA가 아니다.

| 지표 | 결과 |
| --- | ---: |
| p50 | 50.65ms |
| p95 | 55.63ms |
| p99 | 61.09ms |
| 처리량 | 19.48 req/s |
| 요청 수 | 2,630 |
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
`EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON)`을 저장한다.

## Contract under test

- Public feed: `GET /api/v1/discovery?limit=20`
- Popular feed: `GET /api/v1/discovery/popular?limit=20`
- Fixture: `publication` 100,000건, `volunteer_opportunity`와 `trust_report`는
  seed scale 규칙에 따른 합성 데이터
- Fixture 분포: publication ACTIVE 96,552건, DELETED 3,448건, `FREE_BOARD`
  100,000건, 검색어 적중 100,000건
- Query plan: `townpet_public_feed_item` view를 확장한 Append 뒤 top-N
  Sort/Limit plan, 21 rows, 2,612 shared hit blocks, actual execution
  103.475ms를 raw JSON에서 확인했다.
- 이전 V054 및 이전 local run 수치는 historical reference일 뿐 현재 baseline이나
  개선률 계산에 재사용하지 않는다.

## Raw artifacts

- run: `build/performance/runs/20260908T052759Z-feed-read-baseline-0546dfb/`
- seed: `build/performance/seeds/20260908T052754Z-large/`
- 공개 artifact: `portfolio/evidence/artifacts/feed/` 아래의
  `summary.json`, `metadata.txt`, `explain.json`, `distribution.tsv`와
  `checksums.sha256`. raw host log·JVM dump·절대 경로·secret은 포함하지 않았다.
- before/after 동일 조건 측정이 없으므로 개선률은 작성하지 않는다.

## Limitations

측정 전에는 p50/p95/p99, 처리량, 실패율, query plan 선택을 주장하지 않는다.
실행 후에는 동일 commit·fixture·profile의 raw artifact와 checksum을 근거로
수치를 갱신한다. 현재 구현과 동일 조건의 before/after가 없으면 개선률을
작성하지 않는다.
