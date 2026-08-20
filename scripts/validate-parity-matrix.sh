#!/usr/bin/env sh
set -eu

ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
MATRIX="$ROOT_DIR/docs/05-패리티/대조표.yaml"
if [ ! -f "$MATRIX" ]; then
  MATRIX="$ROOT_DIR/docs/parity/matrix.yaml"
fi
if [ ! -f "$MATRIX" ]; then
  echo "parity matrix not found: expected docs/05-패리티/대조표.yaml" >&2
  exit 1
fi

counts="$(awk '
  BEGIN {
    expected_pages = expected_api = expected_verified = -1
    expected_excluded = expected_pending = -1
    section = ""
    in_counts = 0
  }
  /^counts:[[:space:]]*$/ {
    in_counts = 1
    next
  }
  in_counts && /^[^[:space:]][^:]*:/ {
    in_counts = 0
  }
  in_counts && $1 == "pages:" {
    expected_pages = $2 + 0
    next
  }
  in_counts && $1 == "apiRoutes:" {
    expected_api = $2 + 0
    next
  }
  in_counts && $1 == "verified:" {
    expected_verified = $2 + 0
    next
  }
  in_counts && $1 == "excluded:" {
    expected_excluded = $2 + 0
    next
  }
  in_counts && $1 == "pending:" {
    expected_pending = $2 + 0
    next
  }
  /^pages:[[:space:]]*$/ {
    section = "pages"
    next
  }
  /^apiRoutes:[[:space:]]*$/ {
    section = "api"
    next
  }
  /^[^[:space:]][^:]*:/ {
    section = ""
  }
  section != "" && /^[[:space:]]+- path:/ {
    if (section == "pages") {
      actual_pages++
    } else {
      actual_api++
    }
  }
  /^[[:space:]]+spring:[[:space:]]+(verified|pending|excluded)[[:space:]]*$/ {
    status[$2]++
  }
  END {
    printf "%d %d %d %d %d %d %d %d %d %d", \
      expected_pages, expected_api, expected_verified, expected_excluded, expected_pending, \
      actual_pages + 0, actual_api + 0, status["verified"] + 0, status["excluded"] + 0, status["pending"] + 0
  }
' "$MATRIX")"
set -- $counts
expected_pages="$1"
expected_api="$2"
expected_verified="$3"
expected_excluded="$4"
expected_pending="$5"
actual_pages="$6"
actual_api="$7"
verified="$8"
excluded="$9"
pending="${10}"

if [ "$expected_pages" -lt 0 ] || [ "$expected_api" -lt 0 ] || \
  [ "$expected_verified" -lt 0 ] || [ "$expected_excluded" -lt 0 ] || [ "$expected_pending" -lt 0 ]; then
  echo "parity matrix counts block is missing or malformed: $MATRIX" >&2
  exit 1
fi

expected_total=$((expected_pages + expected_api))
actual_total=$((actual_pages + actual_api))
status_total=$((verified + excluded + pending))
mismatch=0

if [ "$expected_pages" -ne "$actual_pages" ]; then
  echo "parity matrix mismatch: pages expected=$expected_pages actual=$actual_pages" >&2
  mismatch=1
fi
if [ "$expected_api" -ne "$actual_api" ]; then
  echo "parity matrix mismatch: apiRoutes expected=$expected_api actual=$actual_api" >&2
  mismatch=1
fi
if [ "$expected_total" -ne "$actual_total" ]; then
  echo "parity matrix mismatch: total expected=$expected_total actual=$actual_total" >&2
  mismatch=1
fi
if [ "$expected_verified" -ne "$verified" ]; then
  echo "parity matrix mismatch: verified expected=$expected_verified actual=$verified" >&2
  mismatch=1
fi
if [ "$expected_excluded" -ne "$excluded" ]; then
  echo "parity matrix mismatch: excluded expected=$expected_excluded actual=$excluded" >&2
  mismatch=1
fi
if [ "$expected_pending" -ne "$pending" ]; then
  echo "parity matrix mismatch: pending expected=$expected_pending actual=$pending" >&2
  mismatch=1
fi
if [ "$status_total" -ne "$actual_total" ]; then
  echo "parity matrix mismatch: status rows expected=$actual_total actual=$status_total" >&2
  mismatch=1
fi
if [ "$mismatch" -ne 0 ]; then
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

echo "parity matrix valid: pages=$actual_pages apiRoutes=$actual_api total=$actual_total verified=$verified excluded=$excluded pending=$pending"
