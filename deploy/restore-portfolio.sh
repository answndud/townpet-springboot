#!/usr/bin/env sh
set -eu

: "${BACKUP_ROOT:?set BACKUP_ROOT to a paired backup directory}"
: "${POSTGRES_CONTAINER:?set POSTGRES_CONTAINER}"
: "${MINIO_CONTAINER:?set MINIO_CONTAINER}"
: "${POSTGRES_USER:?set POSTGRES_USER}"
: "${POSTGRES_DB:?set POSTGRES_DB}"
: "${MINIO_ACCESS_KEY:?set MINIO_ACCESS_KEY}"
: "${MINIO_SECRET_KEY:?set MINIO_SECRET_KEY}"
: "${MINIO_BUCKET:=townpet-media}"
: "${ALLOW_DESTRUCTIVE_RESTORE:?set ALLOW_DESTRUCTIVE_RESTORE=YES}"

if [ "$ALLOW_DESTRUCTIVE_RESTORE" != "YES" ]; then
  echo "refusing paired restore: set ALLOW_DESTRUCTIVE_RESTORE=YES" >&2
  exit 1
fi
[ -f "$BACKUP_ROOT/postgres.dump" ] || { echo "missing postgres.dump" >&2; exit 1; }
[ -d "$BACKUP_ROOT/media" ] || { echo "missing media directory" >&2; exit 1; }
[ -f "$BACKUP_ROOT/manifest.sha256" ] || { echo "missing manifest.sha256" >&2; exit 1; }
(cd "$BACKUP_ROOT" && sha256sum -c manifest.sha256)

docker exec -i "$POSTGRES_CONTAINER" pg_restore \
  --clean --if-exists --no-owner --exit-on-error \
  -U "$POSTGRES_USER" -d "$POSTGRES_DB" < "$BACKUP_ROOT/postgres.dump"

restore_id="$(date -u +%Y%m%dT%H%M%SZ)"
docker exec "$MINIO_CONTAINER" mc alias set townpet-restore http://127.0.0.1:9000 \
  "$MINIO_ACCESS_KEY" "$MINIO_SECRET_KEY" >/dev/null
docker exec "$MINIO_CONTAINER" mc mb --ignore-existing "townpet-restore/$MINIO_BUCKET" >/dev/null
docker exec "$MINIO_CONTAINER" mkdir -p "/tmp/townpet-media-restore-$restore_id"
docker cp "$BACKUP_ROOT/media/." "$MINIO_CONTAINER:/tmp/townpet-media-restore-$restore_id/"
docker exec "$MINIO_CONTAINER" mc mirror --overwrite --remove \
  "/tmp/townpet-media-restore-$restore_id" "townpet-restore/$MINIO_BUCKET"
docker exec "$MINIO_CONTAINER" rm -rf "/tmp/townpet-media-restore-$restore_id"
echo "restored paired backup: $BACKUP_ROOT"
