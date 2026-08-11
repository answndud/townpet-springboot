#!/usr/bin/env sh
set -eu

: "${BACKUP_FILE:?set BACKUP_FILE to an existing custom-format dump}"
: "${POSTGRES_CONTAINER:=townpet-springboot-postgres-1}"
: "${POSTGRES_USER:=townpet_admin}"
: "${POSTGRES_DB:=townpet}"

if [ ! -f "$BACKUP_FILE" ]; then
  echo "backup file does not exist: $BACKUP_FILE" >&2
  exit 1
fi
if [ "${ALLOW_DESTRUCTIVE_RESTORE:-}" != "YES" ]; then
  echo "refusing restore: set ALLOW_DESTRUCTIVE_RESTORE=YES explicitly" >&2
  exit 1
fi

docker exec -i "$POSTGRES_CONTAINER" pg_restore \
  --clean --if-exists --no-owner --exit-on-error \
  -U "$POSTGRES_USER" -d "$POSTGRES_DB" < "$BACKUP_FILE"
echo "restored $BACKUP_FILE into $POSTGRES_CONTAINER/$POSTGRES_DB"
