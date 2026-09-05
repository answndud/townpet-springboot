#!/usr/bin/env bash
set -Eeuo pipefail

COMPOSE_FILE="${COMPOSE_FILE:-deploy/compose/netcup.yml}"
EDGE_COMPOSE_FILE="${EDGE_COMPOSE_FILE:-deploy/compose/edge.yml}"
COMPOSE_ENV_FILE="${COMPOSE_ENV_FILE:-deploy/netcup.env.example}"
EDGE_ENV_FILE="${EDGE_ENV_FILE:-deploy/edge.env.example}"
EDGE_ENV_VALIDATOR="${EDGE_ENV_VALIDATOR:-scripts/validate-edge-env.sh}"
TOWNPET_ENV_VALIDATOR="${TOWNPET_ENV_VALIDATOR:-scripts/validate-portfolio-env.sh}"
RUNTIME_ROLE_GRANTER="${RUNTIME_ROLE_GRANTER:-deploy/grant-runtime-db-role.sh}"
DEPLOY_LOCK_FILE="${DEPLOY_LOCK_FILE:-/tmp/portfolio-deploy.lock}"
SMOKE_URL="${SMOKE_URL:-}"
DEPLOYMENT_ID="${DEPLOYMENT_ID:-$(date -u +%Y%m%dT%H%M%SZ)-${TOWNPET_BUILD_VERSION:-unknown}}"
PREFLIGHT_ONLY="${PREFLIGHT_ONLY:-0}"
MAX_ATTEMPTS="${MAX_ATTEMPTS:-30}"
SLEEP_SECONDS="${SLEEP_SECONDS:-5}"

compose() {
  docker compose --env-file "$COMPOSE_ENV_FILE" -f "$COMPOSE_FILE" "$@"
}

edge_compose() {
  docker compose --env-file "$EDGE_ENV_FILE" -f "$EDGE_COMPOSE_FILE" "$@"
}

schema_version() {
  compose exec -T postgres sh -c \
    'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Atqc "SELECT version FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 1"' \
    2>/dev/null | tr -d '\r' || true
}

phase="preflight"
log_event() {
  echo "event=deployment deployment_id=$DEPLOYMENT_ID phase=$phase outcome=$1${2:+ $2}"
}

diagnostics() {
  phase="diagnostics"
  log_event "started"
  compose ps >&2 || true
  edge_compose ps >&2 || true
  for container in townpet-backend townpet-postgres townpet-minio townpet-minio-init townpet-web; do
    docker inspect --format "event=container_state deployment_id=$DEPLOYMENT_ID container={{.Name}} status={{.State.Status}} health={{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}} restart_count={{.RestartCount}} oom_killed={{.State.OOMKilled}}" "$container" >&2 2>/dev/null || true
  done
  compose logs --tail=200 postgres minio minio-init backend web >&2 || true
  edge_compose logs --tail=100 edge >&2 || true
}

on_exit() {
  status="$?"
  if [[ "$status" -ne 0 ]]; then
    log_event "failed" "exit_code=$status"
    diagnostics
  fi
  exit "$status"
}
trap on_exit EXIT

