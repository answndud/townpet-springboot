#!/usr/bin/env bash
set -Eeuo pipefail

# Contract-level disposable fixture for backup/restore reference policy.
# It does not touch Docker volumes or a real database; the fake docker command
# exposes the same psql/mc/dump/cp calls used by backup-portfolio.sh.

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKUP_SCRIPT="$ROOT_DIR/deploy/backup-portfolio.sh"

run_case() {
  local name="$1"
  local expected="$2"
  local media_keys="$3"
  local db_keys="$4"
  local abandoned_keys="$5"
  local media_keys_second="${6:-}"
  local inventory_change_call="${7:-}"
  local fixture_uploading_count="${8:-1}"
  local fixture_active_seconds="${9:-0}"
  local fixture_presign_expiry="${10:-0}"
  local fixture_media_etag="${11:-fixture-etag}"
  local fixture_media_etag_second="${12:-}"
  local fixture_media_size="${13:-8}"
  local fixture_media_size_second="${14:-}"
  local fixture_expected_checksum="${15:-e80b71cd14d3cbd65f4173abcbfcf01a545dbca32a72d575108b553a648cc96f}"
  local fixture_expected_size="${16:-8}"
  local fixture_uploading_keys=""
  local fixture_non_terminal_count=2
  local temp_dir fake_bin backup_dir output exit_code

  if [ "$fixture_uploading_count" -gt 0 ]; then
    fixture_uploading_keys="uploads/uploading"
    fixture_non_terminal_count=3
  fi

  temp_dir="$(mktemp -d)"
  fake_bin="$temp_dir/bin"
  backup_dir="$temp_dir/backups"
  mkdir -p "$fake_bin"
  cat > "$fake_bin/docker" <<'FAKE_DOCKER'
#!/usr/bin/env bash
set -Eeuo pipefail

if [ "$1" = "exec" ]; then
  container="$2"
  shift 2
  command="$1"
  shift
  case "$container:$command" in
    townpet-backend:wget)
      printf '%s\n' '{"acceptingWrites":false,"activeWrites":0}'
      ;;
    townpet-backend:touch|townpet-backend:rm)
      ;;
    townpet-postgres:psql)
      sql=""
      for argument in "$@"; do
        case "$argument" in
          SELECT*|select*) sql="$argument" ;;
        esac
      done
      case "$sql" in
        *"COUNT(*) FROM upload_asset WHERE status = 'UPLOADING'"*) echo "${FIXTURE_UPLOADING_COUNT:-0}" ;;
        *"MAX(expires_at)"*) echo "${FIXTURE_ACTIVE_SECONDS:-0}" ;;
        *"COUNT(*) FROM upload_asset WHERE status = 'READY'"*) echo "${FIXTURE_READY_COUNT:-0}" ;;
        *"COUNT(*) FROM upload_asset WHERE status = 'ATTACHED'"*) echo "${FIXTURE_ATTACHED_COUNT:-0}" ;;
        *"COUNT(*) FROM upload_asset WHERE status = 'ABANDONED'"*) echo "${FIXTURE_ABANDONED_COUNT:-0}" ;;
        *"COUNT(*) FROM upload_asset WHERE status IN ('READY', 'ATTACHED')"*) echo "${FIXTURE_REQUIRED_COUNT:-0}" ;;
        *"COUNT(*) FROM upload_asset WHERE status IN"*) echo "${FIXTURE_NON_TERMINAL_COUNT:-0}" ;;
        *"SELECT object_key FROM upload_asset WHERE status = 'ABANDONED'"*) printf '%s\n' "${FIXTURE_ABANDONED_KEYS:-}" | sort ;;
        *"SELECT object_key FROM upload_asset WHERE status = 'UPLOADING'"*) printf '%s\n' "${FIXTURE_UPLOADING_KEYS:-}" | sort ;;
        *"SELECT object_key FROM upload_asset WHERE status IN ('READY', 'ATTACHED')"*) printf '%s\n' "${FIXTURE_REQUIRED_KEYS:-}" | sort ;;
        *"checksum_sha256"*)
          while IFS= read -r key; do
            [ -z "$key" ] && continue
            status=READY
            case "$key" in
              */uploading) status=UPLOADING ;;
            esac
            printf '%s\t%s\t%s\t%s\n' "$key" "${FIXTURE_EXPECTED_CHECKSUM:-e80b71cd14d3cbd65f4173abcbfcf01a545dbca32a72d575108b553a648cc96f}" "${FIXTURE_EXPECTED_SIZE:-8}" "$status"
          done <<< "${FIXTURE_DB_KEYS:-}"
          ;;
        *"SELECT object_key FROM upload_asset WHERE status IN"*) printf '%s\n' "${FIXTURE_DB_KEYS:-}" | sort ;;
        *"COUNT(*) FROM publication"*) echo 0 ;;
        *"version FROM flyway_schema_history"*) echo V066 ;;
        *) echo 0 ;;
      esac
      ;;
    townpet-postgres:pg_dump)
      printf '%s\n' disposable-dump
      ;;
    townpet-minio:mc)
      case "$*" in
        *"ls --recursive --json"*)
          keys="${FIXTURE_MEDIA_KEYS:-}"
          state_file="${FIXTURE_INVENTORY_STATE_FILE:?}"
          inventory_call=0
          if [ -f "$state_file" ]; then
            inventory_call="$(cat "$state_file")"
          fi
          inventory_call=$((inventory_call + 1))
          printf '%s\n' "$inventory_call" > "$state_file"
          change_call="${FIXTURE_INVENTORY_CHANGE_CALL:-}"
          if [ -z "$change_call" ] && [ -n "${FIXTURE_MEDIA_KEYS_SECOND:-}" ]; then
            change_call=2
          fi
          etag="${FIXTURE_MEDIA_ETAG:-fixture-etag}"
          size="${FIXTURE_MEDIA_SIZE:-8}"
          if [ -n "$change_call" ] && [ "$inventory_call" -ge "$change_call" ]; then
            keys="${FIXTURE_MEDIA_KEYS_SECOND:-}"
            if [ -n "${FIXTURE_MEDIA_ETAG_SECOND:-}" ]; then
              etag="$FIXTURE_MEDIA_ETAG_SECOND"
            fi
            if [ -n "${FIXTURE_MEDIA_SIZE_SECOND:-}" ]; then
              size="$FIXTURE_MEDIA_SIZE_SECOND"
            fi
          else
            etag="${FIXTURE_MEDIA_ETAG:-fixture-etag}"
            size="${FIXTURE_MEDIA_SIZE:-8}"
          fi
          while IFS= read -r key; do
            [ -z "$key" ] && continue
            printf '{"key":"%s","size":%s,"etag":"%s"}\n' "$key" "$size" "$etag"
          done <<< "$keys"
          ;;
        *) ;;
      esac
      ;;
    townpet-minio:sh|townpet-minio:rm)
      ;;
  esac
