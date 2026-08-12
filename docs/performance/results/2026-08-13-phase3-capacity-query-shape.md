# 3차 용량 경합 query shape 개선

## Summary

- 실행일: 2026-08-13
- application 기준선: `944959e7e50d63d03d4b7a2fc94cd32617c3ac03`
- 변경: `VolunteerService.apply`의 `SELECT ... FOR UPDATE`가 본문·게시자·시각 등 사용하지 않는 컬럼을 읽지 않고 `capacity,status`만 잠근다.
- 상태: `improved` (소폭 개선; 동일 조건의 실행 편차를 함께 기록)
- 유지한 것: 같은 `volunteer_opportunity` row lock, transaction 경계, application count, unique 제약과 `FULL` 전환 규칙

## Why this change

capacity 신청은 lock을 획득한 뒤 정원과 상태만 확인한다. 이전 query는 response에 필요한 전체 opportunity projection을 다시 읽어 lock 대기 중 불필요한 row payload와 mapping 비용을 포함했다. query shape만 줄이고, 초과 신청을 막는 serialization 정책은 변경하지 않았다.

## Environment and workload

- PostgreSQL 18 + PostGIS 3.6, 전용 `townpet-postgres-perf`, port `54331`
- Spring Boot `perf` profile, port `8081`
- `grafana/k6:0.52.0`, 20 VU / 동일 opportunity capacity 10
- 매 run 전에 `./scripts/performance/seed.sh small` 실행
- 모든 run에서 expected response 20/20, 성공 10건·충돌 10건

## Before / after

원본 k6 산출물은 `build/performance/runs/` 아래에 있다.

| code path | run | application p95 | p99 | checks |
|---|---|---:|---:|---:|
| before: 전체 opportunity row 조회 | `20260812T152406Z` | 294.58ms | 321.53ms | 100% |
| before: 전체 opportunity row 조회 | `20260812T152413Z` | 135.14ms | 186.60ms | 100% |
| after: `capacity,status`만 조회 | `20260812T152502Z` | 198.31ms | 205.37ms | 100% |
| after: `capacity,status`만 조회 | `20260812T152509Z` | 114.36ms | 126.85ms | 100% |
| after: `capacity,status`만 조회 | `20260812T152529Z` | 97.34ms | 108.91ms | 100% |

after 실행의 p95 범위는 97.34~198.31ms였고, before 실행은 135.14~294.58ms였다. 같은 local Docker 환경에서도 첫 경합 run과 warm run의 편차가 있으므로 특정 한 run을 기준으로 “몇 % 향상”이라고 주장하지 않는다. 관찰 가능한 개선은 query가 필요한 컬럼만 읽도록 줄였다는 점과 warm run p95가 기준선 하단보다 낮아진 점이다.

## Integrity and trade-off

- 20건 중 10건만 insert되고 10건은 409로 거절됐다.
- opportunity status는 `FULL`, application row 수는 capacity와 동일하게 수렴했다.
- row lock을 제거하지 않았으므로 초과 신청 방지와 PostgreSQL source-of-truth는 유지된다.
- query projection을 별도 공유 DTO로 만들지 않고 capacity 경로 내부 record로 제한해 module 경계를 늘리지 않았다.
- latency 편차가 있으므로 더 큰 capacity·Medium fixture·운영 VPS에서 반복 측정하기 전에는 큰 최적화로 표현하지 않는다.

## Verification

```bash
./gradlew compileJava --no-daemon
./scripts/performance/validate.sh
```

실제 부하 명령은 다음과 같다.

```bash
./scripts/performance/seed.sh small
CONTENTION_CASE=capacity ALLOW_EXPECTED_CONFLICTS=true \
  ./scripts/performance/run.sh --scenario contention --profile contention
```
