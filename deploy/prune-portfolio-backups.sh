#!/usr/bin/env sh
set -eu
umask 077

: "${BACKUP_DIR:=/opt/backups}"
: "${ENCRYPTED_DIR:=$BACKUP_DIR/encrypted}"
: "${OFFSITE_DIR:=$BACKUP_DIR/offsite-copy}"
: "${RETENTION_DAYS:=28}"
: "${MIN_KEEP_COUNT:=7}"
: "${DRY_RUN:=0}"

case "$BACKUP_DIR" in
  /opt/backups|/opt/backups/*) ;;
  *) echo "refusing unexpected BACKUP_DIR: $BACKUP_DIR" >&2; exit 1 ;;
esac
case "$RETENTION_DAYS" in ''|*[!0-9]*) echo "RETENTION_DAYS must be numeric" >&2; exit 1 ;; esac
case "$MIN_KEEP_COUNT" in ''|*[!0-9]*) echo "MIN_KEEP_COUNT must be numeric" >&2; exit 1 ;; esac

command -v find >/dev/null || { echo "find is required" >&2; exit 1; }

remove_path() {
  path="$1"
  if [ "$DRY_RUN" = "1" ]; then
    echo "would_remove=$path"
  else
    rm -rf -- "$path"
    echo "removed=$path"
  fi
}

prune_directories() {
  count="$(find "$BACKUP_DIR" -mindepth 1 -maxdepth 1 -type d -name 'townpet-20*' -print 2>/dev/null | wc -l | tr -d ' ')"
  [ "$count" -gt "$MIN_KEEP_COUNT" ] || return 0
  find "$BACKUP_DIR" -mindepth 1 -maxdepth 1 -type d -name 'townpet-20*' -print 2>/dev/null |
    while IFS= read -r path; do
      [ -n "$path" ] || continue
      if [ "$count" -gt "$MIN_KEEP_COUNT" ] && find "$path" -prune -type d -mtime "+$RETENTION_DAYS" -print | grep -q .; then
        remove_path "$path"
        count=$((count - 1))
      fi
    done
}

prune_files() {
  directory="$1"
  [ -d "$directory" ] || return 0
  count="$(find "$directory" -mindepth 1 -maxdepth 1 -type f -name 'townpet-*.tar.gz.age' -print 2>/dev/null | wc -l | tr -d ' ')"
  [ "$count" -gt "$MIN_KEEP_COUNT" ] || return 0
  find "$directory" -mindepth 1 -maxdepth 1 -type f -name 'townpet-*.tar.gz.age' -mtime "+$RETENTION_DAYS" -print 2>/dev/null |
    while IFS= read -r path; do
      [ -n "$path" ] || continue
      if [ "$count" -gt "$MIN_KEEP_COUNT" ]; then
        remove_path "$path"
        count=$((count - 1))
      fi
    done
}

prune_directories
prune_files "$ENCRYPTED_DIR"
prune_files "$OFFSITE_DIR"
echo "backup_retention retention_days=$RETENTION_DAYS min_keep_count=$MIN_KEEP_COUNT dry_run=$DRY_RUN"
