#!/usr/bin/env sh
set -eu

: "${POSTGRES_CONTAINER:?set POSTGRES_CONTAINER}"
: "${MINIO_CONTAINER:?set MINIO_CONTAINER}"
: "${POSTGRES_USER:?set POSTGRES_USER}"
: "${POSTGRES_DB:?set POSTGRES_DB}"
: "${MINIO_ACCESS_KEY:?set MINIO_ACCESS_KEY}"
: "${MINIO_SECRET_KEY:?set MINIO_SECRET_KEY}"
: "${MINIO_BUCKET:=townpet-media}"
: "${BACKUP_DIR:=./backups}"

command -v docker >/dev/null || { echo "docker is required" >&2; exit 1; }
mkdir -p "$BACKUP_DIR"
backup_id="$(date -u +%Y%m%dT%H%M%SZ)"
backup_root="$BACKUP_DIR/townpet-$backup_id"
mkdir -p "$backup_root/media"

docker exec "$POSTGRES_CONTAINER" pg_dump -Fc -U "$POSTGRES_USER" "$POSTGRES_DB" \
  > "$backup_root/postgres.dump"

docker exec "$MINIO_CONTAINER" mc alias set townpet-backup http://127.0.0.1:9000 \
  "$MINIO_ACCESS_KEY" "$MINIO_SECRET_KEY" >/dev/null
docker exec "$MINIO_CONTAINER" sh -c "rm -rf /tmp/townpet-media-$backup_id && mkdir -p /tmp/townpet-media-$backup_id"
docker exec "$MINIO_CONTAINER" mc mirror --overwrite \
  "townpet-backup/$MINIO_BUCKET" "/tmp/townpet-media-$backup_id"
docker cp "$MINIO_CONTAINER:/tmp/townpet-media-$backup_id/." "$backup_root/media/"
docker exec "$MINIO_CONTAINER" rm -rf "/tmp/townpet-media-$backup_id"

{
  echo "backup_id=$backup_id"
  echo "created_at=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
  echo "postgres_database=$POSTGRES_DB"
  echo "media_bucket=$MINIO_BUCKET"
  echo "media_objects=$(find "$backup_root/media" -type f | wc -l | tr -d ' ')"
  echo "media_bytes=$(du -sk "$backup_root/media" | awk '{print $1 * 1024}')"
} > "$backup_root/manifest.txt"
(cd "$backup_root" && find . -type f ! -name manifest.sha256 -print0 | sort -z | xargs -0 sha256sum > manifest.sha256)
echo "created paired backup: $backup_root"
