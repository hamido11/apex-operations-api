#!/usr/bin/env bash
# Automated check: the image must run as a non-root user.
# Usage: ./scripts/check-nonroot.sh [image-ref]
set -euo pipefail

IMAGE="${1:-apex-operations-api:dev}"
uid="$(docker run --rm --entrypoint id "${IMAGE}" -u)"

if [ "${uid}" = "0" ]; then
  echo "[nonroot] FAIL: ${IMAGE} runs as root (uid 0)"
  exit 1
fi
echo "[nonroot] PASS: ${IMAGE} runs as non-root (uid=${uid})"
