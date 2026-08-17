#!/usr/bin/env sh
set -eu

: "${1:?usage: validate-edge-env.sh /secure/path/edge.env}"
env_file="$1"
[ -r "$env_file" ] || { echo "edge env file is not readable" >&2; exit 1; }

set -a
. "$env_file"
set +a

for name in TOWNPET_DOMAIN TOWNPET_MEDIA_DOMAIN ERP_DOMAIN; do
  eval "value=\${$name-}"
  [ -n "$value" ] || { echo "missing required variable: $name" >&2; exit 1; }
  case "$value" in
    *example.com|*example.test|*placeholder*|*replace-with*)
      echo "placeholder value is not allowed: $name" >&2
      exit 1
      ;;
  esac
  case "$value" in
    *://*|*/*|*' '*|*'	'*)
      echo "$name must be a hostname" >&2
      exit 1
      ;;
  esac
done

[ "$TOWNPET_DOMAIN" != "$TOWNPET_MEDIA_DOMAIN" ] || {
  echo "TOWNPET_DOMAIN and TOWNPET_MEDIA_DOMAIN must differ" >&2
  exit 1
}
echo "edge env policy valid: $env_file"
