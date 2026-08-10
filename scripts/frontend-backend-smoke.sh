#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
if [[ -n "${TOWNPET_JAVA_HOME:-}" ]]; then
  JAVA_HOME="${TOWNPET_JAVA_HOME}"
elif [[ "${JAVA_HOME:-}" == *"openjdk@21"* || -z "${JAVA_HOME:-}" ]]; then
  JAVA_HOME="/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home"
fi
export JAVA_HOME

backend_log="$(mktemp -t townpet-backend-smoke.XXXXXX.log)"
frontend_log="$(mktemp -t townpet-frontend-smoke.XXXXXX.log)"
backend_pid=""
frontend_pid=""

cleanup() {
  [[ -n "${frontend_pid}" ]] && kill "${frontend_pid}" 2>/dev/null || true
  [[ -n "${backend_pid}" ]] && kill "${backend_pid}" 2>/dev/null || true
  rm -f "${backend_log}" "${frontend_log}"
}
trap cleanup EXIT

cd "${ROOT_DIR}"
./gradlew bootRun --args='--spring.profiles.active=smoke' >"${backend_log}" 2>&1 &
backend_pid=$!
for _ in {1..60}; do
  if curl --fail --silent http://127.0.0.1:8080/actuator/health >/dev/null; then break; fi
  sleep 1
done
curl --fail --silent http://127.0.0.1:8080/actuator/health | grep -q '"status":"UP"'

corepack pnpm -C frontend install --frozen-lockfile >/dev/null
corepack pnpm -C frontend build >/dev/null
corepack pnpm -C frontend preview --host 127.0.0.1 >"${frontend_log}" 2>&1 &
frontend_pid=$!
for _ in {1..30}; do
  if curl --fail --silent http://127.0.0.1:4173/ >/dev/null; then break; fi
  sleep 1
done
curl --fail --silent http://127.0.0.1:4173/ | grep -q 'TownPet'
echo "frontend-backend smoke passed"
