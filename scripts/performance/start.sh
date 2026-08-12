#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
PORT="${TOWNPET_PERF_PORT:-8081}"
DB_PORT="${TOWNPET_PERF_DB_PORT:-54331}"
RUN_DIR="$ROOT_DIR/build/performance/run"
PID_FILE="$RUN_DIR/backend.pid"
LOG_FILE="$RUN_DIR/backend.log"

mkdir -p "$RUN_DIR"
if [[ -f "$PID_FILE" ]] && kill -0 "$(cat "$PID_FILE")" 2>/dev/null; then
  if curl -fsS "http://localhost:${PORT}/actuator/health" >/dev/null 2>&1; then
    echo "Performance backend is already running on port $PORT"
    exit 0
  fi
  kill "$(cat "$PID_FILE")" 2>/dev/null || true
fi

(cd "$ROOT_DIR" && ./gradlew bootJar --no-daemon)
JAR_FILE="$(find "$ROOT_DIR/build/libs" -maxdepth 1 -name '*.jar' ! -name '*-plain.jar' -print -quit)"
test -n "$JAR_FILE" || { echo "boot jar not found" >&2; exit 1; }

cd "$ROOT_DIR"
nohup env \
  TOWNPET_PERF_PORT="$PORT" \
  TOWNPET_PERF_DB_URL="jdbc:postgresql://localhost:${DB_PORT}/townpet_perf" \
  TOWNPET_PERF_DB_USERNAME="${TOWNPET_PERF_DB_USERNAME:-townpet_perf}" \
  TOWNPET_PERF_DB_PASSWORD="${TOWNPET_PERF_DB_PASSWORD:-townpet_perf_local}" \
  SPRING_PROFILES_ACTIVE=perf \
  java -jar "$JAR_FILE" >"$LOG_FILE" 2>&1 < /dev/null &
echo $! > "$PID_FILE"

for attempt in $(seq 1 90); do
  if curl -fsS "http://localhost:${PORT}/actuator/health" >/dev/null 2>&1; then
    echo "Performance backend ready: http://localhost:${PORT}"
    exit 0
  fi
  if ! kill -0 "$(cat "$PID_FILE")" 2>/dev/null; then
    echo "Performance backend exited. See $LOG_FILE" >&2
    exit 1
  fi
  sleep 2
done

echo "Timed out waiting for performance backend. See $LOG_FILE" >&2
exit 1
