#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
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
for file in \
  "$ROOT_DIR/scripts/performance/prepare.sh" \
  "$ROOT_DIR/scripts/performance/start.sh" \
  "$ROOT_DIR/scripts/performance/seed.sh" \
  "$ROOT_DIR/scripts/performance/explain-feed.sh" \
  "$ROOT_DIR/scripts/performance/run.sh" \
  "$ROOT_DIR/scripts/performance/stop.sh"; do
  test -f "$file" || { echo "missing: $file" >&2; exit 1; }
  bash -n "$file"
done
test -f "$ROOT_DIR/scripts/performance/seed.sql"
for file in "$ROOT_DIR"/loadtest/{common,smoke,public-read,feed-read,member-read,write,contention,moderator,media,mixed}.js; do
  test -f "$file" || { echo "missing: $file" >&2; exit 1; }
done
loadtest_files=(
  "$ROOT_DIR"/loadtest/common.js
  "$ROOT_DIR"/loadtest/smoke.js
  "$ROOT_DIR"/loadtest/public-read.js
  "$ROOT_DIR"/loadtest/feed-read.js
  "$ROOT_DIR"/loadtest/member-read.js
  "$ROOT_DIR"/loadtest/write.js
  "$ROOT_DIR"/loadtest/contention.js
  "$ROOT_DIR"/loadtest/moderator.js
  "$ROOT_DIR"/loadtest/media.js
  "$ROOT_DIR"/loadtest/mixed.js
)
if command -v rg >/dev/null 2>&1; then
  retired_contract_search=(rg -n 'api/v1/feed|feed/popular|audience=|"scope"|scope:' "${loadtest_files[@]}")
else
  retired_contract_search=(grep -E -n 'api/v1/feed|feed/popular|audience=|"scope"|scope:' "${loadtest_files[@]}")
fi
if "${retired_contract_search[@]}"; then
  echo "loadtest contains a retired HTTP contract" >&2
  exit 1
fi
echo "performance execution scripts are syntactically valid"

if [[ $# -gt 0 ]]; then
  RUN_DIR="$1"
  test -d "$RUN_DIR" || { echo "run directory does not exist: $RUN_DIR" >&2; exit 1; }
  for artifact in summary.json console.log resources.tsv metadata.txt checksums.sha256; do
    test -s "$RUN_DIR/$artifact" || { echo "missing or empty artifact: $RUN_DIR/$artifact" >&2; exit 1; }
  done
  validate_json "$RUN_DIR/summary.json" || { echo "invalid JSON: $RUN_DIR/summary.json" >&2; exit 1; }
  if command -v sha256sum >/dev/null 2>&1; then
    (cd "$RUN_DIR" && sha256sum -c checksums.sha256)
  else
    (cd "$RUN_DIR" && shasum -a 256 -c checksums.sha256)
  fi
  echo "performance run artifacts are valid: $RUN_DIR"
fi
