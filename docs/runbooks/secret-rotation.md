# Secret 교체

대상 secret은 PostgreSQL password, APP DB password, SMTP password, MinIO access/secret key, email token encryption key다.

1. 새 secret을 password manager에서 생성한다.
2. 새 값을 `.env`나 VPS secret store에 반영하되 Git에는 저장하지 않는다.
3. provider·PostgreSQL·MinIO 측 credentials를 먼저 새 값으로 허용한다.
4. backend를 rolling하지 않고 한 번에 재시작해 모든 instance가 동일 값을 읽게 한다.
5. login, password reset request, media presigned upload, health를 확인한다.
6. 이전 secret을 revoke한다.

`TOWNPET_EMAIL_TOKEN_ENCRYPTION_KEY`는 기존 event publication에 암호화된 token이 남아 있을 수 있으므로 무중단 교체하지 않는다. pending delivery가 없음을 확인하고 교체하거나, 별도 key versioning ADR을 만든다.
