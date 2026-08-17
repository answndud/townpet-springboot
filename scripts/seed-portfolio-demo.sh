#!/usr/bin/env bash
set -Eeuo pipefail

# Rebuilds only the synthetic showcase rows owned by the two demo fixtures.
# It never truncates the database and is safe to rerun after a VPS restart.
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FIXTURE_DIR="${FIXTURE_DIR:-$ROOT_DIR/migration/fixtures}"
if [[ ! -r "$FIXTURE_DIR/local-demo.sql" && -r "$(dirname "${BASH_SOURCE[0]}")/local-demo.sql" ]]; then
  FIXTURE_DIR="$(dirname "${BASH_SOURCE[0]}")"
fi
COMPOSE_FILE="${COMPOSE_FILE:-$ROOT_DIR/deploy/compose/netcup.yml}"
COMPOSE_ENV_FILE="${COMPOSE_ENV_FILE:-/opt/townpet/secrets/portfolio.env}"
POSTGRES_CONTAINER="${POSTGRES_CONTAINER:-townpet-postgres}"
POSTGRES_USER="${POSTGRES_USER:-townpet_app}"
POSTGRES_DB="${POSTGRES_DB:-townpet}"

command -v docker >/dev/null 2>&1 || { echo "docker is required" >&2; exit 1; }
test -r "$FIXTURE_DIR/local-demo.sql" || { echo "local-demo.sql is missing" >&2; exit 1; }
test -r "$FIXTURE_DIR/local-community-demo.sql" || { echo "local-community-demo.sql is missing" >&2; exit 1; }

health="$(docker inspect --format '{{.State.Health.Status}}' "$POSTGRES_CONTAINER" 2>/dev/null || true)"
[[ "$health" == "healthy" ]] || {
  echo "$POSTGRES_CONTAINER is not healthy (status: ${health:-missing})" >&2
  exit 1
}

psql() {
  docker exec -i "$POSTGRES_CONTAINER" psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB" "$@"
}

psql < "$FIXTURE_DIR/local-demo.sql"
psql < "$FIXTURE_DIR/local-community-demo.sql"

psql -Atq <<'SQL'
SELECT 'portfolio_demo_publications=' || count(*)
FROM publication
WHERE lifecycle = 'ACTIVE';
SELECT 'portfolio_demo_comments=' || count(*)
FROM engagement_comment
WHERE lifecycle = 'ACTIVE';
SELECT 'portfolio_demo_reactions=' || count(*)
FROM engagement_reaction
WHERE type = 'LIKE';
SQL

echo "Portfolio demo fixture is ready. The public credentials are documented in docs/04-데모/로컬-데모-계정.md."
