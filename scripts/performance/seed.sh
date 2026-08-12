#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
CONTAINER_NAME="${TOWNPET_PERF_DB_CONTAINER:-townpet-postgres-perf}"
DB_NAME="${TOWNPET_PERF_DB_NAME:-townpet_perf}"
DB_USER="${TOWNPET_PERF_DB_USERNAME:-townpet_perf}"
SCALE="${1:-small}"

case "$SCALE" in
  small) ROWS=2000 ;;
  medium) ROWS=20000 ;;
  large) ROWS=100000 ;;
  *) echo "usage: $0 [small|medium|large]" >&2; exit 2 ;;
esac

test -f "$ROOT_DIR/scripts/performance/seed.sql" || exit 1
docker exec -i "$CONTAINER_NAME" psql -v ON_ERROR_STOP=1 \
  -v scale="$ROWS" -U "$DB_USER" -d "$DB_NAME" \
  < "$ROOT_DIR/scripts/performance/seed.sql"

docker exec "$CONTAINER_NAME" psql -Atq -U "$DB_USER" -d "$DB_NAME" -c \
  "SELECT 'publication=' || count(*) FROM publication WHERE title LIKE 'perf-publication-%';
   SELECT 'volunteer=' || count(*) FROM volunteer_opportunity WHERE title LIKE 'perf-opportunity-%';
   SELECT 'report=' || count(*) FROM trust_report WHERE detail = 'performance-fixture';"
