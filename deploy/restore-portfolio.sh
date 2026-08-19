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

command -v docker >/dev/null || { echo "docker is required" >&2; exit 1; }

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

expected_media_objects="$(find "$BACKUP_ROOT/media" -type f | wc -l | tr -d ' ')"
restored_object_listing="$(docker exec "$MINIO_CONTAINER" mc find "townpet-restore/$MINIO_BUCKET" --print '{{.Key}}')"
restored_media_objects="$(printf '%s\n' "$restored_object_listing" | sed '/^$/d' | wc -l | tr -d ' ')"
[ "$expected_media_objects" = "$restored_media_objects" ] || {
  echo "restored media object count mismatch: expected=$expected_media_objects actual=$restored_media_objects" >&2
  exit 1
}

manifest_publications="$(sed -n 's/^db_publications=//p' "$BACKUP_ROOT/manifest.txt")"
if [ -n "$manifest_publications" ]; then
  restored_publications="$(docker exec "$POSTGRES_CONTAINER" psql -At -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c 'SELECT COUNT(*) FROM publication')"
  [ "$manifest_publications" = "$restored_publications" ] || {
    echo "restored publication count mismatch: expected=$manifest_publications actual=$restored_publications" >&2
    exit 1
  }
fi

manifest_upload_assets="$(sed -n 's/^db_upload_assets=//p' "$BACKUP_ROOT/manifest.txt")"
if [ -n "$manifest_upload_assets" ]; then
  restored_upload_assets="$(docker exec "$POSTGRES_CONTAINER" psql -At -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c 'SELECT COUNT(*) FROM upload_asset')"
  [ "$manifest_upload_assets" = "$restored_upload_assets" ] || {
    echo "restored upload asset count mismatch: expected=$manifest_upload_assets actual=$restored_upload_assets" >&2
    exit 1
  }
fi
echo "restored paired backup: $BACKUP_ROOT"
