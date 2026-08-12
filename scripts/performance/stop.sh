#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
CONTAINER_NAME="${TOWNPET_PERF_DB_CONTAINER:-townpet-postgres-perf}"
PID_FILE="$ROOT_DIR/build/performance/run/backend.pid"

if [[ -f "$PID_FILE" ]]; then
  pid="$(cat "$PID_FILE")"
  kill "$pid" 2>/dev/null || true
  for _ in $(seq 1 20); do
    kill -0 "$pid" 2>/dev/null || break
    sleep 1
  done
  rm -f "$PID_FILE"
fi

docker stop "$CONTAINER_NAME" >/dev/null 2>&1 || true
echo "Stopped performance backend and database container $CONTAINER_NAME"
