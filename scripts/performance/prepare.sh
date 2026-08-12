#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
CONTAINER_NAME="${TOWNPET_PERF_DB_CONTAINER:-townpet-postgres-perf}"
DB_PORT="${TOWNPET_PERF_DB_PORT:-54331}"
DB_NAME="${TOWNPET_PERF_DB_NAME:-townpet_perf}"
DB_USER="${TOWNPET_PERF_DB_USERNAME:-townpet_perf}"
DB_PASSWORD="${TOWNPET_PERF_DB_PASSWORD:-townpet_perf_local}"

case "${1:-small}" in
  small) ROWS=2000 ;;
  medium) ROWS=20000 ;;
  large) ROWS=100000 ;;
  *) echo "usage: $0 [small|medium|large]" >&2; exit 2 ;;
esac

command -v docker >/dev/null 2>&1 || { echo "docker is required" >&2; exit 1; }
mkdir -p "$ROOT_DIR/build/performance/media" "$ROOT_DIR/build/performance/run"

if ! docker inspect "$CONTAINER_NAME" >/dev/null 2>&1; then
  docker run -d --name "$CONTAINER_NAME" \
    -e POSTGRES_DB="$DB_NAME" \
    -e POSTGRES_USER="$DB_USER" \
    -e POSTGRES_PASSWORD="$DB_PASSWORD" \
    -p "${DB_PORT}:5432" \
    postgis/postgis:18-3.6 >/dev/null
fi

docker start "$CONTAINER_NAME" >/dev/null 2>&1 || true
for attempt in $(seq 1 60); do
  if docker exec "$CONTAINER_NAME" pg_isready -U "$DB_USER" -d "$DB_NAME" >/dev/null 2>&1; then
    break
  fi
  if [[ "$attempt" == 60 ]]; then
    echo "Timed out waiting for $CONTAINER_NAME" >&2
    exit 1
  fi
  sleep 2
done

docker exec "$CONTAINER_NAME" psql -v ON_ERROR_STOP=1 -U "$DB_USER" -d "$DB_NAME" \
  -c "CREATE EXTENSION IF NOT EXISTS postgis; CREATE EXTENSION IF NOT EXISTS citext;" >/dev/null

echo "Performance database ready: ${DB_NAME} (${ROWS} publication rows planned)"
echo "Next: ./scripts/performance/start.sh && ./scripts/performance/seed.sh ${1:-small}"
