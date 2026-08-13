#!/usr/bin/env bash
set -euo pipefail

# Repository-only security sanity checks. This does not replace a penetration test or VPS check.
repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_root"

fail() {
  echo "security static check failed: $1" >&2
  exit 1
}

grep -Fq 'secure: true' src/main/resources/application-production.yml \
  || fail "production session cookie is not explicitly secure"
grep -Fq 'same-site: lax' src/main/resources/application-production.yml \
  || fail "production session cookie SameSite policy is missing"
grep -Fq 'USER 10001:10001' deploy/Dockerfile.backend \
  || fail "backend image does not declare a non-root runtime user"
grep -Fq 'Strict-Transport-Security' deploy/Caddyfile \
  || fail "Caddy HSTS header is missing"
grep -Fq 'Content-Security-Policy' deploy/Caddyfile \
  || fail "Caddy CSP header is missing"
grep -Fq 'X-Content-Type-Options' deploy/Caddyfile \
  || fail "Caddy nosniff header is missing"
grep -Fq 'townpet-media-app' deploy/compose/portfolio.yml \
  || fail "MinIO application policy bootstrap is missing"
grep -Fq '"s3:PutObject"' deploy/compose/minio-policy.json \
  || fail "MinIO application policy does not allow required object writes"
grep -Fq '"s3:DeleteObject"' deploy/compose/minio-policy.json \
  || fail "MinIO application policy does not allow lifecycle deletion"
grep -Fq 'TOWNPET_MINIO_ROOT_ACCESS_KEY' deploy/compose/portfolio.yml \
  || fail "MinIO root credential is not separated from application credential"
grep -Fq 'RequestRateLimiter' src/main/java/com/townpet/identity/SessionController.java \
  || fail "login rate limiting is missing"
grep -Fq 'RequestRateLimiter' src/main/java/com/townpet/identity/GuestStepUpController.java \
  || fail "guest step-up rate limiting is missing"

echo "security static checks passed"