elif [ "$1" = "cp" ]; then
  destination="${@: -1}"
  mkdir -p "$destination"
  while IFS= read -r key; do
    [ -z "$key" ] && continue
    mkdir -p "$destination/$(dirname "$key")"
    printf 'fixture\n' > "$destination/$key"
  done <<< "${FIXTURE_MEDIA_KEYS:-}"
elif [ "$1" = "inspect" ]; then
  printf '%s\n' townpet-fixture-image
fi
FAKE_DOCKER
  chmod +x "$fake_bin/docker"

  set +e
  output="$({
    PATH="$fake_bin:$PATH" \
    POSTGRES_CONTAINER=townpet-postgres \
    MINIO_CONTAINER=townpet-minio \
    BACKEND_CONTAINER=townpet-backend \
    POSTGRES_USER=fixture \
    POSTGRES_DB=townpet \
    MINIO_ACCESS_KEY=fixture \
    MINIO_SECRET_KEY=fixture \
    MINIO_BUCKET=townpet-media \
    BACKUP_DIR="$backup_dir" \
    MINIO_PRESIGN_EXPIRY_SECONDS="$fixture_presign_expiry" \
    UPLOAD_QUIESCE_GRACE_SECONDS=0 \
    INVENTORY_STABILITY_SECONDS=0 \
    QUIESCE_POLL_SECONDS=0 \
    FIXTURE_MEDIA_KEYS="$media_keys" \
    FIXTURE_DB_KEYS="$db_keys" \
    FIXTURE_ABANDONED_KEYS="$abandoned_keys" \
    FIXTURE_UPLOADING_COUNT="$fixture_uploading_count" \
    FIXTURE_READY_COUNT=1 \
    FIXTURE_ATTACHED_COUNT=1 \
    FIXTURE_ABANDONED_COUNT=1 \
    FIXTURE_NON_TERMINAL_COUNT="$fixture_non_terminal_count" \
    FIXTURE_REQUIRED_COUNT=2 \
    FIXTURE_ACTIVE_SECONDS="$fixture_active_seconds" \
    FIXTURE_UPLOADING_KEYS="$fixture_uploading_keys" \
    FIXTURE_REQUIRED_KEYS=$'uploads/ready\nuploads/attached' \
    FIXTURE_MEDIA_KEYS_SECOND="$media_keys_second" \
    FIXTURE_MEDIA_ETAG="$fixture_media_etag" \
    FIXTURE_MEDIA_ETAG_SECOND="$fixture_media_etag_second" \
    FIXTURE_MEDIA_SIZE="$fixture_media_size" \
    FIXTURE_MEDIA_SIZE_SECOND="$fixture_media_size_second" \
    FIXTURE_EXPECTED_CHECKSUM="$fixture_expected_checksum" \
    FIXTURE_EXPECTED_SIZE="$fixture_expected_size" \
    FIXTURE_INVENTORY_CHANGE_CALL="$inventory_change_call" \
    FIXTURE_INVENTORY_STATE_FILE="$temp_dir/inventory-state" \
    "$BACKUP_SCRIPT"
  } 2>&1)"
  exit_code=$?
  set -e

  if [ "$expected" = "success" ]; then
    [ "$exit_code" -eq 0 ] || {
      echo "[$name] expected success, got exit=$exit_code" >&2
      echo "$output" >&2
      exit 1
    }
    manifest="$(find "$backup_dir" -name manifest.txt -print -quit)"
    grep -q '^ready_assets=1$' "$manifest"
    grep -q '^attached_assets=1$' "$manifest"
    if [ "$fixture_uploading_count" -gt 0 ]; then
      grep -q '^uploading_assets=1$' "$manifest"
    else
      grep -q '^uploading_assets=0$' "$manifest"
    fi
    if [ "$fixture_presign_expiry" -gt 0 ]; then
      grep -q "^upload_quiesce_wait_seconds=$fixture_presign_expiry$" "$manifest"
    fi
    grep -q "^db_upload_assets=$fixture_non_terminal_count$" "$manifest"
  else
    [ "$exit_code" -ne 0 ] || {
      echo "[$name] expected failure" >&2
      echo "$output" >&2
      exit 1
    }
  fi
  rm -rf "$temp_dir"
  echo "backup reference policy: $name=$expected"
}

