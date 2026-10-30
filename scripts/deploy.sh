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

# Record the rollback target: the previously deployed image becomes 'previous'.
if [ -f "${STATE_DIR}/current" ]; then
  prev="$(cat "${STATE_DIR}/current")"
  if [ "${prev}" != "${APP_IMAGE}" ]; then
    echo "${prev}" > "${STATE_DIR}/previous"
  fi
fi
echo "${APP_IMAGE}" > "${STATE_DIR}/current"

docker compose up -d

echo "[deploy] verifying health via smoke test..."
if "${ROOT_DIR}/scripts/smoke-test.sh"; then
  echo "[deploy] SUCCESS: ${APP_IMAGE} deployed and verified healthy"
else
  echo "[deploy] FAILURE: ${APP_IMAGE} did not pass health/smoke checks"
  docker compose ps
  docker compose logs api --tail 30 || true
  exit 1
fi
