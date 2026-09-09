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
  local temp_dir fake_bin backup_dir output exit_code

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
        *"MAX(expires_at)"*) echo 0 ;;
        *"COUNT(*) FROM upload_asset WHERE status = 'READY'"*) echo "${FIXTURE_READY_COUNT:-0}" ;;
        *"COUNT(*) FROM upload_asset WHERE status = 'ATTACHED'"*) echo "${FIXTURE_ATTACHED_COUNT:-0}" ;;
        *"COUNT(*) FROM upload_asset WHERE status = 'ABANDONED'"*) echo "${FIXTURE_ABANDONED_COUNT:-0}" ;;
        *"COUNT(*) FROM upload_asset WHERE status IN"*) echo "${FIXTURE_NON_TERMINAL_COUNT:-0}" ;;
        *"SELECT object_key FROM upload_asset WHERE status = 'ABANDONED'"*) printf '%s\n' "${FIXTURE_ABANDONED_KEYS:-}" | sort ;;
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
          if [ -n "${FIXTURE_MEDIA_KEYS_SECOND:-}" ]; then
            state_file="${FIXTURE_INVENTORY_STATE_FILE:?}"
            if [ -f "$state_file" ]; then
              keys="$FIXTURE_MEDIA_KEYS_SECOND"
            else
              : > "$state_file"
            fi
          fi
          while IFS= read -r key; do
            [ -z "$key" ] && continue
            printf '{"key":"%s"}\n' "$key"
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
    MINIO_PRESIGN_EXPIRY_SECONDS=0 \
    UPLOAD_QUIESCE_GRACE_SECONDS=0 \
    INVENTORY_STABILITY_SECONDS=0 \
    QUIESCE_POLL_SECONDS=0 \
    FIXTURE_MEDIA_KEYS="$media_keys" \
    FIXTURE_DB_KEYS="$db_keys" \
    FIXTURE_ABANDONED_KEYS="$abandoned_keys" \
    FIXTURE_UPLOADING_COUNT=1 \
    FIXTURE_READY_COUNT=1 \
    FIXTURE_ATTACHED_COUNT=1 \
    FIXTURE_ABANDONED_COUNT=1 \
    FIXTURE_NON_TERMINAL_COUNT=3 \
    FIXTURE_MEDIA_KEYS_SECOND="$media_keys_second" \
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
    grep -q '^uploading_assets=1$' "$manifest"
    grep -q '^db_upload_assets=3$' "$manifest"
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
