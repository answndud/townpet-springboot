#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
for file in \
  "$ROOT_DIR/scripts/performance/prepare.sh" \
  "$ROOT_DIR/scripts/performance/start.sh" \
  "$ROOT_DIR/scripts/performance/seed.sh" \
  "$ROOT_DIR/scripts/performance/run.sh" \
  "$ROOT_DIR/scripts/performance/stop.sh"; do
  test -f "$file" || { echo "missing: $file" >&2; exit 1; }
  bash -n "$file"
done
test -f "$ROOT_DIR/scripts/performance/seed.sql"
for file in "$ROOT_DIR"/loadtest/{common,smoke,public-read,feed-read,member-read}.js; do
  test -f "$file" || { echo "missing: $file" >&2; exit 1; }
done
echo "performance execution scripts are syntactically valid"