run_case \
  ready-is-preserved \
  success \
  $'uploads/ready\nuploads/attached\nuploads/uploading' \
  $'uploads/ready\nuploads/attached\nuploads/uploading' \
  abandoned/object
run_case \
  missing-object \
  failure \
  uploads/attached \
  $'uploads/ready\nuploads/attached\nuploads/uploading' \
  abandoned/object
run_case \
  orphan-object \
  failure \
  $'uploads/ready\nuploads/attached\nuploads/uploading\norphan/object' \
  $'uploads/ready\nuploads/attached\nuploads/uploading' \
  abandoned/object
run_case \
  abandoned-object \
  failure \
  $'uploads/ready\nuploads/attached\nuploads/uploading\nabandoned/object' \
  $'uploads/ready\nuploads/attached\nuploads/uploading' \
  abandoned/object
run_case \
  inventory-changed \
  failure \
  $'uploads/ready\nuploads/attached\nuploads/uploading' \
  $'uploads/ready\nuploads/attached\nuploads/uploading' \
  abandoned/object \
  $'uploads/ready\nuploads/attached\nuploads/uploading\nlate/object'

run_case \
  inventory-changed-during-snapshot \
  failure \
  $'uploads/ready\nuploads/attached\nuploads/uploading' \
  $'uploads/ready\nuploads/attached\nuploads/uploading' \
  abandoned/object \
  $'uploads/ready\nuploads/attached\nuploads/uploading\nlate/object' \
  3

run_case \
  uploading-object-missing-is-allowed \
  success \
  $'uploads/ready\nuploads/attached' \
  $'uploads/ready\nuploads/attached\nuploads/uploading' \
  abandoned/object \
  "" \
  "" \
  1 \
  0

run_case \
  ready-object-missing-is-rejected \
  failure \
  $'uploads/attached' \
  $'uploads/ready\nuploads/attached\nuploads/uploading' \
  abandoned/object \
  "" \
  "" \
  1 \
  0

run_case \
  presigned-form-wait-applies-without-uploading-row \
  success \
  $'uploads/ready\nuploads/attached' \
  $'uploads/ready\nuploads/attached' \
  abandoned/object \
  "" \
  "" \
  0 \
  100 \
  1

run_case \
  inventory-etag-changed-for-same-key \
  failure \
  uploads/ready \
  uploads/ready \
  abandoned/object \
  uploads/ready \
  "" \
  0 \
  0 \
  0 \
  fixture-etag \
  changed-etag

run_case \
  inventory-size-changed-during-snapshot \
  failure \
  uploads/ready \
  uploads/ready \
  abandoned/object \
  uploads/ready \
  3 \
  0 \
  0 \
  0 \
  fixture-etag \
  fixture-etag \
  8 \
  9

run_case \
  inventory-order-only-change-is-allowed \
  success \
  $'uploads/ready\nuploads/attached' \
  $'uploads/ready\nuploads/attached' \
  abandoned/object \
  $'uploads/attached\nuploads/ready' \
  "" \
  0 \
  0 \
  0

run_case \
  media-checksum-mismatch-is-rejected \
  failure \
  uploads/ready \
  uploads/ready \
  abandoned/object \
  "" \
  "" \
  0 \
  0 \
  0 \
  fixture-etag \
  "" \
  8 \
  "" \
  deadbeef

run_case \
  media-size-mismatch-is-rejected \
  failure \
  uploads/ready \
  uploads/ready \
  abandoned/object \
  "" \
  "" \
  0 \
  0 \
  0 \
  fixture-etag \
  "" \
  8 \
  "" \
  e80b71cd14d3cbd65f4173abcbfcf01a545dbca32a72d575108b553a648cc96f \
  9
