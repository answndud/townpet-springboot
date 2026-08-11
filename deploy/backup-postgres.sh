#!/usr/bin/env sh
set -eu
: "${POSTGRES_CONTAINER:=townpet-springboot-postgres-1}"
: "${BACKUP_DIR:=./backups}"
mkdir -p "$BACKUP_DIR"
file="$BACKUP_DIR/townpet-$(date -u +%Y%m%dT%H%M%SZ).dump"
docker exec "$POSTGRES_CONTAINER" pg_dump -Fc -U "${POSTGRES_USER:-townpet_admin}" "${POSTGRES_DB:-townpet}" > "$file"
echo "created $file"
