# 2차 용량 경합 측정 보정

## Summary

- 실행일: 2026-08-13
- application commit under test: `944959e7e50d63d03d4b7a2fc94cd32617c3ac03`
- k6 harness change: recorded in the commit that adds this result document
- 상태: `improved`
- 목적: 1차 capacity contention의 aggregate p95에서 BCrypt 로그인 비용과 실제 volunteer application 비용을 분리한다.
- 결론: 같은 opportunity row를 잠그는 정책은 capacity 불변식을 지키는 데 필요한 상태로 유지한다. 실제 application endpoint의 p95는 238.98ms였고, 20건 모두 예상 응답(201 10건, 409 10건)으로 수렴했다.

## Why the first number was misleading

1차 capacity run은 각 VU가 한 번씩 CSRF를 받고 로그인한 뒤 application을 요청했다. 따라서 `http_req_duration`에는 20건의 BCrypt 로그인(약 2.6~2.8초)이 함께 들어갔다. aggregate p95 약 1.2~2.7초를 application row-lock 비용으로 설명하면 안 된다.

이번 2차에서는 k6에 `capacity_apply_duration`·`capacity_expected_response`·성공/충돌 카운터를 추가하고, application 요청만 별도 threshold로 평가했다.

## Environment and fixture

- machine: MacBookAir10,1 arm64, 8 cores, 16 GB
- OS: macOS 26.5.2
- Java: OpenJDK 25.0.4
- database: PostgreSQL 18 + PostGIS 3.6, `townpet-postgres-perf`, host port `54331`
- application: Spring Boot `perf` profile, port `8081`
- load tool: `grafana/k6:0.52.0` in Docker Desktop
- fixture: `small` (2,000 publications, 200 volunteer opportunities, 200 reports)
- workload: 20 VU, each VU one application request, same opportunity capacity 10

## Result

원본: `build/performance/runs/20260812T151245Z-contention-contention-944959e/`

| metric | result | interpretation |
|---|---:|---|
| `capacity_apply_duration` p50 | 191.62ms | application endpoint only |
| `capacity_apply_duration` p95 | 238.98ms | dedicated acceptance threshold `< 1s` 통과 |
| `capacity_apply_duration` p99 | 253.24ms | same row lock 경합 포함 |
| expected response | 100% (20/20) | 201 또는 409만 반환 |
| success | 10 | capacity 10건 수용 |
| conflict | 10 | 초과 신청 10건 거절 |
| final application rows | 10 | fixture 대사와 일치 |
| final opportunity status | `FULL` | capacity 불변식 유지 |

전체 k6 요청의 p95는 2.43초였지만, 이는 20건의 로그인 요청이 포함된 값이다. 인증 경로와 도메인 mutation을 하나의 SLA로 합치지 않고 별도 metric으로 보관한다.

## Decision

- production `SELECT ... FOR UPDATE`와 transaction 경계를 변경하지 않는다. row lock을 제거하고 Redis counter나 비동기 event로 우회하면 초과 신청 방지와 source-of-truth 일관성을 다시 설계해야 한다.
- 이번 2차의 개선은 production 알고리즘 변경이 아니라 측정 하네스의 오탐 제거다. 따라서 “애플리케이션이 몇 배 빨라졌다”고 주장하지 않는다.
- RSS 증가는 1차 30분 soak에서 JVM heap 사용량(종료 시 약 98MB)과 native/class cache를 분리하지 못한 상태다. 현재는 누수로 단정하지 않고, VPS 배포 전 동일 soak을 한 번만 재현하는 후속 항목으로 둔다.

## Short mixed-load memory check

원본: `build/performance/runs/20260812T151433Z-mixed-calibration-3247ef5/`

- 5 VU calibration(15초 ramp + 3분 유지), checks 4,965/4,965 (100%)
- HTTP p95 21.99ms, p99 32.04ms, unexpected error 0
- active load 중 Java RSS 관찰 범위 60,752~472,608 KB
- 부하 종료 후 약 1분 idle 뒤 RSS 40,880 KB, `jcmd GC.heap_info` heap committed 215,040 KB / used 125,762 KB

활성 부하 중 RSS가 확장됐지만 idle 후 높은 값이 유지되지 않았다. 이 짧은 실행만으로 leak 없음이나 운영 메모리 상한을 단정하지 않는다. 다만 1차 soak의 RSS 증가를 즉시 production memory tuning으로 연결할 근거도 부족하므로, JVM 옵션 변경 없이 배포 전 VPS에서 동일한 30분 soak과 cgroup memory를 재확인한다.

## Reproduction

```bash
./scripts/performance/prepare.sh small
# 별도 지속 터미널에서 perf profile을 기동한다.
./scripts/performance/start.sh
./scripts/performance/seed.sh small
CONTENTION_CASE=capacity ALLOW_EXPECTED_CONFLICTS=true \
  ./scripts/performance/run.sh --scenario contention --profile contention
```

`loadtest/contention.js`의 capacity 전용 metric과 threshold가 로그인 비용을 제외한 application latency를 검증한다. `start.sh`를 일회성 셸에서 실행해 백그라운드 프로세스가 종료되는 환경에서는, 애플리케이션을 지속 터미널 세션에서 실행한 뒤 k6를 별도 터미널에서 실행한다.
