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
    *) echo "usage: $0 --scenario smoke|public-read|feed-read|member-read --profile smoke|baseline|calibration|ramp [--base-url URL]" >&2; exit 2 ;;
  esac
done

case "$SCENARIO" in
  smoke|public-read|feed-read|member-read) ;;
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
} > "$OUT_DIR/metadata.txt"

docker run --rm \
  --add-host=host.docker.internal:host-gateway \
  -v "$ROOT_DIR/loadtest:/scripts:ro" \
  -v "$OUT_DIR:/results" \
  -e BASE_URL="$BASE_URL" \
  -e LOAD_PROFILE="$PROFILE" \
  "$K6_IMAGE" run --quiet \
  --summary-export "/results/summary.json" \
  "/scripts/${SCENARIO}.js" | tee "$OUT_DIR/console.log"

echo "Performance result: $OUT_DIR"
