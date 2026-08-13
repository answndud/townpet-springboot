# 최소 관측성

production에서 우선 확인할 신호는 외부 SaaS가 아니라 container·Actuator·구조화 log다.

```bash
curl -fsS https://townpet.example.com/actuator/health
docker compose -f deploy/compose/portfolio.yml ps
docker compose -f deploy/compose/portfolio.yml logs --tail=200 backend
```

확인 기준:

- readiness가 DB를 포함해 `UP`
- 모든 HTTP 요청에 `X-Trace-Id`와 같은 MDC trace id가 존재
- token·password·query string이 log에 없음
- `account_token_delivery_failed`와 `account_token_delivery_exhausted`가 SMTP 장애를 설명함
- backup manifest와 checksum 파일이 생성됨
- PostgreSQL·MinIO disk 사용량이 VPS 여유 공간을 침해하지 않음

외부 collector, Sentry, Prometheus와 장기 retention은 실제 운영 신호와 비용을 확인한 뒤 추가한다.
