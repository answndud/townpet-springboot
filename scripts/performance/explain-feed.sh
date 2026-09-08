#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
DB_PORT="${TOWNPET_PERF_DB_PORT:-54331}"
DB_NAME="${TOWNPET_PERF_DB_NAME:-townpet_perf}"
DB_USER="${TOWNPET_PERF_DB_USERNAME:-townpet_perf}"
DB_PASSWORD="${TOWNPET_PERF_DB_PASSWORD:-townpet_perf_local}"
OUT_DIR="${1:-$ROOT_DIR/build/performance/runs/$(date -u +%Y%m%dT%H%M%SZ)-feed-plan}"

mkdir -p "$OUT_DIR"
test -z "$(git -C "$ROOT_DIR" status --porcelain)" \
  || { echo "clean working tree required before performance evidence" >&2; exit 1; }

export TOWNPET_QUERY_PLAN_JDBC_URL="${TOWNPET_QUERY_PLAN_JDBC_URL:-jdbc:postgresql://localhost:${DB_PORT}/${DB_NAME}}"
export TOWNPET_QUERY_PLAN_DB_USER="${TOWNPET_QUERY_PLAN_DB_USER:-$DB_USER}"
export TOWNPET_QUERY_PLAN_DB_PASSWORD="${TOWNPET_QUERY_PLAN_DB_PASSWORD:-$DB_PASSWORD}"
export TOWNPET_QUERY_PLAN_ARTIFACT="$OUT_DIR"

(cd "$ROOT_DIR" && ./gradlew integrationTest \
  --tests '*CommunityFeedQueryPlanIntegrationTest' --rerun-tasks --no-daemon)

test -s "$OUT_DIR/query.sql"
test -s "$OUT_DIR/binds.txt"
test -s "$OUT_DIR/query-plan-metadata.txt"
test -s "$OUT_DIR/explain-first.json"
test -s "$OUT_DIR/explain-next.json"
printf 'explain_status=recorded\n' > "$OUT_DIR/status.txt"
echo "Feed query plan artifact: $OUT_DIR"
