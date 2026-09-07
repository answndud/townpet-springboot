#!/usr/bin/env sh
set -eu
umask 077

: "${POSTGRES_CONTAINER:?set POSTGRES_CONTAINER}"
: "${MINIO_CONTAINER:?set MINIO_CONTAINER}"
: "${BACKEND_CONTAINER:=townpet-backend}"
: "${POSTGRES_USER:?set POSTGRES_USER}"
: "${POSTGRES_DB:?set POSTGRES_DB}"
: "${MINIO_ACCESS_KEY:?set MINIO_ACCESS_KEY}"
: "${MINIO_SECRET_KEY:?set MINIO_SECRET_KEY}"
: "${MINIO_BUCKET:=townpet-media}"
: "${BACKUP_DIR:=./backups}"
: "${BACKUP_ALERT_WEBHOOK_URL:=}"
: "${BACKUP_EXECUTION_ID:=$(date -u +%Y%m%dT%H%M%SZ)}"
: "${MAINTENANCE_FILE:=/tmp/townpet-maintenance}"
: "${QUIESCE_TIMEOUT_SECONDS:=60}"
: "${QUIESCE_POLL_SECONDS:=1}"

command -v docker >/dev/null || { echo "docker is required" >&2; exit 1; }
mkdir -p "$BACKUP_DIR"
backup_id="$(date -u +%Y%m%dT%H%M%SZ)"
started_epoch="$(date +%s)"
backup_root="$BACKUP_DIR/townpet-$backup_id"
phase=initialization
mkdir -p "$backup_root/media"
backup_complete=0
maintenance_enabled=0
disable_maintenance() {
  if [ "$maintenance_enabled" -eq 1 ]; then
    docker exec "$BACKEND_CONTAINER" rm -f "$MAINTENANCE_FILE" >/dev/null 2>&1 || true
    maintenance_enabled=0
  fi
}
cleanup_failed_backup() {
  if [ "$backup_complete" -ne 1 ] && [ -n "$backup_root" ] && [ -d "$backup_root" ]; then
    rm -rf -- "$backup_root"
  fi
}
notify_backup_failure() {
  status="$?"
  disable_maintenance
  if [ "$status" -ne 0 ] && [ "$backup_complete" -ne 1 ]; then
    cleanup_failed_backup
    if [ -n "$BACKUP_ALERT_WEBHOOK_URL" ] && command -v curl >/dev/null 2>&1; then
      curl --fail --silent --show-error --max-time 10 \
        -X POST \
        -H 'Content-Type: application/json' \
        --data "{\"event\":\"townpet_backup_failed\",\"execution_id\":\"$BACKUP_EXECUTION_ID\",\"backup_id\":\"$backup_id\",\"phase\":\"$phase\",\"exit_code\":$status}" \
        "$BACKUP_ALERT_WEBHOOK_URL" >/dev/null ||
        echo "event=backup_alert outcome=failure execution_id=$BACKUP_EXECUTION_ID backup_id=$backup_id" >&2
      echo "event=backup outcome=failure execution_id=$BACKUP_EXECUTION_ID backup_id=$backup_id phase=$phase exit_code=$status alert=attempted" >&2
    else
      echo "event=backup outcome=failure execution_id=$BACKUP_EXECUTION_ID backup_id=$backup_id phase=$phase exit_code=$status alert=not_configured" >&2
    fi
  fi
  exit "$status"
}
trap notify_backup_failure EXIT
trap 'exit 130' HUP INT TERM

phase=quiesce
docker exec "$BACKEND_CONTAINER" touch "$MAINTENANCE_FILE"
maintenance_enabled=1
quiesce_deadline=$(($(date +%s) + QUIESCE_TIMEOUT_SECONDS))
quiesce_state=""
while [ "$(date +%s)" -lt "$quiesce_deadline" ]; do
  quiesce_state="$(docker exec "$BACKEND_CONTAINER" wget -qO- http://127.0.0.1:8080/api/health 2>/dev/null || true)"
  case "$quiesce_state" in
    *'"acceptingWrites":false'*)
      case "$quiesce_state" in *'"activeWrites":0'*) break ;; esac
      ;;
  esac
  sleep "$QUIESCE_POLL_SECONDS"
