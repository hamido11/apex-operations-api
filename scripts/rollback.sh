#!/usr/bin/env bash
# Roll back to a previous image. With no argument, rolls back to the last recorded
# previous version. Failure injection is forced OFF on rollback. The rollback is only
# reported as successful if the recovered service actually passes health/smoke checks.
#
# Usage: ./scripts/rollback.sh [image-ref]
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT_DIR}"
STATE_DIR="${ROOT_DIR}/.deploy-state"

TARGET="${1:-}"
if [ -z "${TARGET}" ]; then
  if [ -f "${STATE_DIR}/previous" ]; then
    TARGET="$(cat "${STATE_DIR}/previous")"
  else
    echo "[rollback] No previous version recorded and no target provided."
    echo "Usage: $0 <image-ref>"
    exit 2
  fi
fi

echo "[rollback] rolling back to ${TARGET}"
APP_SIMULATE_FAILURE=false "${ROOT_DIR}/scripts/deploy.sh" "${TARGET}"
echo "[rollback] SUCCESS: recovered on ${TARGET}"
