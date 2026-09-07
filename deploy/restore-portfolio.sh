#!/usr/bin/env sh
set -eu

: "${BACKUP_ROOT:?set BACKUP_ROOT to a paired backup directory}"
: "${POSTGRES_CONTAINER:?set POSTGRES_CONTAINER}"
: "${MINIO_CONTAINER:?set MINIO_CONTAINER}"
: "${POSTGRES_USER:?set POSTGRES_USER}"
: "${POSTGRES_DB:?set POSTGRES_DB}"
: "${APP_DB_USER:=$POSTGRES_USER}"
: "${MINIO_ACCESS_KEY:?set MINIO_ACCESS_KEY}"
: "${MINIO_SECRET_KEY:?set MINIO_SECRET_KEY}"
: "${MINIO_RESTORE_ACCESS_KEY:=$MINIO_ACCESS_KEY}"
: "${MINIO_RESTORE_SECRET_KEY:=$MINIO_SECRET_KEY}"
: "${MINIO_BUCKET:=townpet-media}"
: "${ALLOW_DESTRUCTIVE_RESTORE:?set ALLOW_DESTRUCTIVE_RESTORE=YES}"
: "${RESTORE_EXECUTION_ID:=$(date -u +%Y%m%dT%H%M%SZ)}"
: "${RESTORE_HEALTH_URL:=}"
: "${RESTORE_API_URL:=}"
: "${RESTORE_MEDIA_URL:=}"

command -v docker >/dev/null || { echo "docker is required" >&2; exit 1; }
started_epoch="$(date +%s)"
phase=validate
temp_dir="$(mktemp -d)"
on_exit() {
  status="$?"
  if [ "$status" -eq 0 ]; then
    echo "event=restore outcome=success execution_id=$RESTORE_EXECUTION_ID backup_root=$BACKUP_ROOT duration_seconds=$(($(date +%s) - started_epoch))"
  else
    echo "event=restore outcome=failure execution_id=$RESTORE_EXECUTION_ID backup_root=$BACKUP_ROOT phase=$phase exit_code=$status" >&2
  fi
  rm -rf -- "$temp_dir"
  exit "$status"
}
trap on_exit EXIT

if [ "$ALLOW_DESTRUCTIVE_RESTORE" != "YES" ]; then
  echo "refusing paired restore: set ALLOW_DESTRUCTIVE_RESTORE=YES" >&2
  exit 1
fi
[ -f "$BACKUP_ROOT/postgres.dump" ] || { echo "missing postgres.dump" >&2; exit 1; }
[ -d "$BACKUP_ROOT/media" ] || { echo "missing media directory" >&2; exit 1; }
[ -f "$BACKUP_ROOT/manifest.sha256" ] || { echo "missing manifest.sha256" >&2; exit 1; }
(cd "$BACKUP_ROOT" && sha256sum -c manifest.sha256)

phase=postgres_restore
docker exec -i "$POSTGRES_CONTAINER" pg_restore \
  --clean --if-exists --no-owner --exit-on-error \
  -U "$POSTGRES_USER" -d "$POSTGRES_DB" < "$BACKUP_ROOT/postgres.dump"

# A dump restored with --no-owner does not reliably preserve the runtime role's
# grants. Reapply the least required application grants before API verification.
docker exec -i "$POSTGRES_CONTAINER" psql -v ON_ERROR_STOP=1 \
  -U "$POSTGRES_USER" -d "$POSTGRES_DB" \
  -v app_user="$APP_DB_USER" <<'SQL'
SELECT format('GRANT USAGE ON SCHEMA public TO %I', :'app_user') \gexec
SELECT format('GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO %I', :'app_user') \gexec
SELECT format('GRANT USAGE, SELECT, UPDATE ON ALL SEQUENCES IN SCHEMA public TO %I', :'app_user') \gexec
SQL

phase=reference_verify
db_keys_file="$temp_dir/db-object-keys"
abandoned_keys_file="$temp_dir/abandoned-object-keys"
backup_keys_file="$temp_dir/backup-object-keys"
docker exec "$POSTGRES_CONTAINER" psql -At -U "$POSTGRES_USER" -d "$POSTGRES_DB" \
  -c "SELECT object_key FROM upload_asset WHERE status IN ('UPLOADING', 'READY', 'ATTACHED') ORDER BY object_key" \
  | sed '/^$/d' > "$db_keys_file"
docker exec "$POSTGRES_CONTAINER" psql -At -U "$POSTGRES_USER" -d "$POSTGRES_DB" \
  -c "SELECT object_key FROM upload_asset WHERE status = 'ABANDONED' ORDER BY object_key" \
  | sed '/^$/d' > "$abandoned_keys_file"
