#!/usr/bin/env sh
set -eu

ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
FIXTURE="$ROOT_DIR/migration/fixtures/logical-fixture.yaml"

for scenario in care-owner-accept-complete guest-search moderator-report-review; do
  grep -q "^  - id: $scenario$" "$FIXTURE"
done

sh -n "$ROOT_DIR/scripts/frontend-backend-smoke.sh"
sh -n "$ROOT_DIR/scripts/reset-demo-data.sh"

echo "PR metadata valid: fixture scenarios and helper scripts present"
