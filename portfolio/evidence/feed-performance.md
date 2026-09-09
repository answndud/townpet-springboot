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

## VPS public-read soak evidence

2026-09-09에 현재 배포된 VPS를 대상으로 `public-read` read-only synthetic workload를
실행했다. `mixed` 시나리오처럼 publication을 생성하거나 인증·메일 경로를 호출하지
않았고, 30초 warm-up 뒤 30분 동안 최대 5 VU로 다음 세 endpoint만 호출했다.

- `GET /api/v1/discovery?limit=20`
- `GET /api/v1/discovery/popular?limit=20`
- `GET /api/v1/local-resources`

| 지표 | 결과 |
| --- | ---: |
| 실행 구간 | 2026-09-09T06:06:05Z–06:36:35Z (30분 30초) |
| requests / throughput | 18,850 / 10.30 req/s |
| p50 / p95 / p99 | 37.33ms / 49.35ms / 53.83ms |
| HTTP failure | 0% |
| checks | 100% (18,850/18,850) |
| 최대 VU | 5 |
| PostgreSQL CPU | 0.01–8.25% |
| PostgreSQL memory | 67.24–73.34MiB |

실행 전후 workload budget은 모두 PASS였다. 시작 시 DB connections 19/100(19%),
backend memory 34.06%, storage 10%였고, 종료 시 DB connections 19/100(19%),
backend memory 37.26%, storage 10%였다. 실행 중 backend/web는 healthy였고 restart는
0이었다. 이 결과는 해당 commit·VPS·5 VU read-only workload의 관측값이며,
사전에 정의된 SLO가 없으므로 성능 pass/fail이나 운영 SLA로 해석하지 않는다.

raw artifact는
`portfolio/evidence/artifacts/vps-public-read-soak/20260909T060605Z-public-read-soak-297209b/`
아래 `summary.json`, `console.log`, `resources.tsv`, `metadata.txt`,
`checksums.sha256`에 보관했고, 로컬에서 JSON·checksum을 재검증했다. 실행 commit은
`297209b65e10487fcbd13c8ff5c187d96e29f974`이며 공개 base URL은
`https://townpet.cloud`였다.

## VPS synthetic capacity gate (proposed)

다음 기준은 현재 VPS와 `public-read` endpoint 조합을 비교하기 위한 임시 capacity
gate다. 운영 SLA나 전체 서비스의 최대 용량으로 해석하지 않는다.

| 항목 | 제안 기준 | 5 VU soak 관측값 |
| --- | ---: | ---: |
| HTTP failure | < 1% | 0% |
| p95 | < 100ms | 49.35ms |
| p99 | < 200ms | 53.83ms |
| PostgreSQL connections | < 80% | 19% (19/100) |
| backend memory | < 80% | 37.26% (종료 시) |
| container restart | 0 | 0 |
| critical stop conditions | connection/memory/storage >= 90%, readiness failure, restart | 발생 없음 |

이 기준은 ramp에서 단계별 중단 여부를 판단하는 데만 사용한다. workload가
read-only이고 VU가 제한되어 있으며, 인증·write·메일·fault injection을 포함하지
않으므로 실제 사용자 capacity나 write path SLO를 증명하지 않는다.

## VPS public-read ramp against proposed gate

같은 VPS와 endpoint에서 `15s@1 warm-up → 5m@10 → 5m@20 → 5m@40` profile을
실행했다. 실행 commit은 `757436a3ef1a9d8c89ba1b4025fd3eb5db9fc2ab`이고, 실제
backend/web image revision은 `2e9d3f79a0bf2e48595321a75cab1c812d5720fe`였다.

| 지표 | 결과 | proposed gate | 판정 |
| --- | ---: | ---: | --- |
| requests / throughput | 59,667 / 65.19 req/s | 관측 | 관측값 |
| p95 | 68.06ms | < 100ms | 충족 |
| p99 | 82.73ms | < 200ms | 충족 |
| HTTP failure | 0% | < 1% | 충족 |
| checks | 100% (59,667/59,667) | 99% 이상 | 충족 |
| PostgreSQL CPU | 0.01–14.47% | 별도 SLO 없음 | 관측값 |
| PostgreSQL memory | 66.28–74.56MiB | 별도 SLO 없음 | 관측값 |
| 종료 DB connections | 19/100 (19%) | < 80% | 충족 |
| 종료 backend memory | 37.68% | < 80% | 충족 |
| container restart | 0 | 0 | 충족 |

실행 전후 workload budget은 PASS였고 readiness·backend/web health도 유지됐다. 따라서
이번 조건에서는 40 VU 단계까지 제안 gate를 위반하지 않았다. 이것은 `public-read`
세 endpoint와 현재 VPS resource에 대한 관측 가능한 결과이며, 40 VU를 최대 용량으로
단정하거나 write·인증·메일 경로의 SLO로 확장하지 않는다.

raw artifact는
`portfolio/evidence/artifacts/vps-public-read-ramp/20260909T095816Z-public-read-ramp-757436a/`
아래에 보관했다. 실행 중 SSH session이 종료됐지만 원격 k6와 결과 파일은 유지됐고,
summary·console·resources·metadata를 독립 SSH에서 checksum 생성·validate했다. 이
transport 사건은 workload 실패가 아니며, 장시간 실행 시 detached runner를 사용해야
한다는 운영 한계로 기록한다.

## Limitations

측정 전에는 p50/p95/p99, 처리량, 실패율, query plan 선택을 주장하지 않는다.
실행 후에는 동일 commit·fixture·profile의 raw artifact와 checksum을 근거로
수치를 갱신한다. 현재 구현과 동일 조건의 before/after가 없으면 개선률을
작성하지 않는다. raw summary가 생성되지 않았던 이전 VPS ramp 기록은 이 soak
evidence와 합산하지 않는다.
