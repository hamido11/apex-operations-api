#!/usr/bin/env bash
# Simulate a controlled, reversible bad release: re-deploy the current image with
# APP_SIMULATE_FAILURE=true, which forces the readiness probe DOWN. The deploy verification
# is expected to FAIL, demonstrating that a bad release is detected before it is accepted.
#
# Usage: ./scripts/simulate-failure.sh [image-ref]
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT_DIR}"
API_PORT="${API_PORT:-8080}"

IMAGE="${1:-$(cat "${ROOT_DIR}/.deploy-state/current" 2>/dev/null || echo "apex-operations-api:dev")}"

echo "[simulate-failure] re-deploying ${IMAGE} with APP_SIMULATE_FAILURE=true (controlled bad release)"

set +e
APP_SIMULATE_FAILURE=true SMOKE_TIMEOUT="${SMOKE_TIMEOUT:-45}" \
  "${ROOT_DIR}/scripts/deploy.sh" "${IMAGE}"
rc=$?
set -e

if [ "${rc}" -ne 0 ]; then
  echo "[simulate-failure] EXPECTED: deploy verification failed (readiness DOWN)."
  echo "[simulate-failure] current readiness response:"
  curl -s "http://localhost:${API_PORT}/actuator/health/readiness" || true
  echo
  echo "[simulate-failure] recover with: ./scripts/rollback.sh"
  exit 0
fi

echo "[simulate-failure] UNEXPECTED: deploy reported healthy despite failure injection."
exit 1
