# 최종 코드 feed smoke - accepted

## 실행 정보

- commit: `7a8e00f`
- 환경: local macOS + Docker, Spring Boot `perf` profile, PostgreSQL `townpet_perf`
- fixture: small, publication 2,000건
- 부하: k6 `feed-read`, 1 VU smoke
- 원본: `build/performance/runs/20260813T104318Z-feed-read-smoke-7a8e00f/`

## 결과

| 지표 | 결과 |
|---|---:|
| requests | 5,652 |
| throughput | 188.39 req/s |
| p50 | 4.81ms |
| p95 | 7.87ms |
| p99 | 11.37ms |
| checks | 100% |
| HTTP error | 0 |

## 판단

이번 계획에서 변경한 것은 production Compose bootstrap·env validation·demo reset 범위이며 feed query path는 변경하지 않았다. 따라서 기존 S0~S8 결과와의 회귀 비교를 위한 최종 smoke로 사용했고, public feed가 당시 코드에서 정상 응답하며 기존 query/index 기준선을 벗어나지 않는 것을 확인했다. 현재 V062 이후 API는 scope/audience 없이 동일한 active feed를 조회한다.

이 결과는 1 VU local smoke이므로 운영 SLA·VPS capacity·외부 네트워크 성능을 의미하지 않는다. Redis/Kafka 도입 판단을 바꾸지 않으며, 실제 VPS 공개 전에는 동일 fixture의 public/member/mixed workload와 resource metric을 다시 측정한다.
