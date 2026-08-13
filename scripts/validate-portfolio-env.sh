#!/usr/bin/env sh
set -eu

# Validate a trusted, local-only env file without printing secret values.
: "${1:?usage: validate-portfolio-env.sh /secure/path/townpet.portfolio.env}"
env_file="$1"
[ -r "$env_file" ] || { echo "env file is not readable" >&2; exit 1; }

# The operator owns this file; source it only from the explicitly supplied path.
set -a
. "$env_file"
set +a

required="POSTGRES_PASSWORD APP_DB_USER APP_DB_PASSWORD TOWNPET_EMAIL_ENABLED TOWNPET_EMAIL_FROM TOWNPET_PUBLIC_BASE_URL TOWNPET_EMAIL_TOKEN_ENCRYPTION_KEY SPRING_MAIL_HOST SPRING_MAIL_USERNAME SPRING_MAIL_PASSWORD TOWNPET_MINIO_PUBLIC_ENDPOINT TOWNPET_MINIO_ROOT_ACCESS_KEY TOWNPET_MINIO_ROOT_SECRET_KEY TOWNPET_MINIO_ACCESS_KEY TOWNPET_MINIO_SECRET_KEY TOWNPET_DOMAIN TOWNPET_MEDIA_DOMAIN"
for name in $required; do
  eval "value=\${$name-}"
  if [ -z "$value" ]; then
    echo "missing required variable: $name" >&2
    exit 1
  fi
  case "$value" in
    *replace-with*|*example.com*|*example.test*)
      echo "placeholder value is not allowed: $name" >&2
      exit 1
      ;;
  esac
done

[ "$TOWNPET_EMAIL_ENABLED" = "true" ] || {
  echo "TOWNPET_EMAIL_ENABLED must be true for the production portfolio profile" >&2
  exit 1
}
case "$TOWNPET_PUBLIC_BASE_URL" in https://*) ;; *) echo "TOWNPET_PUBLIC_BASE_URL must use https" >&2; exit 1 ;; esac
case "$TOWNPET_MINIO_PUBLIC_ENDPOINT" in https://*) ;; *) echo "TOWNPET_MINIO_PUBLIC_ENDPOINT must use https" >&2; exit 1 ;; esac
case "$TOWNPET_DOMAIN" in *://*|*/*) echo "TOWNPET_DOMAIN must be a host name" >&2; exit 1 ;; esac
case "$TOWNPET_MEDIA_DOMAIN" in *://*|*/*) echo "TOWNPET_MEDIA_DOMAIN must be a host name" >&2; exit 1 ;; esac
TOWNPET_MINIO_BUCKET=${TOWNPET_MINIO_BUCKET:-townpet-media}
[ "$TOWNPET_MINIO_BUCKET" = "townpet-media" ] || {
  echo "TOWNPET_MINIO_BUCKET must remain townpet-media for the least-privilege policy" >&2
  exit 1
}

# Fail before Compose if credentials or cryptographic material are obviously
# unsafe. This checks shape only; it never prints the secret values.
[ "${#POSTGRES_PASSWORD}" -ge 16 ] || { echo "POSTGRES_PASSWORD must be at least 16 characters" >&2; exit 1; }
[ "${#APP_DB_PASSWORD}" -ge 16 ] || { echo "APP_DB_PASSWORD must be at least 16 characters" >&2; exit 1; }
[ "${#SPRING_MAIL_PASSWORD}" -ge 12 ] || { echo "SPRING_MAIL_PASSWORD must be at least 12 characters" >&2; exit 1; }
[ "${#TOWNPET_EMAIL_TOKEN_ENCRYPTION_KEY}" -ge 32 ] || { echo "TOWNPET_EMAIL_TOKEN_ENCRYPTION_KEY is too short" >&2; exit 1; }
[ "$TOWNPET_MINIO_ROOT_ACCESS_KEY" != "$TOWNPET_MINIO_ACCESS_KEY" ] || { echo "MinIO root and application access keys must differ" >&2; exit 1; }
[ "$TOWNPET_MINIO_ROOT_SECRET_KEY" != "$TOWNPET_MINIO_SECRET_KEY" ] || { echo "MinIO root and application secret keys must differ" >&2; exit 1; }
[ "$SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE" = "true" ] || {
  echo "SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE must be true" >&2
  exit 1
}

public_host=${TOWNPET_PUBLIC_BASE_URL#https://}
public_host=${public_host%%/*}
media_host=${TOWNPET_MINIO_PUBLIC_ENDPOINT#https://}
media_host=${media_host%%/*}
[ "$public_host" = "$TOWNPET_DOMAIN" ] || {
  echo "TOWNPET_PUBLIC_BASE_URL host must equal TOWNPET_DOMAIN" >&2
  exit 1
}
[ "$media_host" = "$TOWNPET_MEDIA_DOMAIN" ] || {
  echo "TOWNPET_MINIO_PUBLIC_ENDPOINT host must equal TOWNPET_MEDIA_DOMAIN" >&2
  exit 1
}

echo "portfolio production env policy valid: $env_file"
