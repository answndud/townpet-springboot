#!/usr/bin/env bash
set -Eeuo pipefail

COMPOSE_FILE="${COMPOSE_FILE:-deploy/compose/netcup.yml}"
EDGE_COMPOSE_FILE="${EDGE_COMPOSE_FILE:-deploy/compose/edge.yml}"
COMPOSE_ENV_FILE="${COMPOSE_ENV_FILE:-deploy/netcup.env.example}"
EDGE_ENV_FILE="${EDGE_ENV_FILE:-deploy/edge.env.example}"
EDGE_ENV_VALIDATOR="${EDGE_ENV_VALIDATOR:-scripts/validate-edge-env.sh}"
TOWNPET_ENV_VALIDATOR="${TOWNPET_ENV_VALIDATOR:-scripts/validate-portfolio-env.sh}"
DEPLOY_LOCK_FILE="${DEPLOY_LOCK_FILE:-/tmp/portfolio-deploy.lock}"
SMOKE_URL="${SMOKE_URL:-}"
PREFLIGHT_ONLY="${PREFLIGHT_ONLY:-0}"
MAX_ATTEMPTS="${MAX_ATTEMPTS:-30}"
SLEEP_SECONDS="${SLEEP_SECONDS:-5}"

compose() {
  docker compose --env-file "$COMPOSE_ENV_FILE" -f "$COMPOSE_FILE" "$@"
}

[[ -f "$COMPOSE_FILE" && -f "$EDGE_COMPOSE_FILE" && -f "$COMPOSE_ENV_FILE" && -f "$EDGE_ENV_FILE" ]] || {
  echo "TownPet netcup deployment asset is missing" >&2
  exit 1
}
[[ -x "$EDGE_ENV_VALIDATOR" && -x "$TOWNPET_ENV_VALIDATOR" ]] || {
  echo "deployment env validator is missing or not executable" >&2
  exit 1
}
if command -v flock >/dev/null; then
  exec 9>"$DEPLOY_LOCK_FILE"
  flock -n 9 || { echo "another portfolio deployment is already running" >&2; exit 1; }
elif [[ "${ALLOW_UNSERIALIZED_DEPLOY:-0}" == "1" ]]; then
  echo "warning: flock is unavailable; deployment is explicitly running without a lock" >&2
else
  echo "flock is required for deployment serialization (or set ALLOW_UNSERIALIZED_DEPLOY=1 for local rehearsal)" >&2
  exit 1
fi
"$EDGE_ENV_VALIDATOR" "$EDGE_ENV_FILE"
"$TOWNPET_ENV_VALIDATOR" "$COMPOSE_ENV_FILE"
docker network inspect edge >/dev/null 2>&1 || docker network create edge >/dev/null
compose config >/dev/null
docker compose --env-file "$EDGE_ENV_FILE" -f "$EDGE_COMPOSE_FILE" config >/dev/null

if [[ "$PREFLIGHT_ONLY" == "1" ]]; then
  echo "TownPet netcup deployment preflight passed"
  exit 0
fi

previous_image="$(docker inspect --format '{{.Config.Image}}' townpet-backend 2>/dev/null || true)"
docker compose --env-file "$EDGE_ENV_FILE" -f "$EDGE_COMPOSE_FILE" up -d
compose pull
compose up -d postgres minio minio-init backend web

ready=1
for _ in $(seq 1 "$MAX_ATTEMPTS"); do
  health="$(docker inspect --format '{{.State.Health.Status}}' townpet-backend 2>/dev/null || true)"
  if [[ "$health" == "healthy" ]]; then
    ready=0
    break
  fi
  sleep "$SLEEP_SECONDS"
done

if [[ "$ready" -eq 0 && -n "$SMOKE_URL" ]]; then
  curl --fail --silent --show-error --location --max-time 10 "$SMOKE_URL" >/dev/null || ready=1
fi

if [[ "$ready" -eq 0 ]]; then
  echo "TownPet netcup deployment ready"
  exit 0
fi

echo "TownPet deployment failed; attempting backend image rollback" >&2
if [[ -z "$previous_image" ]]; then
  compose ps >&2 || true
  compose logs --tail=200 backend >&2 || true
  exit 1
fi
TOWNPET_BACKEND_IMAGE="$previous_image" compose up -d backend web
for _ in $(seq 1 "$MAX_ATTEMPTS"); do
  health="$(docker inspect --format '{{.State.Health.Status}}' townpet-backend 2>/dev/null || true)"
  [[ "$health" == "healthy" ]] && {
    echo "rollback restored $previous_image" >&2
    exit 1
  }
  sleep "$SLEEP_SECONDS"
done
compose ps >&2 || true
compose logs --tail=200 backend >&2 || true
echo "rollback failed" >&2
exit 1
