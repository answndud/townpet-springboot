# 로컬 SMTP 검증

기본 local profile은 토큰을 메모리 capture한다. 실제 SMTP 경로를 확인할 때만 Mailpit override를 사용한다.

```bash
cd /Users/alex/project/townpet-springboot
docker compose -f deploy/compose/local.yml -f deploy/compose/smtp-local.yml up --build
```

- Mailpit inbox: <http://localhost:8025>
- SMTP: `localhost:1025`

계정 복구 또는 이메일 인증 요청 후 inbox의 링크를 열어 confirm endpoint가 동작하는지 확인한다. Mailpit은 local synthetic data만 사용하고 외부 메일을 발송하지 않는다.

```bash
docker compose -f deploy/compose/local.yml -f deploy/compose/smtp-local.yml logs --tail=100 backend
```

검증할 것:

- unknown account도 동일한 202 응답
- token 원문이 backend log에 없음
- 메일 링크가 `TOWNPET_PUBLIC_BASE_URL`을 사용함
- token 만료·single use·password reset session revoke
- Mailpit 중지 시 backend가 3회 재시도 후 실패를 기록함
