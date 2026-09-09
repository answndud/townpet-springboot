#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SCENARIO="smoke"
PROFILE="smoke"
BASE_URL="${TOWNPET_PERF_BASE_URL:-http://host.docker.internal:8081}"
READINESS_URL="${TOWNPET_PERF_READINESS_URL:-$BASE_URL}"
READINESS_PATH="${TOWNPET_PERF_READINESS_PATH:-/actuator/health/readiness}"
K6_IMAGE="${TOWNPET_K6_IMAGE:-grafana/k6:0.52.0}"

validate_json() {
  if command -v jq >/dev/null 2>&1; then
    jq empty "$1"
  elif command -v python3 >/dev/null 2>&1; then
    python3 - "$1" <<'PY'
import json
import sys

with open(sys.argv[1], encoding="utf-8") as stream:
    json.load(stream)
PY
  else
    echo "jq or python3 is required to validate JSON" >&2
    return 1
  fi
}

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

if [[ "$READINESS_URL" == *host.docker.internal* ]]; then
  READINESS_URL="${READINESS_URL//host.docker.internal/127.0.0.1}"
fi

RUN_ID="$(date -u +%Y%m%dT%H%M%SZ)-${SCENARIO}-${PROFILE}-$(git -C "$ROOT_DIR" rev-parse --short HEAD)"
OUT_DIR="$ROOT_DIR/build/performance/runs/$RUN_ID"
mkdir -p "$OUT_DIR"
test -z "$(git -C "$ROOT_DIR" status --porcelain)" \
  || { echo "clean working tree required before performance evidence" >&2; exit 1; }
LATEST_SEED_METADATA="$(find "$ROOT_DIR/build/performance/seeds" -mindepth 2 -maxdepth 2 -name metadata.txt -print 2>/dev/null | sort | tail -1 || true)"
if [[ -n "$LATEST_SEED_METADATA" ]]; then
  cp "$LATEST_SEED_METADATA" "$OUT_DIR/seed-metadata.txt"
fi
FIXTURE_SCALE="unknown"
if [[ -n "$LATEST_SEED_METADATA" ]]; then
  FIXTURE_SCALE="$(sed -n 's/^scale=//p' "$LATEST_SEED_METADATA" | head -1)"
fi
case "$PROFILE" in
  smoke) PROFILE_STAGES='15s@1 warm-up; 30s@1'; WARMUP_SECONDS=15; MAX_VUS=1 ;;
  baseline) PROFILE_STAGES='15s@1 warm-up; 2m@1'; WARMUP_SECONDS=15; MAX_VUS=1 ;;
  calibration) PROFILE_STAGES='15s@1 warm-up; 3m@5'; WARMUP_SECONDS=15; MAX_VUS=5 ;;
  ramp) PROFILE_STAGES='15s@1 warm-up; 5m@10; 5m@20; 5m@40'; WARMUP_SECONDS=15; MAX_VUS=40 ;;
  soak) PROFILE_STAGES='30s@1 warm-up; 30m@5'; WARMUP_SECONDS=30; MAX_VUS=5 ;;
  spike) PROFILE_STAGES='15s@1 warm-up; 30s@1; 30s@20; 1m@20; 30s@1'; WARMUP_SECONDS=15; MAX_VUS=20 ;;
  contention) PROFILE_STAGES='10s@8; 20s@8'; WARMUP_SECONDS=0; MAX_VUS=8 ;;
  *) PROFILE_STAGES='unknown'; WARMUP_SECONDS=unknown; MAX_VUS=unknown ;;
esac
{
  echo "run_id=$RUN_ID"
  echo "commit=$(git -C "$ROOT_DIR" rev-parse HEAD)"
  echo "working_tree_state=clean"
  echo "scenario=$SCENARIO"
  echo "profile=$PROFILE"
  echo "base_url=$BASE_URL"
  echo "readiness_url=$READINESS_URL"
  echo "readiness_path=$READINESS_PATH"
  echo "k6_image=$K6_IMAGE"
  echo "k6_image_digest=$(docker image inspect "$K6_IMAGE" --format '{{index .RepoDigests 0}}' 2>/dev/null || echo unknown)"
  echo "backend_jar_sha256=$(shasum -a 256 "$ROOT_DIR"/build/libs/*.jar 2>/dev/null | head -1 | cut -d ' ' -f1 || echo unknown)"
  echo "load_profile=$PROFILE"
  echo "profile_stages=$PROFILE_STAGES"
  echo "warmup_stage_seconds=$WARMUP_SECONDS"
  echo "max_vus=$MAX_VUS"
  echo "fixture_scale=$FIXTURE_SCALE"
  echo "os=$(uname -srmo)"
  echo "cpu=$(sysctl -n hw.ncpu 2>/dev/null || getconf _NPROCESSORS_ONLN 2>/dev/null || echo unknown)"
  echo "memory_bytes=$(sysctl -n hw.memsize 2>/dev/null || awk '/MemTotal/ {print $2 * 1024}' /proc/meminfo 2>/dev/null || echo unknown)"
  echo "jvm_options=${JAVA_TOOL_OPTIONS:-${JAVA_OPTS:-unset}}"
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

curl --fail --silent --show-error --max-time 5 "$READINESS_URL$READINESS_PATH" >/dev/null \
  || { echo "performance backend is not ready at $BASE_URL; refusing to start k6" >&2; exit 1; }

docker run --rm \
  --add-host=host.docker.internal:host-gateway \
  --user "$(id -u):$(id -g)" \
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

test -s "$OUT_DIR/summary.json" \
  || { echo "k6 summary is missing or empty: $OUT_DIR/summary.json" >&2; exit 1; }
validate_json "$OUT_DIR/summary.json" \
  || { echo "k6 summary is not valid JSON: $OUT_DIR/summary.json" >&2; exit 1; }
test -s "$OUT_DIR/console.log" \
  || { echo "k6 console log is missing or empty: $OUT_DIR/console.log" >&2; exit 1; }
test -s "$OUT_DIR/resources.tsv" \
  || { echo "resource log is missing or empty: $OUT_DIR/resources.tsv" >&2; exit 1; }

if [[ "$SCENARIO" == feed-read ]]; then
  "$ROOT_DIR/scripts/performance/explain-feed.sh" "$OUT_DIR"
  cp "$OUT_DIR/explain-first.json" "$OUT_DIR/explain.json"
  echo "explain_status=recorded" >> "$OUT_DIR/metadata.txt"
else
  echo "explain_status=not_applicable" >> "$OUT_DIR/metadata.txt"
fi

if command -v sha256sum >/dev/null 2>&1; then
  sha256sum "$OUT_DIR"/{summary.json,console.log,resources.tsv,metadata.txt} > "$OUT_DIR/checksums.sha256"
else
  shasum -a 256 "$OUT_DIR"/{summary.json,console.log,resources.tsv,metadata.txt} > "$OUT_DIR/checksums.sha256"
fi

echo "Performance result: $OUT_DIR"
