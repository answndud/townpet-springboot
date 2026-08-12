#!/usr/bin/env sh
set -eu

ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
MATRIX="$ROOT_DIR/docs/parity/matrix.yaml"

counts="$(awk '/spring: (verified|pending|excluded)/ { count[$2]++ } END { printf "%d %d %d", count["verified"], count["pending"], count["excluded"] }' "$MATRIX")"
set -- $counts
verified="$1"
pending="$2"
excluded="$3"
total=$((verified + pending + excluded))

if [ "$total" -ne 104 ]; then
  echo "parity matrix must contain 104 page/API rows; got $total" >&2
  exit 1
fi
if [ "$pending" -ne 0 ]; then
  echo "parity matrix contains pending rows" >&2
  exit 1
fi
if grep -q '^  uncovered: \[' "$MATRIX" && ! grep -q '^  uncovered: \[\]$' "$MATRIX"; then
  echo "parity matrix has uncovered evidence" >&2
  exit 1
fi

echo "parity matrix valid: total=$total verified=$verified excluded=$excluded pending=$pending"
