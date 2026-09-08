#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
CONTAINER_NAME="${TOWNPET_PERF_DB_CONTAINER:-townpet-postgres-perf}"
DB_NAME="${TOWNPET_PERF_DB_NAME:-townpet_perf}"
DB_USER="${TOWNPET_PERF_DB_USERNAME:-townpet_perf}"
SCALE="${1:-small}"
SEED_RUN_ID="$(date -u +%Y%m%dT%H%M%SZ)-${SCALE}"
SEED_DIR="$ROOT_DIR/build/performance/seeds/$SEED_RUN_ID"
WORKING_TREE_STATE="$(test -z "$(git -C "$ROOT_DIR" status --porcelain)" && echo clean || echo dirty)"
test "$WORKING_TREE_STATE" = clean \
  || { echo "clean working tree required before performance evidence" >&2; exit 1; }

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

mkdir -p "$SEED_DIR"
{
  echo "seed_run_id=$SEED_RUN_ID"
echo "commit=$(git -C "$ROOT_DIR" rev-parse HEAD)"
  echo "working_tree_state=clean"
  echo "scale=$SCALE"
  echo "planned_publication_rows=$ROWS"
  echo "container=$CONTAINER_NAME"
  echo "database=$DB_NAME"
  echo "postgres=$(docker exec "$CONTAINER_NAME" psql -Atq -U "$DB_USER" -d "$DB_NAME" -c 'SHOW server_version' | tr -d '\n')"
  echo "postgis=$(docker exec "$CONTAINER_NAME" psql -Atq -U "$DB_USER" -d "$DB_NAME" -c 'SELECT PostGIS_Full_Version()' | tr -d '\n')"
  echo "seeded_at=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
} > "$SEED_DIR/metadata.txt"

docker exec -i "$CONTAINER_NAME" psql -Atq -U "$DB_USER" -d "$DB_NAME" > "$SEED_DIR/distribution.tsv" <<'SQL'
SELECT 'publication_count' AS metric, count(*)::text AS value
FROM publication WHERE title LIKE 'perf-publication-%'
UNION ALL
SELECT 'publication_type_' || type, count(*)::text
FROM publication WHERE title LIKE 'perf-publication-%' GROUP BY type
UNION ALL
SELECT 'publication_lifecycle_' || lifecycle, count(*)::text
FROM publication WHERE title LIKE 'perf-publication-%' GROUP BY lifecycle
UNION ALL
SELECT 'publication_search_hit_perf', count(*)::text
FROM publication WHERE title LIKE 'perf-publication-%' AND (title ILIKE '%perf%' OR body ILIKE '%perf%')
UNION ALL
SELECT 'publication_created_at_min', min(created_at)::text
FROM publication WHERE title LIKE 'perf-publication-%'
UNION ALL
SELECT 'publication_created_at_max', max(created_at)::text
FROM publication WHERE title LIKE 'perf-publication-%'
UNION ALL
SELECT 'volunteer_count', count(*)::text
FROM volunteer_opportunity WHERE title LIKE 'perf-opportunity-%'
UNION ALL
SELECT 'report_count', count(*)::text
FROM trust_report WHERE detail = 'performance-fixture';
SQL

echo "Performance seed ready: $SEED_DIR"
