# S3~S8 대표 workload - accepted with limitations

## Summary

- 실행일: 2026-08-12
- commit: `a9326a8` (실행 시점 short SHA)
- 상태: `accepted`
- 결론: 쓰기·상태 경합·운영 큐·미디어·혼합·장기 부하를 전용 PostgreSQL fixture에서 실행했다. 애플리케이션 5xx와 데이터 불변식 위반은 없었다. 20 VU spike에서 두 번 모두 k6 Docker-to-host bridge의 단일 `dial i/o timeout`이 발생했으므로 spike는 완전 무오류 결과가 아니라 환경 제한을 포함한 결과로 보관한다.

## Environment

- machine: MacBookAir10,1 arm64, 8 cores, 16 GB
- OS: macOS 26.5.2
- Java: OpenJDK 25.0.4
- database: PostgreSQL 18 + PostGIS 3.6, `townpet-postgres-perf`, host port `54331`
- application: Spring Boot perf profile, host port `8081`
- load tool: `grafana/k6:0.52.0` in Docker Desktop
- fixture: small (2,000 publications, 200 volunteer opportunities, 200 reports) unless stated otherwise

## HTTP results

| scenario | run | requests | throughput | p50 | p95 | p99 | checks | unexpected app error |
|---|---|---:|---:|---:|---:|---:|---:|---:|
| S3 write baseline | `20260812T134638Z` | 5,644 | 41.79/s | 7.77ms | 19.72ms | 27.31ms | 100% | 0 |
| S4 view contention, 8 VU | `20260812T135026Z` | 3,439 | 114.48/s | 6.09ms | 13.55ms | 29.10ms | 100% | 0 |
| S4 capacity contention, 20 VU | `20260812T145959Z` | 60 | 46.48/s | 25.08ms | 1,214.89ms | 1,237.07ms | 100% | 0; 10 expected 201 + 50 expected 409 |
| S5 moderator baseline | `20260812T135442Z` | 2,298 | 17.01/s | 6.58ms | 16.75ms | 23.94ms | 100% | 0 |
| S6 media smoke | `20260812T135706Z` | 1,027 | 34.12/s | 6.88ms | 16.79ms | 25.09ms | 100% | 0 |
| S7 mixed baseline | `20260812T135750Z` | 1,411 | 10.45/s | 11.87ms | 20.95ms | 41.50ms | 100% | 0 |
| S8 mixed spike, 20 VU | `20260812T140639Z` | 19,571 | 106.62/s | 8.20ms | 46.61ms | 115.20ms | 99.99% | 1 bridge timeout |
| S8 mixed soak, 5 VU / 30m | `20260812T140954Z` | 49,491 | 27.04/s | 9.11ms | 18.03ms | 36.94ms | 100% | 0 |

S8 spike의 두 번째 반복(`20260812T140323Z`)도 애플리케이션 응답 오류 없이 단일 bridge timeout이 발생했다. timeout은 Spring 로그에 5xx로 나타나지 않았고 k6가 `dial: i/o timeout`으로 보고했다.

## Integrity checks

- S3: 게시글 806개와 댓글 806개가 생성됐고, 각 write flow의 reaction/bookmark on→off와 read-back이 모두 성공했다. 비활성화 후 원장 중복은 없었다.
- S4 view: 최종 `publication_metric.view_count = 3,431`, 성공 increment 수와 일치했다. lost update는 없었다.
- S4 capacity: `volunteer_application`이 정확히 10개로 수렴했고 opportunity status가 `FULL`이 됐다. capacity 10 초과는 없었다.
- S5: 공개 fixture report를 운영 큐에서 읽고 일부를 bulk review했으며, moderation log·auth audit·case queue가 모두 2xx였다.
- S6: metadata→multipart content→checksum finalize→publication attachment가 205회 반복 성공했다.
- S8 soak: k6 checks 100%, HTTP error 0%; backend process 재시작과 Hikari timeout은 관찰되지 않았다.

