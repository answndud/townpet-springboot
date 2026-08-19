#!/usr/bin/env sh
set -eu
umask 077

: "${POSTGRES_CONTAINER:?set POSTGRES_CONTAINER}"
: "${MINIO_CONTAINER:?set MINIO_CONTAINER}"
: "${POSTGRES_USER:?set POSTGRES_USER}"
: "${POSTGRES_DB:?set POSTGRES_DB}"
: "${MINIO_ACCESS_KEY:?set MINIO_ACCESS_KEY}"
: "${MINIO_SECRET_KEY:?set MINIO_SECRET_KEY}"
: "${MINIO_BUCKET:=townpet-media}"
: "${BACKUP_DIR:=./backups}"
: "${BACKUP_ALERT_WEBHOOK_URL:=}"

command -v docker >/dev/null || { echo "docker is required" >&2; exit 1; }
mkdir -p "$BACKUP_DIR"
backup_id="$(date -u +%Y%m%dT%H%M%SZ)"
started_epoch="$(date +%s)"
backup_root="$BACKUP_DIR/townpet-$backup_id"
mkdir -p "$backup_root/media"
backup_complete=0
cleanup_failed_backup() {
  if [ "$backup_complete" -ne 1 ] && [ -n "$backup_root" ] && [ -d "$backup_root" ]; then
    rm -rf -- "$backup_root"
  fi
}
notify_backup_failure() {
  status="$?"
  if [ "$status" -ne 0 ] && [ "$backup_complete" -ne 1 ]; then
    cleanup_failed_backup
    if [ -n "$BACKUP_ALERT_WEBHOOK_URL" ] && command -v curl >/dev/null 2>&1; then
      curl --fail --silent --show-error --max-time 10 \
        -X POST \
        -H 'Content-Type: application/json' \
        --data "{\"event\":\"townpet_backup_failed\",\"backup_id\":\"$backup_id\"}" \
        "$BACKUP_ALERT_WEBHOOK_URL" >/dev/null ||
        echo "backup failure alert could not be delivered: backup_id=$backup_id" >&2
    else
      echo "backup failure alert is not configured: backup_id=$backup_id" >&2
    fi
  fi
  exit "$status"
}
trap notify_backup_failure EXIT
trap 'exit 130' HUP INT TERM

docker exec "$POSTGRES_CONTAINER" pg_dump -Fc -U "$POSTGRES_USER" "$POSTGRES_DB" \
  > "$backup_root/postgres.dump"
[ -s "$backup_root/postgres.dump" ] || { echo "postgres dump is empty" >&2; exit 1; }

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
  echo "duration_seconds=$(($(date +%s) - started_epoch))"
  echo "postgres_database=$POSTGRES_DB"
  echo "db_publications=$(docker exec "$POSTGRES_CONTAINER" psql -At -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c 'SELECT COUNT(*) FROM publication')"
  echo "db_upload_assets=$(docker exec "$POSTGRES_CONTAINER" psql -At -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c 'SELECT COUNT(*) FROM upload_asset')"
  echo "media_bucket=$MINIO_BUCKET"
  echo "media_objects=$(find "$backup_root/media" -type f | wc -l | tr -d ' ')"
  echo "media_bytes=$(du -sk "$backup_root/media" | awk '{print $1 * 1024}')"
} > "$backup_root/manifest.txt"
(cd "$backup_root" && find . -type f ! -name manifest.sha256 -print0 | sort -z | xargs -0 sha256sum > manifest.sha256)
backup_complete=1
echo "created paired backup: $backup_root"
