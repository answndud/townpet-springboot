# 성능 실행 결과 보관 규칙

이 폴더에는 실제로 실행한 성능 실험의 요약만 보관한다. 계획과 시나리오는 상위 문서에 둔다.

## 파일명

```text
<YYYY-MM-DD>-<scenario>-<commit>.md
```

예시:

```text
2026-08-20-public-read-medium-a1b2c3d.md
2026-08-21-contention-medium-d4e5f6a.md
```

## 결과 문서 템플릿

```md
# <시나리오> - <상태>

## Summary

- 실행일:
- commit:
- 상태: baseline | regression | improved | accepted | blocked
- 결론:

## Environment

- machine:
- CPU/RAM:
- OS:
- Java:
- Spring Boot:
- PostgreSQL/PostGIS:
- Docker:
- application profile:
- DB resource limit:

## Fixture and workload

- scale:
- row counts:
- VU:
- stages:
- warm-up:
- duration:
- request mix:
- command:

## HTTP result

| endpoint/group | requests | throughput | p50 | p95 | p99 | unexpected error |
|---|---:|---:|---:|---:|---:|---:|

## Resource and database result

- CPU:
- RSS/heap/GC:
- Hikari:
- PostgreSQL connections:
- lock/deadlock:
- slow query/query plan:

## Integrity checks

- duplicate rows:
- capacity/version violation:
- event duplicate/lost item:
- final row reconciliation:

## Findings and changes

### Finding

### Change

### Before/after

## Limitations

## Reproduction

```bash
# exact commands
```
```

## 원본 산출물

k6 raw JSON, Docker stats 원본, PostgreSQL explain 결과처럼 큰 파일은 `build/performance/`에 생성하고 저장소에는 넣지 않는다. 결과 문서에는 재생성 명령과 필요한 작은 요약만 기록한다.

측정하지 않은 값은 `N/A`로 남긴다. 추정값이나 다른 환경에서 가져온 수치를 채워 넣지 않는다.
