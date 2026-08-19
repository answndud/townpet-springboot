#!/usr/bin/env bash
set -euo pipefail

: "${APP_DB_USER:?APP_DB_USER is required}"
: "${APP_DB_PASSWORD:?APP_DB_PASSWORD is required}"
: "${MIGRATION_DB_USER:?MIGRATION_DB_USER is required}"
: "${MIGRATION_DB_PASSWORD:?MIGRATION_DB_PASSWORD is required}"

psql -v ON_ERROR_STOP=1 \
  --username "$POSTGRES_USER" \
  --dbname "$POSTGRES_DB" \
  -v app_user="$APP_DB_USER" \
  -v app_password="$APP_DB_PASSWORD" <<'SQL'
SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', :'app_user', :'app_password') \gexec
SQL

psql -v ON_ERROR_STOP=1 \
  --username "$POSTGRES_USER" \
  --dbname "$POSTGRES_DB" \
  -v migration_user="$MIGRATION_DB_USER" \
  -v migration_password="$MIGRATION_DB_PASSWORD" <<'SQL'
SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', :'migration_user', :'migration_password') \gexec
SQL

psql -v ON_ERROR_STOP=1 \
  --username "$POSTGRES_USER" \
  --dbname "$POSTGRES_DB" \
  -v app_user="$APP_DB_USER" <<'SQL'
SELECT format('GRANT CONNECT ON DATABASE %I TO %I', current_database(), :'app_user') \gexec
SELECT format('GRANT USAGE ON SCHEMA public TO %I', :'app_user') \gexec
SQL

psql -v ON_ERROR_STOP=1 \
  --username "$POSTGRES_USER" \
  --dbname "$POSTGRES_DB" \
  -v app_user="$APP_DB_USER" \
  -v migration_user="$MIGRATION_DB_USER" <<'SQL'
SELECT format('GRANT CONNECT ON DATABASE %I TO %I', current_database(), :'migration_user') \gexec
SELECT format('GRANT USAGE, CREATE ON SCHEMA public TO %I', :'migration_user') \gexec
SELECT format('ALTER DEFAULT PRIVILEGES FOR ROLE %I IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO %I', :'migration_user', :'app_user') \gexec
SELECT format('ALTER DEFAULT PRIVILEGES FOR ROLE %I IN SCHEMA public GRANT USAGE, SELECT, UPDATE ON SEQUENCES TO %I', :'migration_user', :'app_user') \gexec
SQL