done
case "$quiesce_state" in
  *'"acceptingWrites":false'*)
    case "$quiesce_state" in *'"activeWrites":0'*) ;; *) echo "write quiesce was not confirmed before timeout" >&2; exit 1 ;; esac
    ;;
  *) echo "write quiesce was not confirmed before timeout" >&2; exit 1 ;;
esac

phase=postgres_dump
docker exec "$POSTGRES_CONTAINER" pg_dump -Fc -U "$POSTGRES_USER" "$POSTGRES_DB" \
  > "$backup_root/postgres.dump"
[ -s "$backup_root/postgres.dump" ] || { echo "postgres dump is empty" >&2; exit 1; }

phase=minio_copy
docker exec "$MINIO_CONTAINER" mc alias set townpet-backup http://127.0.0.1:9000 \
  "$MINIO_ACCESS_KEY" "$MINIO_SECRET_KEY" >/dev/null
docker exec "$MINIO_CONTAINER" sh -c "rm -rf /tmp/townpet-media-$backup_id && mkdir -p /tmp/townpet-media-$backup_id"
docker exec "$MINIO_CONTAINER" mc mirror --overwrite \
  "townpet-backup/$MINIO_BUCKET" "/tmp/townpet-media-$backup_id"
docker cp "$MINIO_CONTAINER:/tmp/townpet-media-$backup_id/." "$backup_root/media/"
docker exec "$MINIO_CONTAINER" rm -rf "/tmp/townpet-media-$backup_id"

phase=reference_verify
db_keys_file="$backup_root/.db-object-keys"
media_keys_file="$backup_root/.media-object-keys"
docker exec "$POSTGRES_CONTAINER" psql -At -U "$POSTGRES_USER" -d "$POSTGRES_DB" \
  -c 'SELECT object_key FROM upload_asset WHERE publication_id IS NOT NULL ORDER BY object_key' | sed '/^$/d' > "$db_keys_file"
(cd "$backup_root/media" && find . -type f -printf '%P\n' | sort) > "$media_keys_file"
comm -23 "$db_keys_file" "$media_keys_file" | grep -q . && {
  echo "database references media objects missing from backup" >&2; exit 1;
} || true
comm -13 "$db_keys_file" "$media_keys_file" | grep -q . && {
  echo "backup contains media objects without a database reference" >&2; exit 1;
} || true

{
  echo "execution_id=$BACKUP_EXECUTION_ID"
  echo "backup_id=$backup_id"
  echo "created_at=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
  echo "snapshot_at=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
  echo "duration_seconds=$(($(date +%s) - started_epoch))"
  echo "flyway_version=$(docker exec "$POSTGRES_CONTAINER" psql -At -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c 'SELECT version FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 1')"
  echo "source_backend_image=$(docker inspect --format '{{.Config.Image}}' "$BACKEND_CONTAINER" 2>/dev/null || true)"
  echo "source_web_image=$(docker inspect --format '{{.Config.Image}}' townpet-web 2>/dev/null || true)"
  echo "postgres_database=$POSTGRES_DB"
  echo "db_publications=$(docker exec "$POSTGRES_CONTAINER" psql -At -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c 'SELECT COUNT(*) FROM publication')"
  echo "db_upload_assets=$(docker exec "$POSTGRES_CONTAINER" psql -At -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c 'SELECT COUNT(*) FROM upload_asset WHERE publication_id IS NOT NULL')"
  echo "media_bucket=$MINIO_BUCKET"
  echo "media_objects=$(find "$backup_root/media" -type f | wc -l | tr -d ' ')"
  echo "media_bytes=$(du -sk "$backup_root/media" | awk '{print $1 * 1024}')"
  echo "db_object_keys_sha256=$(sha256sum "$db_keys_file" | awk '{print $1}')"
  echo "media_object_keys_sha256=$(sha256sum "$media_keys_file" | awk '{print $1}')"
} > "$backup_root/manifest.txt"
rm -f "$db_keys_file" "$media_keys_file"
phase=checksum
(cd "$backup_root" && find . -type f ! -name manifest.sha256 -print0 | sort -z | xargs -0 sha256sum > manifest.sha256)
backup_complete=1
disable_maintenance
echo "event=backup outcome=success execution_id=$BACKUP_EXECUTION_ID backup_id=$backup_id duration_seconds=$(($(date +%s) - started_epoch)) backup_root=$backup_root"