[[ -f "$COMPOSE_FILE" && -f "$EDGE_COMPOSE_FILE" && -f "$COMPOSE_ENV_FILE" && -f "$EDGE_ENV_FILE" ]] || {
  echo "TownPet netcup deployment asset is missing" >&2
  exit 1
}
[[ -x "$EDGE_ENV_VALIDATOR" && -x "$TOWNPET_ENV_VALIDATOR" && -x "$RUNTIME_ROLE_GRANTER" ]] || {
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
log_event "success"
docker network inspect edge >/dev/null 2>&1 || docker network create edge >/dev/null
compose config >/dev/null
docker compose --env-file "$EDGE_ENV_FILE" -f "$EDGE_COMPOSE_FILE" config >/dev/null

if [[ "$PREFLIGHT_ONLY" == "1" ]]; then
  log_event "success" "preflight_only=true"
  exit 0
fi

previous_image="$(docker inspect --format '{{.Config.Image}}' townpet-backend 2>/dev/null || true)"
previous_web_image="$(docker inspect --format '{{.Config.Image}}' townpet-web 2>/dev/null || true)"
phase="edge"
edge_compose up -d
log_event "success"
phase="pull"
compose pull
log_event "success"
phase="postgres"
compose up -d postgres
log_event "success"
phase="runtime_role"
COMPOSE_FILE="$COMPOSE_FILE" COMPOSE_ENV_FILE="$COMPOSE_ENV_FILE" "$RUNTIME_ROLE_GRANTER"
log_event "success"
phase="migration_guard"
schema_before="$(schema_version)"
if [[ -z "$schema_before" ]]; then
  log_event "failed" "reason=unable_to_read_flyway_version"
  exit 1
fi
log_event "success" "schema_version_present=true"
phase="application"
compose up -d minio minio-init backend web
log_event "success"

ready=1
phase="readiness"
for _ in $(seq 1 "$MAX_ATTEMPTS"); do
  backend_health="$(docker inspect --format '{{.State.Health.Status}}' townpet-backend 2>/dev/null || true)"
  web_health="$(docker inspect --format '{{.State.Health.Status}}' townpet-web 2>/dev/null || true)"
  if [[ "$backend_health" == "healthy" && "$web_health" == "healthy" ]]; then
    ready=0
    log_event "success" "backend_health=$backend_health web_health=$web_health"
    break
  fi
  sleep "$SLEEP_SECONDS"
done

if [[ "$ready" -eq 0 && -n "$SMOKE_URL" ]]; then
  phase="smoke"
  curl --fail --silent --show-error --location --max-time 10 "$SMOKE_URL" >/dev/null || ready=1
  [[ "$ready" -eq 0 ]] && log_event "success" "smoke_url_configured=true"
fi

if [[ "$ready" -eq 0 ]]; then
  phase="complete"
  log_event "success"
  exit 0
fi

schema_after="$(schema_version)"
if [[ -z "$schema_after" || "$schema_before" != "$schema_after" ]]; then
  phase="rollback"
  log_event "unavailable" "reason=schema_changed_or_unreadable automatic_image_rollback=false"
  diagnostics
  exit 1
fi

phase="rollback"
echo "event=deployment deployment_id=$DEPLOYMENT_ID phase=rollback outcome=started" >&2
if [[ -z "$previous_image" ]]; then
  echo "event=deployment deployment_id=$DEPLOYMENT_ID phase=rollback outcome=unavailable reason=no_previous_backend_image" >&2
  exit 1
fi
if [[ -n "$previous_web_image" ]]; then
  TOWNPET_BACKEND_IMAGE="$previous_image" TOWNPET_WEB_IMAGE="$previous_web_image" compose up -d backend web
else
  TOWNPET_BACKEND_IMAGE="$previous_image" compose up -d backend web
fi
for _ in $(seq 1 "$MAX_ATTEMPTS"); do
  backend_health="$(docker inspect --format '{{.State.Health.Status}}' townpet-backend 2>/dev/null || true)"
  web_health="$(docker inspect --format '{{.State.Health.Status}}' townpet-web 2>/dev/null || true)"
  [[ "$backend_health" == "healthy" && "$web_health" == "healthy" ]] && {
    if [[ -n "$SMOKE_URL" ]] && ! curl --fail --silent --show-error --location --max-time 10 "$SMOKE_URL" >/dev/null; then
      echo "event=deployment deployment_id=$DEPLOYMENT_ID phase=rollback outcome=smoke_failed" >&2
      exit 1
    fi
    echo "event=deployment deployment_id=$DEPLOYMENT_ID phase=rollback outcome=success backend_image=$previous_image" >&2
    exit 1
  }
  sleep "$SLEEP_SECONDS"
done
compose ps >&2 || true
compose logs --tail=200 backend >&2 || true
echo "rollback failed" >&2
exit 1
