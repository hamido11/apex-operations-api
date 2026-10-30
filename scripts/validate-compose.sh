#!/usr/bin/env bash
# Automated check: the Docker Compose file is syntactically valid and fully resolvable.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT_DIR}"

docker compose config -q
echo "[compose] config is valid"
