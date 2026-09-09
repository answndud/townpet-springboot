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
: "${MINIO_PRESIGN_EXPIRY_SECONDS:=${TOWNPET_MINIO_PRESIGN_EXPIRY_SECONDS:-900}}"
: "${UPLOAD_QUIESCE_GRACE_SECONDS:=30}"
: "${INVENTORY_STABILITY_SECONDS:=1}"

command -v docker >/dev/null || { echo "docker is required" >&2; exit 1; }
mkdir -p "$BACKUP_DIR"
backup_id="$(date -u +%Y%m%dT%H%M%SZ)"
started_epoch="$(date +%s)"
backup_root="$BACKUP_DIR/townpet-$backup_id"
phase=initialization
mkdir -p "$backup_root/media"
backup_complete=0
maintenance_enabled=0
temp_dir="$(mktemp -d)"
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
  rm -rf -- "$temp_dir"
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

inventory_fingerprint() {
  docker exec "$MINIO_CONTAINER" mc ls --recursive --json "townpet-backup/$MINIO_BUCKET" \
    | while IFS= read -r inventory_line; do
        object_key="$(printf '%s\n' "$inventory_line" | sed -n 's/.*"key":"\([^"]*\)".*/\1/p')"
        [ -n "$object_key" ] || continue
        object_size="$(printf '%s\n' "$inventory_line" | sed -n 's/.*"size":\([0-9][0-9]*\).*/\1/p')"
        object_etag="$(printf '%s\n' "$inventory_line" | sed -n 's/.*"etag":"\([^"]*\)".*/\1/p')"
        object_version="$(printf '%s\n' "$inventory_line" | sed -n 's/.*"versionId":"\([^"]*\)".*/\1/p')"
        printf '%s\t%s\t%s\t%s\n' "$object_key" "${object_size:-unknown}" "${object_etag:-unknown}" "${object_version:-none}"
      done | sort
}

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

phase=upload_quiesce
upload_wait_seconds=0
active_assets="$(docker exec "$POSTGRES_CONTAINER" psql -At -U "$POSTGRES_USER" -d "$POSTGRES_DB" \
  -c "SELECT COUNT(*) FROM upload_asset WHERE status IN ('UPLOADING', 'READY', 'ATTACHED')")"
if [ "$active_assets" -gt 0 ]; then
  active_asset_seconds="$(docker exec "$POSTGRES_CONTAINER" psql -At -U "$POSTGRES_USER" -d "$POSTGRES_DB" \
    -c "SELECT COALESCE(CEIL(EXTRACT(EPOCH FROM (MAX(expires_at) - CURRENT_TIMESTAMP))), 0)::bigint FROM upload_asset WHERE status IN ('UPLOADING', 'READY', 'ATTACHED')")"
  upload_wait_seconds="$MINIO_PRESIGN_EXPIRY_SECONDS"
  if [ "$active_asset_seconds" -le 0 ]; then
    upload_wait_seconds=0
  elif [ "$active_asset_seconds" -lt "$upload_wait_seconds" ]; then
    upload_wait_seconds="$active_asset_seconds"
  fi
  upload_quiesce_deadline=$(($(date +%s) + upload_wait_seconds + UPLOAD_QUIESCE_GRACE_SECONDS))
  while [ "$(date +%s)" -lt "$upload_quiesce_deadline" ]; do
    sleep "$QUIESCE_POLL_SECONDS"
  done
fi

phase=minio_inventory_before
docker exec "$MINIO_CONTAINER" mc alias set townpet-backup http://127.0.0.1:9000 \
  "$MINIO_ACCESS_KEY" "$MINIO_SECRET_KEY" >/dev/null
inventory_fingerprint > "$temp_dir/media-before"
sleep "$INVENTORY_STABILITY_SECONDS"
inventory_fingerprint > "$temp_dir/media-before-stable"
if ! cmp -s "$temp_dir/media-before" "$temp_dir/media-before-stable"; then
  echo "media object inventory changed before snapshot" >&2
  exit 1
fi

phase=postgres_dump
docker exec "$POSTGRES_CONTAINER" pg_dump -Fc -U "$POSTGRES_USER" "$POSTGRES_DB" \
  > "$backup_root/postgres.dump"
[ -s "$backup_root/postgres.dump" ] || { echo "postgres dump is empty" >&2; exit 1; }

phase=minio_copy
docker exec "$MINIO_CONTAINER" sh -c "rm -rf /tmp/townpet-media-$backup_id && mkdir -p /tmp/townpet-media-$backup_id"
docker exec "$MINIO_CONTAINER" mc mirror --overwrite \
  "townpet-backup/$MINIO_BUCKET" "/tmp/townpet-media-$backup_id"
docker cp "$MINIO_CONTAINER:/tmp/townpet-media-$backup_id/." "$backup_root/media/"
docker exec "$MINIO_CONTAINER" rm -rf "/tmp/townpet-media-$backup_id"

phase=minio_inventory_after
inventory_fingerprint > "$temp_dir/media-after"
if ! cmp -s "$temp_dir/media-before-stable" "$temp_dir/media-after"; then
  echo "media object inventory changed during snapshot" >&2
  exit 1
fi

phase=reference_verify
db_keys_file="$backup_root/.db-object-keys"
db_asset_metadata_file="$temp_dir/.db-asset-metadata"
required_db_keys_file="$temp_dir/.required-db-object-keys"
uploading_db_keys_file="$temp_dir/.uploading-db-object-keys"
abandoned_keys_file="$temp_dir/abandoned-object-keys"
media_keys_file="$backup_root/.media-object-keys"
docker exec "$POSTGRES_CONTAINER" psql -At -U "$POSTGRES_USER" -d "$POSTGRES_DB" \
  -c "SELECT object_key FROM upload_asset WHERE status IN ('UPLOADING', 'READY', 'ATTACHED') ORDER BY object_key" \
  | sed '/^$/d' > "$db_keys_file"
docker exec "$POSTGRES_CONTAINER" psql -At -U "$POSTGRES_USER" -d "$POSTGRES_DB" \
  -c "SELECT object_key FROM upload_asset WHERE status IN ('READY', 'ATTACHED') ORDER BY object_key" \
  | sed '/^$/d' > "$required_db_keys_file"
docker exec "$POSTGRES_CONTAINER" psql -At -U "$POSTGRES_USER" -d "$POSTGRES_DB" \
  -c "SELECT object_key FROM upload_asset WHERE status = 'UPLOADING' ORDER BY object_key" \
  | sed '/^$/d' > "$uploading_db_keys_file"
docker exec "$POSTGRES_CONTAINER" psql -At -U "$POSTGRES_USER" -d "$POSTGRES_DB" \
  -c "SELECT concat_ws(E'\\t', object_key, checksum_sha256, byte_size, status) FROM upload_asset WHERE status IN ('UPLOADING', 'READY', 'ATTACHED') ORDER BY object_key" \
  | sed '/^$/d' > "$db_asset_metadata_file"
docker exec "$POSTGRES_CONTAINER" psql -At -U "$POSTGRES_USER" -d "$POSTGRES_DB" \
  -c "SELECT object_key FROM upload_asset WHERE status = 'ABANDONED' ORDER BY object_key" \
  | sed '/^$/d' > "$abandoned_keys_file"
(cd "$backup_root/media" && find . -type f -print | sed 's#^\./##' | sort) > "$media_keys_file"
comm -12 "$abandoned_keys_file" "$media_keys_file" | grep -q . && {
  echo "abandoned upload asset still has a media object" >&2; exit 1;
} || true
comm -23 "$required_db_keys_file" "$media_keys_file" | grep -q . && {
  echo "database references media objects missing from backup" >&2; exit 1;
} || true
comm -13 "$db_keys_file" "$media_keys_file" | grep -q . && {
  echo "backup contains media objects without a database reference" >&2; exit 1;
} || true

media_content_checked=0
missing_uploading_objects=0
while IFS="$(printf '\t')" read -r object_key expected_checksum expected_size asset_status; do
  [ -n "$object_key" ] || continue
  media_path="$backup_root/media/$object_key"
  if [ ! -f "$media_path" ]; then
    if [ "$asset_status" = "UPLOADING" ]; then
      missing_uploading_objects=$((missing_uploading_objects + 1))
      continue
    fi
    echo "required media file missing during content verification: $object_key" >&2
    exit 1
  fi
  actual_size="$(wc -c < "$media_path" | tr -d ' ')"
  actual_checksum="$(sha256sum "$media_path" | awk '{print $1}')"
  if [ "$actual_size" != "$expected_size" ] || [ "$actual_checksum" != "$expected_checksum" ]; then
    echo "media content metadata mismatch: $object_key" >&2
    exit 1
  fi
  media_content_checked=$((media_content_checked + 1))
done < "$db_asset_metadata_file"

{
  echo "execution_id=$BACKUP_EXECUTION_ID"
  echo "backup_id=$backup_id"
  echo "created_at=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
  echo "snapshot_at=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
  echo "duration_seconds=$(($(date +%s) - started_epoch))"
  echo "minio_presign_expiry_seconds=$MINIO_PRESIGN_EXPIRY_SECONDS"
  echo "upload_quiesce_grace_seconds=$UPLOAD_QUIESCE_GRACE_SECONDS"
  echo "upload_quiesce_active_assets=$active_assets"
  echo "upload_quiesce_wait_seconds=$upload_wait_seconds"
  echo "uploading_assets=$(docker exec "$POSTGRES_CONTAINER" psql -At -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "SELECT COUNT(*) FROM upload_asset WHERE status = 'UPLOADING'")"
  echo "ready_assets=$(docker exec "$POSTGRES_CONTAINER" psql -At -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "SELECT COUNT(*) FROM upload_asset WHERE status = 'READY'")"
  echo "attached_assets=$(docker exec "$POSTGRES_CONTAINER" psql -At -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "SELECT COUNT(*) FROM upload_asset WHERE status = 'ATTACHED'")"
  echo "abandoned_assets=$(docker exec "$POSTGRES_CONTAINER" psql -At -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "SELECT COUNT(*) FROM upload_asset WHERE status = 'ABANDONED'")"
  echo "flyway_version=$(docker exec "$POSTGRES_CONTAINER" psql -At -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c 'SELECT version FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 1')"
  echo "source_backend_image=$(docker inspect --format '{{.Config.Image}}' "$BACKEND_CONTAINER" 2>/dev/null || true)"
  echo "source_web_image=$(docker inspect --format '{{.Config.Image}}' townpet-web 2>/dev/null || true)"
  echo "postgres_database=$POSTGRES_DB"
  echo "db_publications=$(docker exec "$POSTGRES_CONTAINER" psql -At -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c 'SELECT COUNT(*) FROM publication')"
  echo "db_upload_assets=$(docker exec "$POSTGRES_CONTAINER" psql -At -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "SELECT COUNT(*) FROM upload_asset WHERE status IN ('UPLOADING', 'READY', 'ATTACHED')")"
  echo "media_bucket=$MINIO_BUCKET"
  echo "media_objects=$(find "$backup_root/media" -type f | wc -l | tr -d ' ')"
  echo "media_bytes=$(du -sk "$backup_root/media" | awk '{print $1 * 1024}')"
  echo "db_object_keys_sha256=$(sha256sum "$db_keys_file" | awk '{print $1}')"
  echo "media_object_keys_sha256=$(sha256sum "$media_keys_file" | awk '{print $1}')"
  echo "required_media_assets=$(wc -l < "$required_db_keys_file" | tr -d ' ')"
  echo "optional_uploading_assets=$(wc -l < "$uploading_db_keys_file" | tr -d ' ')"
  echo "missing_uploading_objects=$missing_uploading_objects"
  echo "media_content_checked=$media_content_checked"
  echo "db_asset_metadata_sha256=$(sha256sum "$db_asset_metadata_file" | awk '{print $1}')"
} > "$backup_root/manifest.txt"
rm -f "$db_keys_file" "$db_asset_metadata_file" "$required_db_keys_file" "$uploading_db_keys_file" "$media_keys_file" "$abandoned_keys_file"
phase=checksum
(cd "$backup_root" && find . -type f ! -name manifest.sha256 -print0 | sort -z | xargs -0 sha256sum > manifest.sha256)
backup_complete=1
disable_maintenance
echo "event=backup outcome=success execution_id=$BACKUP_EXECUTION_ID backup_id=$backup_id duration_seconds=$(($(date +%s) - started_epoch)) backup_root=$backup_root"
