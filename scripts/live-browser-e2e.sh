#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
export TOWNPET_E2E_CONFIG="e2e/live.config.ts"
export TOWNPET_E2E_ARTIFACT_DIR="${TOWNPET_E2E_ARTIFACT_DIR:-${ROOT_DIR}/build/e2e-artifacts}"
args=()
for arg in "$@"; do
  args+=("${arg#frontend/}")
done
exec "${ROOT_DIR}/scripts/auth-browser-e2e.sh" "${args[@]}"
