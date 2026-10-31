#!/usr/bin/env bash
# Deploy a specific image via Docker Compose and verify it is healthy.
# The running image is selected by APP_IMAGE; deploy fails loudly if the service
# does not pass health/smoke checks (so a bad release is detected, not published as "ok").
#
# Usage: ./scripts/deploy.sh <image-ref>
#   e.g. ./scripts/deploy.sh apex-operations-api:1.0.0
#        ./scripts/deploy.sh ghcr.io/<owner>/apex-operations-api:sha-<...>
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT_DIR}"

APP_IMAGE="${1:-${APP_IMAGE:-}}"
if [ -z "${APP_IMAGE}" ]; then
  echo "Usage: $0 <image-ref>   e.g. apex-operations-api:1.0.0"
  exit 2
fi
export APP_IMAGE
export APP_SIMULATE_FAILURE="${APP_SIMULATE_FAILURE:-false}"
export API_PORT="${API_PORT:-8080}"
export SMOKE_TIMEOUT="${SMOKE_TIMEOUT:-90}"

STATE_DIR="${ROOT_DIR}/.deploy-state"
mkdir -p "${STATE_DIR}"

echo "[deploy] image=${APP_IMAGE} simulate-failure=${APP_SIMULATE_FAILURE} port=${API_PORT}"

# Remember the current known-good image as a candidate rollback target BEFORE anything changes.
# It is only promoted to 'previous' once the new release is verified healthy (see below), so a
# failed deploy can never leave a bad image recorded as a rollback target.
previous_good=""
if [ -f "${STATE_DIR}/current" ]; then
  previous_good="$(cat "${STATE_DIR}/current")"
fi

# Ensure the EXACT image is available without letting Compose fall back to building the current
# source tree and mislabelling it as ${APP_IMAGE} (compose.yaml has both image: and build:). If the
# image is not present locally, try to pull it; if that also fails, refuse rather than deploy a
# silently-rebuilt artifact masquerading as this version.
if ! docker image inspect "${APP_IMAGE}" >/dev/null 2>&1; then
  echo "[deploy] image ${APP_IMAGE} not present locally; attempting pull..."
  if ! docker pull "${APP_IMAGE}"; then
    echo "[deploy] ERROR: ${APP_IMAGE} is not available locally and cannot be pulled."
    echo "[deploy] Refusing to deploy (Compose would otherwise build current source and label it ${APP_IMAGE})."
    exit 3
  fi
fi

# --no-build: deploy/rollback must run the resolved image above, never build on the fly.
docker compose up -d --no-build

echo "[deploy] verifying health via smoke test..."
if "${ROOT_DIR}/scripts/smoke-test.sh"; then
  # Record rollback state ONLY now that the release is verified healthy.
  if [ -n "${previous_good}" ] && [ "${previous_good}" != "${APP_IMAGE}" ]; then
    echo "${previous_good}" > "${STATE_DIR}/previous"
  fi
  echo "${APP_IMAGE}" > "${STATE_DIR}/current"
  echo "[deploy] SUCCESS: ${APP_IMAGE} deployed and verified healthy"
else
  echo "[deploy] FAILURE: ${APP_IMAGE} did not pass health/smoke checks (rollback state left unchanged)"
  docker compose ps
  docker compose logs api --tail 30 || true
  exit 1
fi
