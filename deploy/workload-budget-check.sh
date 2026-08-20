#!/usr/bin/env sh
set -eu
umask 077

: "${BACKEND_CONTAINER:=townpet-backend}"
: "${POSTGRES_CONTAINER:=townpet-postgres}"
: "${STORAGE_PATH:=/opt/backups}"
: "${DB_CONNECTION_WARN_PERCENT:=80}"
: "${DB_CONNECTION_CRITICAL_PERCENT:=90}"
: "${BACKEND_MEMORY_WARN_PERCENT:=80}"
: "${BACKEND_MEMORY_CRITICAL_PERCENT:=90}"
: "${STORAGE_WARN_PERCENT:=80}"
: "${STORAGE_CRITICAL_PERCENT:=90}"
: "${TOWNPET_WORKLOAD_ALERT_WEBHOOK_URL:=${BACKUP_ALERT_WEBHOOK_URL:-}}"

command -v docker >/dev/null || { echo "docker is required" >&2; exit 1; }

status=PASS
details=""

append_detail() {
  if [ -n "$details" ]; then details="$details,"; fi
  details="$details$1"
}

set_status() {
  requested="$1"
  if [ "$requested" = CRITICAL ]; then
    status=CRITICAL
  elif [ "$requested" = WARN ] && [ "$status" = PASS ]; then
    status=WARN
  fi
}

evaluate_percent() {
  metric="$1"
  value="$2"
  warn="$3"
  critical="$4"
  if awk -v value="$value" -v threshold="$critical" 'BEGIN { exit !(value >= threshold) }'; then
    set_status CRITICAL
    append_detail "$metric=$value"
  elif awk -v value="$value" -v threshold="$warn" 'BEGIN { exit !(value >= threshold) }'; then
    set_status WARN
    append_detail "$metric=$value"
  fi
}

backend_memory_percent="$(docker stats --no-stream --format '{{.MemPerc}}' "$BACKEND_CONTAINER" | tr -d '%' | head -n 1)"
db_connections="$(docker exec "$POSTGRES_CONTAINER" sh -c 'psql -At -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "select count(*) from pg_stat_activity"' | tr -d '[:space:]')"
db_max_connections="$(docker exec "$POSTGRES_CONTAINER" sh -c 'psql -At -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "show max_connections"' | tr -d '[:space:]')"
db_connection_percent="$(awk -v current="$db_connections" -v maximum="$db_max_connections" 'BEGIN { printf "%.2f", (current / maximum) * 100 }')"
storage_percent="$(df -P "$STORAGE_PATH" | awk 'NR == 2 { gsub("%", "", $5); print $5 }')"

evaluate_percent db_connections_percent "$db_connection_percent" "$DB_CONNECTION_WARN_PERCENT" "$DB_CONNECTION_CRITICAL_PERCENT"
evaluate_percent backend_memory_percent "$backend_memory_percent" "$BACKEND_MEMORY_WARN_PERCENT" "$BACKEND_MEMORY_CRITICAL_PERCENT"
evaluate_percent storage_percent "$storage_percent" "$STORAGE_WARN_PERCENT" "$STORAGE_CRITICAL_PERCENT"

echo "workload_budget status=$status db_connections=$db_connections/$db_max_connections db_connections_percent=$db_connection_percent backend_memory_percent=$backend_memory_percent storage_percent=$storage_percent storage_path=$STORAGE_PATH"

if [ "$status" != PASS ]; then
  if [ -n "$TOWNPET_WORKLOAD_ALERT_WEBHOOK_URL" ] && command -v curl >/dev/null 2>&1; then
    payload="$(printf '{"event":"townpet_workload_budget","status":"%s","details":"%s"}' "$status" "$details")"
    curl --fail --silent --show-error --max-time 10 \
      -X POST \
      -H 'Content-Type: application/json' \
      --data "$payload" \
      "$TOWNPET_WORKLOAD_ALERT_WEBHOOK_URL" >/dev/null ||
      echo "workload alert could not be delivered" >&2
  else
    echo "workload alert is not configured" >&2
  fi
fi

case "$status" in
  PASS) exit 0 ;;
  WARN) exit 1 ;;
  CRITICAL) exit 2 ;;
esac
