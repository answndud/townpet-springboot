# 재시작·장애 대응

## 확인 순서

```bash
docker compose -f deploy/compose/portfolio.yml ps
docker compose -f deploy/compose/portfolio.yml logs --tail=200 backend
docker compose -f deploy/compose/portfolio.yml logs --tail=100 postgres minio web
```

1. web만 실패하면 backend health와 Caddy upstream을 확인한다.
2. backend만 실패하면 최근 migration·SMTP·MinIO 초기화 오류를 확인한다.
3. PostgreSQL이 unhealthy면 volume을 삭제하지 않고 disk·connection·복구 로그를 확인한다.
4. MinIO가 unhealthy면 object volume과 credentials를 확인한다. bucket을 재생성하기 전에 object 손실 여부를 확인한다.

## 안전한 재시작

```bash
docker compose -f deploy/compose/portfolio.yml restart backend
docker compose -f deploy/compose/portfolio.yml restart web
```

DB·MinIO container는 원인을 확인하기 전 무작정 재생성하지 않는다. 재시작 전후로 health와 최근 error trace ID를 기록한다.

## 장애 종료 조건

- readiness와 web 응답이 회복됨
- SMTP·MinIO·DB error가 새로 쌓이지 않음
- 최근 backup이 존재하고 manifest checksum이 검증됨
- 복구하지 못했으면 `rollback.md` 절차로 전환
