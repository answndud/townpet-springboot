#!/usr/bin/env bash
set -Eeuo pipefail

REPO="${GITHUB_REPOSITORY:-answndud/townpet-springboot}"
BRANCH="main"
DEPLOY="false"

usage() {
  cat <<'EOF'
Usage:
  ./scripts/release-netcup.sh --publish-only
  ./scripts/release-netcup.sh --deploy

The script verifies the local main branch, origin/main, and the matching
Continuous integration run before dispatching Release and deploy.
EOF
}

case "${1:-}" in
  --publish-only) DEPLOY="false" ;;
  --deploy) DEPLOY="true" ;;
  --help|-h) usage; exit 0 ;;
  *) usage >&2; exit 2 ;;
esac

command -v gh >/dev/null || { echo "gh CLI is required" >&2; exit 1; }
command -v git >/dev/null || { echo "git is required" >&2; exit 1; }

[[ "$(git branch --show-current)" == "$BRANCH" ]] || {
  echo "release must be started from $BRANCH" >&2
  exit 1
}
[[ -z "$(git status --porcelain)" ]] || {
  echo "working tree is not clean; commit or stash changes before release" >&2
  exit 1
}

git fetch origin "$BRANCH" --quiet
head_sha="$(git rev-parse HEAD)"
origin_sha="$(git rev-parse "origin/$BRANCH")"
[[ "$head_sha" == "$origin_sha" ]] || {
  echo "local $BRANCH is not synchronized with origin/$BRANCH" >&2
  echo "local=$head_sha origin=$origin_sha" >&2
  exit 1
}

ci_run_id="$(gh run list \
  --repo "$REPO" \
  --workflow ci.yml \
  --branch "$BRANCH" \
  --commit "$head_sha" \
  --limit 1 \
  --json databaseId \
  --jq '.[0].databaseId // empty')"
[[ -n "$ci_run_id" ]] || {
  echo "no CI run exists for $head_sha; wait for Continuous integration first" >&2
  exit 1
}

echo "waiting for CI run $ci_run_id ($head_sha)"
gh run watch "$ci_run_id" --repo "$REPO" --exit-status

echo "dispatching release.yml (deploy=$DEPLOY)"
gh workflow run release.yml \
  --repo "$REPO" \
  --ref "$BRANCH" \
  -f "deploy=$DEPLOY"

release_run_id=""
for _ in {1..15}; do
  release_run_id="$(gh run list \
    --repo "$REPO" \
    --workflow release.yml \
    --branch "$BRANCH" \
    --commit "$head_sha" \
    --event workflow_dispatch \
    --limit 1 \
    --json databaseId \
    --jq '.[0].databaseId // empty')"
  [[ -n "$release_run_id" ]] && break
  sleep 2
done

[[ -n "$release_run_id" ]] || {
  echo "release workflow was dispatched but its run could not be located" >&2
  exit 1
}

echo "watching release run $release_run_id"
gh run watch "$release_run_id" --repo "$REPO" --exit-status
echo "release completed: $release_run_id"
