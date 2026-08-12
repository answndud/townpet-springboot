# 3차 용량 경합 query shape 재검증

## Summary

- 실행일: 2026-08-13
- 상태: `accepted` (후보 기각, 기존 구현 유지)
- 목적: `VolunteerService.apply`의 전체 opportunity row lock 조회를 `capacity,status` projection으로 줄였을 때 실제 경합 latency가 재현 가능하게 개선되는지 검증한다.
- 결론: 개선이 재현되지 않았다. 후보 query는 원복했고, 전체 opportunity row를 읽는 기존 구현을 유지한다. row lock이 capacity 정합성을 보장하는 핵심 비용이라는 판단은 유지한다.

## Controlled variants

Before/after 모두 동일한 현재 working tree와 perf 설정으로 jar를 만들고, `VolunteerService.apply`의 lock query만 임시로 바꿔 비교했다. 사용자 작업 중인 다른 파일은 두 jar에 동일하게 포함했으며 staging·커밋하지 않았다.

| variant | lock query | SHA-256 |
|---|---|---|
| before | 전체 opportunity column + `FOR UPDATE` | `89ca6c12e808961afe03c827c6232581792235747396c275c9a5005f0dead953` |
| after | `capacity,status` + `FOR UPDATE` | `7f433e869468902c551245b214d4bb0884ff778b0007540fafb6fc24647821c0` |

실행 metadata의 repository HEAD는 `b817f7d`이며, jar SHA가 실제 variant를 식별한다. 원본 산출물은 `build/performance/runs/`에, 측정 묶음과 DB 대사는 `build/performance/revalidation/phase3/`에 있다.

## Fixed conditions

- PostgreSQL 18 + PostGIS 3.6, 전용 `townpet-postgres-perf`, port `54331`
- Spring Boot `perf` profile, port `8081`
- `grafana/k6:0.52.0`, 20 VU, 동일 opportunity capacity 10
- Small fixture: publication 2,000 / volunteer opportunity 200 / report 200
- 매 run 전 seed 후 target opportunity description을 `repeat('x', 5000)`으로 고정
- 각 variant: 2회 warm-up + 5회 측정
- 실행 순서 편향 확인을 위해 `before → after`와 `after → before` 두 block 수행
- 매 측정 후 `volunteer_application` 수와 opportunity 상태를 SQL로 대사

## Application latency results

`capacity_apply_duration`은 로그인 요청을 제외한 application POST의 k6 client duration이다. 모든 측정에서 checks 100%, 성공 10건·예상 충돌 10건이었다.

| block | variant | p95 values (ms) | median of block (ms) |
|---|---|---|---:|
| 1 | before | 58.74, 31.55, 32.58, 59.36, 53.45 | 53.45 |
| 1 | after | 108.31, 102.48, 117.00, 58.21, 60.45 | 102.48 |
| 2 (reverse) | after | 111.91, 152.45, 62.56, 86.26, 53.13 | 86.26 |
| 2 (reverse) | before | 75.22, 101.27, 87.00, 76.71, 96.32 | 87.00 |

전체 10회 중앙값은 before **67.29ms**, after **94.37ms**였다. 첫 block에서는 after가 느렸고, reverse block에서는 두 variant가 비슷했다. 따라서 query projection 변경이 latency를 개선했다고 재현성 있게 주장할 수 없다. 단일 run의 p95 차이를 퍼센트 개선으로 환산하지 않는다.

## Database evidence

`build/performance/revalidation/phase3/explain/before.txt`와 `after.txt`에 `EXPLAIN (ANALYZE, BUFFERS)`를 보관했다.

- 양쪽 모두 `volunteer_opportunity_pkey`를 사용하는 동일한 `Index Scan` + `LockRows` 계획
- 양쪽 모두 `shared hit=4`
- 단일 plan 실행 시간은 before 4.780ms, after 5.250ms로 after가 더 낮지 않았다. 이 값은 단일 EXPLAIN 실행이므로 HTTP 성능 수치로 해석하지 않는다.

PostgreSQL heap tuple과 동일 row lock을 여전히 읽기 때문에 projection 축소만으로 lock 대기와 heap 접근 비용을 제거하지 못했다. 현재 fixture의 large description에서도 개선이 재현되지 않은 근거와 일치한다.

## Integrity evidence

20회 측정 모두 별도 SQL 대사 결과가 다음과 같았다.

```text
volunteer_application count = 10
volunteer_opportunity    = FULL|10
```

즉 두 구현 모두 capacity 10 초과 insert가 없었고, 예상된 409 충돌이 정확히 수렴했다.

## Decision

- `capacity,status` projection 후보는 성능 개선 근거 부족으로 원복했다.
- 기존 전체 row `SELECT ... FOR UPDATE`는 현재 규모에서 유지한다. 이는 response projection이 아니라 신청 정합성을 위한 lock 경로다.
- 다음에 다시 최적화할 때는 row lock 대기·transaction time·connection pool을 직접 계측하고, 단순 projection보다 transaction 범위나 capacity counter 설계를 먼저 검토한다.
- Redis counter나 Kafka event로 lock을 우회하지 않는다. 정확한 capacity source of truth와 중복 신청 정책을 먼저 재설계해야 한다.

## Verification

```bash
./gradlew compileJava --no-daemon
./scripts/performance/validate.sh
```
