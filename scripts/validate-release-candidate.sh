#!/usr/bin/env sh
set -eu

ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
"$ROOT_DIR/scripts/validate-parity-matrix.sh"
for scenario in care-owner-accept-complete guest-search moderator-report-review; do
  grep -q "^  - id: $scenario$" "$ROOT_DIR/migration/fixtures/logical-fixture.yaml"
done
sh -n "$ROOT_DIR/scripts/frontend-backend-smoke.sh"
sh -n "$ROOT_DIR/scripts/reset-demo-data.sh"
echo "release candidate metadata valid: fixture scenarios and scripts present"
