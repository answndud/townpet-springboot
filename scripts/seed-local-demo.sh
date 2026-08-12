#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COMPOSE_FILE="$ROOT_DIR/deploy/compose/local.yml"
FIXTURE_FILE="$ROOT_DIR/migration/fixtures/local-demo.sql"

command -v docker >/dev/null 2>&1 || { echo "docker is required" >&2; exit 1; }
test -f "$FIXTURE_FILE" || { echo "fixture not found: $FIXTURE_FILE" >&2; exit 1; }

echo "Starting the local TownPet stack and applying Flyway migrations..."
docker compose -f "$COMPOSE_FILE" up -d --build postgres backend minio

for attempt in $(seq 1 60); do
  postgres_health="$(docker inspect -f '{{.State.Health.Status}}' townpet-postgres 2>/dev/null || true)"
  backend_container="$(docker compose -f "$COMPOSE_FILE" ps -q backend 2>/dev/null || true)"
  backend_health=""
  if [[ -n "$backend_container" ]]; then
    backend_health="$(docker inspect -f '{{.State.Health.Status}}' "$backend_container" 2>/dev/null || true)"
  fi
  if [[ "$postgres_health" == "healthy" && "$backend_health" == "healthy" ]]; then
    break
  fi
  if [[ "$postgres_health" == "unhealthy" || "$backend_health" == "unhealthy" ]]; then
    echo "Local stack did not become healthy. Check: docker compose -f $COMPOSE_FILE logs backend postgres" >&2
    exit 1
  fi
  sleep 2
  if [[ "$attempt" == 60 ]]; then
    echo "Timed out waiting for the local stack" >&2
    exit 1
  fi
done

docker compose -f "$COMPOSE_FILE" exec -T postgres \
  psql -v ON_ERROR_STOP=1 -U townpet_app -d townpet < "$FIXTURE_FILE"

echo "Local demo data is ready. See docs/demo/local-demo-accounts.md for credentials."
