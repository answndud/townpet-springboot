#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SCENARIO="smoke"
PROFILE="smoke"
BASE_URL="${TOWNPET_PERF_BASE_URL:-http://host.docker.internal:8081}"
K6_IMAGE="${TOWNPET_K6_IMAGE:-grafana/k6:0.52.0}"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --scenario) SCENARIO="$2"; shift 2 ;;
    --profile) PROFILE="$2"; shift 2 ;;
    --base-url) BASE_URL="$2"; shift 2 ;;
    *) echo "usage: $0 --scenario smoke|public-read|feed-read|member-read|write|contention|moderator|media|mixed --profile smoke|baseline|calibration|ramp|contention|soak|spike [--base-url URL]" >&2; exit 2 ;;
  esac
done

case "$SCENARIO" in
  smoke|public-read|feed-read|member-read|write|contention|moderator|media|mixed) ;;
  *) echo "unsupported scenario: $SCENARIO" >&2; exit 2 ;;
esac

RUN_ID="$(date -u +%Y%m%dT%H%M%SZ)-${SCENARIO}-${PROFILE}-$(git -C "$ROOT_DIR" rev-parse --short HEAD)"
OUT_DIR="$ROOT_DIR/build/performance/runs/$RUN_ID"
mkdir -p "$OUT_DIR"
{
  echo "run_id=$RUN_ID"
  echo "commit=$(git -C "$ROOT_DIR" rev-parse HEAD)"
  echo "scenario=$SCENARIO"
  echo "profile=$PROFILE"
  echo "base_url=$BASE_URL"
  echo "k6_image=$K6_IMAGE"
  echo "started_at=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
  echo "host=$(hostname)"
  echo "java=$(java -version 2>&1 | head -1)"
} > "$OUT_DIR/metadata.txt"

PERF_PID_FILE="$ROOT_DIR/build/performance/run/backend.pid"
PERF_DB_CONTAINER="${TOWNPET_PERF_DB_CONTAINER:-townpet-postgres-perf}"
capture_resources() {
  while kill -0 "$K6_PID" 2>/dev/null; do
    printf '%s ' "$(date -u +%Y-%m-%dT%H:%M:%SZ)" >> "$OUT_DIR/resources.tsv"
    if [[ -f "$PERF_PID_FILE" ]]; then
      ps -p "$(cat "$PERF_PID_FILE")" -o pid=,pcpu=,rss=,etime= 2>/dev/null | tr '\n' ' ' >> "$OUT_DIR/resources.tsv" || true
    fi
    docker stats --no-stream --format 'db_cpu={{.CPUPerc}} db_mem={{.MemUsage}} db_net={{.NetIO}} db_block={{.BlockIO}}' "$PERF_DB_CONTAINER" 2>/dev/null >> "$OUT_DIR/resources.tsv" || true
    printf '\n' >> "$OUT_DIR/resources.tsv"
    sleep 5
  done
}

export ALLOW_EXPECTED_CONFLICTS="${ALLOW_EXPECTED_CONFLICTS:-false}"
export CONTENTION_CASE="${CONTENTION_CASE:-views}"
export PERF_MEMBER_COUNT="${PERF_MEMBER_COUNT:-100}"

docker run --rm \
  --add-host=host.docker.internal:host-gateway \
  -v "$ROOT_DIR/loadtest:/scripts:ro" \
  -v "$OUT_DIR:/results" \
  -e BASE_URL="$BASE_URL" \
  -e LOAD_PROFILE="$PROFILE" \
  -e ALLOW_EXPECTED_CONFLICTS="$ALLOW_EXPECTED_CONFLICTS" \
  -e CONTENTION_CASE="$CONTENTION_CASE" \
  -e PERF_MEMBER_COUNT="$PERF_MEMBER_COUNT" \
  "$K6_IMAGE" run --quiet \
  --summary-export "/results/summary.json" \
  "/scripts/${SCENARIO}.js" > "$OUT_DIR/console.log" 2>&1 &
K6_PID=$!
capture_resources & RESOURCE_PID=$!
set +e
wait "$K6_PID"
K6_STATUS=$?
set -e
wait "$RESOURCE_PID" 2>/dev/null || true

if [[ -f "$PERF_PID_FILE" ]]; then
  APP_PID="$(cat "$PERF_PID_FILE")"
  {
    echo "--- jcmd VM.info ---"
    jcmd "$APP_PID" VM.info 2>&1 || true
    echo "--- jcmd GC.heap_info ---"
    jcmd "$APP_PID" GC.heap_info 2>&1 || true
  } > "$OUT_DIR/jvm-after.txt"
fi

cat "$OUT_DIR/console.log"
if [[ "$K6_STATUS" -ne 0 ]]; then
  echo "k6 failed with exit code $K6_STATUS; see $OUT_DIR/console.log" >&2
  exit "$K6_STATUS"
fi

echo "Performance result: $OUT_DIR"