(cd "$BACKUP_ROOT/media" && find . -type f -print | sed 's#^\./##' | sort) > "$backup_keys_file"
comm -12 "$abandoned_keys_file" "$backup_keys_file" | grep -q . && {
  echo "abandoned upload asset still has a media object" >&2; exit 1;
} || true
comm -23 "$db_keys_file" "$backup_keys_file" | grep -q . && {
  echo "database references media objects missing from backup" >&2; exit 1;
} || true
comm -13 "$db_keys_file" "$backup_keys_file" | grep -q . && {
  echo "backup contains media objects without a database reference" >&2; exit 1;
} || true
expected_keys_hash="$(sed -n 's/^db_object_keys_sha256=//p' "$BACKUP_ROOT/manifest.txt")"
[ -z "$expected_keys_hash" ] || [ "$expected_keys_hash" = "$(sha256sum "$db_keys_file" | awk '{print $1}')" ] || {
  echo "database object-key checksum mismatch" >&2; exit 1;
}

restore_id="$(date -u +%Y%m%dT%H%M%SZ)"
phase=minio_restore
docker exec "$MINIO_CONTAINER" mc alias set townpet-restore http://127.0.0.1:9000 \
  "$MINIO_RESTORE_ACCESS_KEY" "$MINIO_RESTORE_SECRET_KEY" >/dev/null
docker exec "$MINIO_CONTAINER" mc mb --ignore-existing "townpet-restore/$MINIO_BUCKET" >/dev/null
docker exec "$MINIO_CONTAINER" mkdir -p "/tmp/townpet-media-restore-$restore_id"
docker cp "$BACKUP_ROOT/media/." "$MINIO_CONTAINER:/tmp/townpet-media-restore-$restore_id/"
docker exec "$MINIO_CONTAINER" mc mirror --overwrite --remove \
  "/tmp/townpet-media-restore-$restore_id" "townpet-restore/$MINIO_BUCKET"
docker exec "$MINIO_CONTAINER" rm -rf "/tmp/townpet-media-restore-$restore_id"

expected_media_objects="$(find "$BACKUP_ROOT/media" -type f | wc -l | tr -d ' ')"
phase=media_verify
restored_object_listing="$(docker exec "$MINIO_CONTAINER" mc ls --recursive --json "townpet-restore/$MINIO_BUCKET" \
  | sed -n 's/.*"key":"\([^"]*\)".*/\1/p')"
restored_media_objects="$(printf '%s\n' "$restored_object_listing" | sed '/^$/d' | wc -l | tr -d ' ')"
[ "$expected_media_objects" = "$restored_media_objects" ] || {
  echo "restored media object count mismatch: expected=$expected_media_objects actual=$restored_media_objects" >&2
  exit 1
}

phase=media_reference_verify
while IFS= read -r object_key; do
  [ -z "$object_key" ] && continue
  docker exec "$MINIO_CONTAINER" mc stat "townpet-restore/$MINIO_BUCKET/$object_key" >/dev/null
done < "$db_keys_file"

manifest_publications="$(sed -n 's/^db_publications=//p' "$BACKUP_ROOT/manifest.txt")"
if [ -n "$manifest_publications" ]; then
  phase=database_verify
  restored_publications="$(docker exec "$POSTGRES_CONTAINER" psql -At -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c 'SELECT COUNT(*) FROM publication')"
  [ "$manifest_publications" = "$restored_publications" ] || {
    echo "restored publication count mismatch: expected=$manifest_publications actual=$restored_publications" >&2
    exit 1
  }
fi

manifest_upload_assets="$(sed -n 's/^db_upload_assets=//p' "$BACKUP_ROOT/manifest.txt")"
if [ -n "$manifest_upload_assets" ]; then
  restored_upload_assets="$(docker exec "$POSTGRES_CONTAINER" psql -At -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "SELECT COUNT(*) FROM upload_asset WHERE status IN ('UPLOADING', 'READY', 'ATTACHED')")"
  [ "$manifest_upload_assets" = "$restored_upload_assets" ] || {
    echo "restored upload asset count mismatch: expected=$manifest_upload_assets actual=$restored_upload_assets" >&2
    exit 1
  }
fi
for asset_status in UPLOADING READY ATTACHED ABANDONED; do
  manifest_status_key="$(printf '%s' "$asset_status" | tr '[:upper:]' '[:lower:]')_assets"
  manifest_status_count="$(sed -n "s/^${manifest_status_key}=//p" "$BACKUP_ROOT/manifest.txt")"
  if [ -n "$manifest_status_count" ]; then
    restored_status_count="$(docker exec "$POSTGRES_CONTAINER" psql -At -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "SELECT COUNT(*) FROM upload_asset WHERE status = '$asset_status'")"
    [ "$manifest_status_count" = "$restored_status_count" ] || {
      echo "restored upload asset status mismatch: status=$asset_status expected=$manifest_status_count actual=$restored_status_count" >&2
      exit 1
    }
  fi
done
if [ -n "$RESTORE_HEALTH_URL" ]; then
  phase=application_verify
  curl --fail --silent --show-error --location --max-time 15 "$RESTORE_HEALTH_URL" >/dev/null
fi
if [ -n "$RESTORE_API_URL" ]; then
  curl --fail --silent --show-error --location --max-time 15 "$RESTORE_API_URL" >/dev/null
fi
if [ -n "$RESTORE_MEDIA_URL" ]; then
  curl --fail --silent --show-error --location --max-time 15 "$RESTORE_MEDIA_URL" >/dev/null
fi
echo "restored paired backup: $BACKUP_ROOT"
