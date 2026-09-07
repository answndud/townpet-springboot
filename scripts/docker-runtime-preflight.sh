#!/usr/bin/env bash
set -Eeuo pipefail

# Check the host/runtime contract before a Docker-backed local or E2E command.
# The temporary storage probe is removed automatically and never reuses an
# application volume.

failures=0
warnings=0

fail() {
  echo "docker preflight: FAIL: $*" >&2
  failures=$((failures + 1))
}

warn() {
  echo "docker preflight: WARN: $*" >&2
  warnings=$((warnings + 1))
}

require_command() {
  command -v "$1" >/dev/null 2>&1 || fail "required command is missing: $1"
}

require_command docker
require_command java

if command -v docker >/dev/null 2>&1; then
  docker info >/dev/null 2>&1 || fail "Docker daemon is unreachable; start Docker Desktop or the Docker service"
  docker compose version >/dev/null 2>&1 || fail "Docker Compose v2 plugin is unavailable"
fi

if command -v java >/dev/null 2>&1; then
  java_version="$(java -version 2>&1 | sed -n 's/.*version "\([0-9][0-9]*\).*/\1/p' | head -1)"
  [[ "$java_version" == "25" ]] || fail "Java 25 is required, found ${java_version:-unknown}"
fi

if [[ -x ./gradlew ]]; then
  ./gradlew --version >/dev/null 2>&1 || fail "Gradle Wrapper cannot start"
else
  fail "Gradle Wrapper ./gradlew is missing or not executable"
fi

if command -v docker >/dev/null 2>&1 && docker info >/dev/null 2>&1; then
  docker_arch="$(docker info --format '{{.Architecture}}' 2>/dev/null || true)"
  host_arch="$(uname -m)"
  case "$docker_arch:$host_arch" in
    amd64:x86_64|arm64:arm64|aarch64:arm64) ;;
    amd64:arm64|amd64:aarch64) warn "Docker is using linux/amd64 on an ARM host; emulation must be available" ;;
    *) warn "could not establish host/Docker architecture parity: host=$host_arch docker=${docker_arch:-unknown}" ;;
  esac

  memory_bytes="$(docker info --format '{{.MemTotal}}' 2>/dev/null || true)"
  if [[ "$memory_bytes" =~ ^[0-9]+$ ]] && (( memory_bytes < 4294967296 )); then
    warn "Docker memory is below 4 GiB; Testcontainers integration suites may fail to start"
  fi

  storage_image="${TOWNPET_PREFLIGHT_STORAGE_IMAGE:-postgis/postgis:18-3.6}"
  if docker image inspect "$storage_image" >/dev/null 2>&1; then
    storage_available_kib="$(docker run --rm --pull=never --entrypoint sh "$storage_image" -c 'df -Pk /var/lib/postgresql | tail -1' 2>/dev/null | awk '{print $4}')"
    if [[ "$storage_available_kib" =~ ^[0-9]+$ ]]; then
      if (( storage_available_kib < 4194304 )); then
        fail "Docker filesystem has less than 4 GiB available for PostgreSQL/Testcontainers (available_kib=$storage_available_kib); increase Docker Desktop disk or remove unused data"
      fi
    else
      warn "Docker filesystem free space could not be measured with $storage_image"
    fi
  else
    warn "storage probe image is not cached: $storage_image (the first test run may need to pull it)"
  fi
fi

if command -v lsof >/dev/null 2>&1; then
  for port in ${TOWNPET_PREFLIGHT_PORTS:-54330 8080}; do
    if lsof -nP -iTCP:"$port" -sTCP:LISTEN >/dev/null 2>&1; then
      fail "required local port is already listening: $port"
    fi
  done
else
  warn "lsof is unavailable; local port conflicts were not checked"
fi

if (( failures > 0 )); then
  echo "docker preflight: $failures failure(s), $warnings warning(s)" >&2
  exit 1
fi

echo "docker preflight: OK (warnings=$warnings)"
