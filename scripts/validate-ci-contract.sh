#!/usr/bin/env bash
set -euo pipefail

# Fast, repository-local checks for assumptions shared by GitHub Actions and
# production image builds. This intentionally validates text/configuration only;
# it does not add another application test suite.
repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_root"

python3 - <<'PY'
import json
import re
from pathlib import Path

workflow_dir = Path(".github/workflows")
workflow_files = sorted(workflow_dir.glob("*.yml"))
if not workflow_files:
    raise SystemExit("CI contract failed: no workflow files found")
expected_workflows = {"ci.yml", "release.yml"}
actual_workflows = {path.name for path in workflow_files}
if actual_workflows != expected_workflows:
    raise SystemExit(
        f"CI contract failed: expected workflows={sorted(expected_workflows)} "
        f"actual={sorted(actual_workflows)}"
    )

action_ref = re.compile(r"^\s*-\s+uses:\s+([^@\s]+)@([^\s#]+)")
node_version = re.compile(r"^\s*node-version:\s*['\"]?([0-9]+)", re.MULTILINE)
pnpm_setup = re.compile(r"^\s*-\s+uses:\s+pnpm/action-setup@")
setup_node = re.compile(r"^\s*-\s+uses:\s+actions/setup-node@")
docs_ref = re.compile(r"(?:^|[\s\"'])docs/")

for workflow in workflow_files:
    lines = workflow.read_text().splitlines()
    pnpm_setup_seen = False
    for line_number, line in enumerate(lines, 1):
        if re.match(r"^\s{4}runs-on:\s*", line):
            pnpm_setup_seen = False
        match = action_ref.match(line)
        if match and not match.group(1).startswith("./"):
            ref = match.group(2)
            if not re.fullmatch(r"[0-9a-f]{40}", ref):
                raise SystemExit(
                    f"CI contract failed: {workflow}:{line_number} uses a mutable action ref {ref}"
                )
        if pnpm_setup.match(line):
            pnpm_setup_seen = True
        if setup_node.match(line):
            lookahead = "\n".join(lines[line_number - 1 : line_number + 8])
            if "cache: pnpm" in lookahead and not pnpm_setup_seen:
                raise SystemExit(
                    f"CI contract failed: {workflow}:{line_number} caches pnpm before pnpm/action-setup"
                )
        if docs_ref.search(line):
            raise SystemExit(
                f"CI contract failed: {workflow}:{line_number} references ignored docs/"
            )

package = json.loads(Path("frontend/package.json").read_text())
package_manager = package.get("packageManager", "")
pnpm_match = re.fullmatch(r"pnpm@([0-9]+\.[0-9]+\.[0-9]+)(?:\+.*)?", package_manager)
if not pnpm_match:
    raise SystemExit("CI contract failed: frontend/package.json has no pinned pnpm packageManager")
expected_pnpm = pnpm_match.group(1)

workflow_text = "\n".join(path.read_text() for path in workflow_files)
release_text = Path(".github/workflows/release.yml").read_text()
if not re.search(r"^\s+needs:\s+publish\s*$", release_text, re.MULTILINE):
    raise SystemExit("CI contract failed: netcup deploy does not depend on image publish")
if re.search(r"^\s+push:\s*$", release_text, re.MULTILINE):
    raise SystemExit("CI contract failed: release workflow must remain manual")
configured_pnpm = set(re.findall(r"^\s*version:\s*([0-9]+\.[0-9]+\.[0-9]+)\s*$", workflow_text, re.MULTILINE))
if configured_pnpm != {expected_pnpm}:
    raise SystemExit(
        f"CI contract failed: pnpm versions workflow={sorted(configured_pnpm)} package={expected_pnpm}"
    )

ci_node_versions = set(node_version.findall(workflow_text))
docker_node_versions = set(
    re.findall(r"^FROM node:([0-9]+)(?:[.-]|\s)", Path("deploy/Dockerfile.frontend").read_text(), re.MULTILINE)
)
if ci_node_versions != docker_node_versions:
    raise SystemExit(
        f"CI contract failed: Node versions workflow={sorted(ci_node_versions)} docker={sorted(docker_node_versions)}"
    )

print(
    "CI contract valid: pinned actions, pnpm setup/order, ignored-docs boundary, "
    f"pnpm={expected_pnpm}, node={','.join(sorted(ci_node_versions))}"
)
PY