## Resource observations

- soak `resources.tsv`는 5초마다 앱 PID와 perf DB container를 기록했다.
- soak 중 관찰 범위: Java process CPU 0~14%, RSS 55,984~276,304 KB, DB CPU 0.46~26.04%, DB memory 약 190~210 MiB.
- 종료 시 `jcmd GC.heap_info`: G1 heap committed 200,704 KB, used 98,466 KB.
- RSS는 실행 중 증가했지만 종료 GC 후 heap 사용량은 98MB였다. native memory·class cache를 분리 측정하지 않았으므로 “메모리 누수 없음”으로 단정하지 않고, 배포 전 VPS에서 동일 soak을 한 번 더 수행할 후속 항목으로 남긴다.

## Findings and changes

1. k6 session 요청에서 `SESSION`만 직접 지정하면 CSRF cookie가 빠져 반복 write가 403이 됐다. 하네스의 인증 header를 `SESSION`과 `XSRF-TOKEN`으로 명시해 실제 브라우저 계약을 재현했다.
2. perf fixture의 moderator hash가 문서 계정 비밀번호와 달라 401이 발생했다. perf seed가 문서화된 moderator credential을 명시적으로 보정하도록 했다.
3. perf reset은 write/media 상호작용이 남아 다음 실행을 오염시킬 수 있었다. synthetic publication의 comment/reaction/bookmark/upload/metric을 먼저 정리하는 재실행 경로를 추가했다.
4. capacity contention의 p95 약 1.19초는 `SELECT ... FOR UPDATE`로 같은 opportunity row를 직렬화한 비용이다. 이는 capacity 불변식을 위해 허용한 trade-off이며, row lock이 실제 병목으로 확인될 때만 counter/event 구조를 검토한다.

## Redis/Kafka decision

- Redis: `deferred`. 100,000 publication feed에서도 V054 복합 index 적용 후 p95 5.01ms를 기록했고, small S1·mixed·soak의 read p95도 13.63~20.95ms였다. 현재 측정에서는 cache가 해결할 DB read bottleneck이 입증되지 않았다.
- Kafka: `deferred`. write·mixed 요청의 후속 작업이 HTTP p95를 유의하게 차지한다는 증거와 consumer backlog가 없다. Spring Modulith event publication + PostgreSQL을 유지하고, notification/projection backlog가 실제로 생길 때 candidate-enabled 실험을 연다.

## Limitations

- 모든 수치는 local macOS + Docker Desktop 결과이며 운영 SLA가 아니다.
- DB connection pool 대기·PostgreSQL lock wait·Actuator Hikari 시계열은 이번 run script에서 별도 endpoint snapshot을 수집하지 않았다.
- spike의 Docker host bridge timeout은 VPS 네트워크 결과로 해석하지 않는다.
- 대량 bulk moderation before/after historical replay는 아직 실행하지 않았고, 기존 code/query-plan evidence와 이번 HTTP workload를 섞어 주장하지 않는다.

## Reproduction

```bash
./scripts/performance/prepare.sh small
./scripts/performance/start.sh
./scripts/performance/seed.sh small
./scripts/performance/run.sh --scenario write --profile baseline
./scripts/performance/run.sh --scenario contention --profile contention
CONTENTION_CASE=capacity ALLOW_EXPECTED_CONFLICTS=true ./scripts/performance/run.sh --scenario contention --profile contention
./scripts/performance/run.sh --scenario moderator --profile baseline
./scripts/performance/run.sh --scenario media --profile smoke
./scripts/performance/run.sh --scenario mixed --profile baseline
./scripts/performance/run.sh --scenario mixed --profile spike
./scripts/performance/run.sh --scenario mixed --profile soak
```

원본 k6 summary, console, resource sample, JVM snapshot은 `build/performance/runs/<run-id>/`에서 확인한다.
