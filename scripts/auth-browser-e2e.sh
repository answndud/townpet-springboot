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

pnpm_cmd=(corepack pnpm)
if [[ -n "${TOWNPET_PNPM_BIN:-}" ]]; then
  read -r -a pnpm_cmd <<< "${TOWNPET_PNPM_BIN}"
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

cd "${ROOT_DIR}/frontend"
"${pnpm_cmd[@]}" exec vite --host 127.0.0.1 >"${FRONTEND_LOG}" 2>&1 &
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

"${pnpm_cmd[@]}" exec playwright test --config e2e/auth.config.ts "$@"

verify_auth_evidence=false
verify_publication_evidence=false
verify_deleted_publication_evidence=false
verify_comment_evidence=false
verify_reaction_evidence=false
verify_bookmark_evidence=false
verify_relationship_evidence=false
if (( $# == 0 )); then
  verify_auth_evidence=true
  verify_publication_evidence=true
fi
for test_filter in "$@"; do
  [[ "${test_filter}" == *"auth-parity"* ]] && verify_auth_evidence=true
  [[ "${test_filter}" == *"publication-parity"* ]] && verify_publication_evidence=true
  [[ "${test_filter}" == *"public-search-parity"* ]] && verify_publication_evidence=true
  [[ "${test_filter}" == *"publication-management"* ]] && verify_deleted_publication_evidence=true
  [[ "${test_filter}" == *"comment-management"* ]] && verify_comment_evidence=true
  [[ "${test_filter}" == *"reaction-management"* ]] && verify_reaction_evidence=true
  [[ "${test_filter}" == *"bookmark-management"* ]] && verify_bookmark_evidence=true
  [[ "${test_filter}" == *"relationship-management"* ]] && verify_relationship_evidence=true
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
deleted_publication_count="$(
  docker compose -p "${COMPOSE_PROJECT}" -f "${ROOT_DIR}/deploy/compose/e2e.yml" \
    exec -T postgres psql -U postgres -d townpet -tAc \
    "SELECT COUNT(*) FROM publication WHERE type = 'FREE_BOARD' AND lifecycle = 'DELETED'"
)"
comment_count="$(
  docker compose -p "${COMPOSE_PROJECT}" -f "${ROOT_DIR}/deploy/compose/e2e.yml" \
    exec -T postgres psql -U postgres -d townpet -tAc \
    "SELECT COUNT(*) FROM engagement_comment"
)"
reaction_count="$(
  docker compose -p "${COMPOSE_PROJECT}" -f "${ROOT_DIR}/deploy/compose/e2e.yml" \
    exec -T postgres psql -U postgres -d townpet -tAc \
    "SELECT COUNT(*) FROM engagement_reaction"
)"
bookmark_count="$(
  docker compose -p "${COMPOSE_PROJECT}" -f "${ROOT_DIR}/deploy/compose/e2e.yml" \
    exec -T postgres psql -U postgres -d townpet -tAc \
    "SELECT COUNT(*) FROM engagement_bookmark"
)"
follow_count="$(
  docker compose -p "${COMPOSE_PROJECT}" -f "${ROOT_DIR}/deploy/compose/e2e.yml" \
    exec -T postgres psql -U postgres -d townpet -tAc \
    "SELECT COUNT(*) FROM relationship_follow"
)"
block_count="$(
  docker compose -p "${COMPOSE_PROJECT}" -f "${ROOT_DIR}/deploy/compose/e2e.yml" \
    exec -T postgres psql -U postgres -d townpet -tAc \
    "SELECT COUNT(*) FROM relationship_block"
)"
required_session_count=2
if [[ "${verify_comment_evidence}" == true || "${verify_reaction_evidence}" == true || "${verify_bookmark_evidence}" == true || "${verify_relationship_evidence}" == true ]] \
  && [[ "${verify_publication_evidence}" == false ]] \
  && [[ "${verify_deleted_publication_evidence}" == false ]]; then
  required_session_count=1
fi
if (( session_count < required_session_count )); then
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
if [[ "${verify_deleted_publication_evidence}" == true ]] && (( deleted_publication_count < 2 )); then
  echo "Expected lifecycle deletion evidence, got deleted_publications=${deleted_publication_count}" >&2
  exit 1
fi
if [[ "${verify_comment_evidence}" == true ]] && (( comment_count < 1 )); then
  echo "Expected comment evidence, got active_comments=${comment_count}" >&2
  exit 1
fi
if [[ "${verify_reaction_evidence}" == true ]] && (( reaction_count != 0 )); then
  echo "Expected final reaction toggle state to be empty, got reactions=${reaction_count}" >&2
  exit 1
fi
if [[ "${verify_bookmark_evidence}" == true ]] && (( bookmark_count != 0 )); then
  echo "Expected final bookmark toggle state to be empty, got bookmarks=${bookmark_count}" >&2
  exit 1
fi
if [[ "${verify_relationship_evidence}" == true ]] && (( follow_count != 0 || block_count != 0 )); then
  echo "Expected final relationship toggle state to be empty, got follows=${follow_count}, blocks=${block_count}" >&2
  exit 1
fi
echo "PostgreSQL evidence verified: sessions=${session_count}, auth_audits=${audit_count}, publications=${publication_count}, deleted_publications=${deleted_publication_count}, active_comments=${comment_count}, reactions=${reaction_count}, bookmarks=${bookmark_count}, follows=${follow_count}, blocks=${block_count}"
