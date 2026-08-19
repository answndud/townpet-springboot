#!/usr/bin/env sh
set -eu
umask 077

: "$1"
: "$2"
backup_root="$1"
output_file="$2"
: "${AGE_RECIPIENT:?set AGE_RECIPIENT}"
: "${BACKUP_OFFSITE_COPY_DIR:=}"

[ -d "$backup_root" ] || { echo "backup directory is missing: $backup_root" >&2; exit 1; }
command -v age >/dev/null || { echo "age is required" >&2; exit 1; }
case "$output_file" in
  /*) ;;
  *) echo "output file must be an absolute path" >&2; exit 1 ;;
esac
[ ! -e "$output_file" ] || { echo "refusing to overwrite existing output" >&2; exit 1; }

parent_dir=$(dirname "$backup_root")
backup_name=$(basename "$backup_root")
tar -C "$parent_dir" -czf - "$backup_name" | age -r "$AGE_RECIPIENT" -o "$output_file"
chmod 600 "$output_file"
if [ -n "$BACKUP_OFFSITE_COPY_DIR" ]; then
  mkdir -p "$BACKUP_OFFSITE_COPY_DIR"
  offsite_file="$BACKUP_OFFSITE_COPY_DIR/$(basename "$output_file")"
  [ ! -e "$offsite_file" ] || {
    echo "refusing to overwrite existing offsite backup" >&2
    exit 1
  }
  cp "$output_file" "$offsite_file"
  chmod 600 "$offsite_file"
  echo "encrypted backup copied to configured offsite path"
fi
echo "encrypted backup created: $output_file"
