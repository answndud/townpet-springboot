#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COMPOSE_FILE="$ROOT_DIR/deploy/compose/local.yml"
FIXTURE_FILE="$ROOT_DIR/migration/fixtures/local-demo.sql"

command -v docker >/dev/null 2>&1 || { echo "docker is required" >&2; exit 1; }
test -f "$FIXTURE_FILE" || { echo "fixture not found: $FIXTURE_FILE" >&2; exit 1; }

postgres_container="$(docker compose -f "$COMPOSE_FILE" ps -q postgres 2>/dev/null || true)"
if [[ -z "$postgres_container" ]]; then
  echo "PostgreSQL is not running. Start it first with:" >&2
  echo "docker compose -f deploy/compose/local.yml up -d postgres" >&2
  exit 1
fi

for attempt in $(seq 1 60); do
  postgres_health="$(docker inspect -f '{{.State.Health.Status}}' "$postgres_container" 2>/dev/null || true)"
  if [[ "$postgres_health" == "healthy" ]]; then
    break
  elif [[ "$postgres_health" == "unhealthy" || "$postgres_health" == "exited" ]]; then
    echo "PostgreSQL is not healthy. Check: docker compose -f deploy/compose/local.yml logs postgres" >&2
    exit 1
  fi
  sleep 2
  if [[ "$attempt" == 60 ]]; then
    echo "Timed out waiting for the local stack" >&2
    exit 1
  fi
done

schema_ready="$(docker compose -f "$COMPOSE_FILE" exec -T postgres \
  psql -Atq -U townpet_app -d townpet -c "select to_regclass('public.member_account')" 2>/dev/null || true)"
if [[ "$schema_ready" != "member_account" ]]; then
  echo "Flyway schema is not ready. Start the Spring Boot backend once, then rerun this script." >&2
  exit 1
fi

docker compose -f "$COMPOSE_FILE" exec -T postgres \
  psql -v ON_ERROR_STOP=1 -U townpet_app -d townpet < "$FIXTURE_FILE"

echo "Local demo data is ready. See docs/04-데모/로컬-데모-계정.md for credentials."
