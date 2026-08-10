#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COMPOSE_PROJECT="townpet-auth-e2e"
BACKEND_LOG="$(mktemp -t townpet-auth-backend.XXXXXX.log)"
FRONTEND_LOG="$(mktemp -t townpet-auth-frontend.XXXXXX.log)"
backend_pid=""
frontend_pid=""

cleanup() {
  [[ -n "${frontend_pid}" ]] && kill "${frontend_pid}" 2>/dev/null || true
  [[ -n "${backend_pid}" ]] && kill "${backend_pid}" 2>/dev/null || true
  docker compose -p "${COMPOSE_PROJECT}" -f "${ROOT_DIR}/deploy/compose/e2e.yml" down --volumes >/dev/null 2>&1 || true
  rm -f "${BACKEND_LOG}" "${FRONTEND_LOG}"
}
trap cleanup EXIT

if [[ "${1:-}" == "--" ]]; then
  shift
fi

docker compose -p "${COMPOSE_PROJECT}" -f "${ROOT_DIR}/deploy/compose/e2e.yml" up --detach --wait

gradle_args=(bootRun "--args=--spring.profiles.active=e2e")
if [[ -n "${TOWNPET_GRADLE_INIT_SCRIPT:-}" ]]; then
  gradle_args=(--init-script "${TOWNPET_GRADLE_INIT_SCRIPT}" "${gradle_args[@]}")
fi

cd "${ROOT_DIR}"
TOWNPET_DB_URL="jdbc:postgresql://127.0.0.1:54330/townpet" \
TOWNPET_DB_USERNAME="townpet_app" \
TOWNPET_DB_PASSWORD="townpet_local_dev" \
  ./gradlew "${gradle_args[@]}" >"${BACKEND_LOG}" 2>&1 &
backend_pid=$!

for _ in {1..90}; do
  if curl --fail --silent http://127.0.0.1:8080/actuator/health >/dev/null; then
    break
  fi
  if ! kill -0 "${backend_pid}" 2>/dev/null; then
    cat "${BACKEND_LOG}"
    exit 1
  fi
  sleep 1
done
if ! curl --fail --silent http://127.0.0.1:8080/actuator/health >/dev/null; then
  cat "${BACKEND_LOG}"
  exit 1
fi

corepack pnpm -C frontend exec vite --host 127.0.0.1 >"${FRONTEND_LOG}" 2>&1 &
frontend_pid=$!
for _ in {1..30}; do
  if curl --fail --silent http://127.0.0.1:5173/ >/dev/null; then
    break
  fi
  if ! kill -0 "${frontend_pid}" 2>/dev/null; then
    cat "${FRONTEND_LOG}"
    exit 1
  fi
  sleep 1
done
if ! curl --fail --silent http://127.0.0.1:5173/ >/dev/null; then
  cat "${FRONTEND_LOG}"
  exit 1
fi

corepack pnpm -C frontend exec playwright test --config e2e/auth.config.ts "$@"

verify_auth_evidence=false
verify_publication_evidence=false
if (( $# == 0 )); then
  verify_auth_evidence=true
  verify_publication_evidence=true
fi
for test_filter in "$@"; do
  [[ "${test_filter}" == *"auth-parity"* ]] && verify_auth_evidence=true
  [[ "${test_filter}" == *"publication-parity"* ]] && verify_publication_evidence=true
done

session_count="$(
  docker compose -p "${COMPOSE_PROJECT}" -f "${ROOT_DIR}/deploy/compose/e2e.yml" \
    exec -T postgres psql -U postgres -d townpet -tAc \
    "SELECT COUNT(*) FROM spring_session WHERE principal_name IS NOT NULL"
)"
audit_count="$(
  docker compose -p "${COMPOSE_PROJECT}" -f "${ROOT_DIR}/deploy/compose/e2e.yml" \
    exec -T postgres psql -U postgres -d townpet -tAc \
    "SELECT COUNT(*) FROM identity_auth_audit WHERE action IN ('PASSWORD_RESET', 'EMAIL_VERIFIED')"
)"
publication_count="$(
  docker compose -p "${COMPOSE_PROJECT}" -f "${ROOT_DIR}/deploy/compose/e2e.yml" \
    exec -T postgres psql -U postgres -d townpet -tAc \
    "SELECT COUNT(*) FROM publication WHERE type = 'FREE_BOARD' AND lifecycle = 'ACTIVE'"
)"
if (( session_count < 2 )); then
  echo "Expected JDBC session evidence, got sessions=${session_count}" >&2
  exit 1
fi
if [[ "${verify_auth_evidence}" == true ]] && (( audit_count < 4 )); then
  echo "Expected JDBC session and auth audit evidence, got sessions=${session_count}, audits=${audit_count}" >&2
  exit 1
fi
if [[ "${verify_publication_evidence}" == true ]] && (( publication_count < 2 )); then
  echo "Expected PostgreSQL publication evidence, got publications=${publication_count}" >&2
  exit 1
fi
echo "PostgreSQL evidence verified: sessions=${session_count}, auth_audits=${audit_count}, publications=${publication_count}"
