#!/usr/bin/env bash
# Smoke test: verifies the running service answers health/readiness and the API.
# Idempotent (read-only) so it is safe to run repeatedly.
set -euo pipefail

API_PORT="${API_PORT:-8080}"
BASE_URL="${BASE_URL:-http://localhost:${API_PORT}}"
TIMEOUT="${SMOKE_TIMEOUT:-90}"

echo "[smoke] target=${BASE_URL} timeout=${TIMEOUT}s"

# 1) Wait for readiness to report UP (curl -f fails on non-2xx such as 503 DOWN).
deadline=$(( $(date +%s) + TIMEOUT ))
until curl -fsS "${BASE_URL}/actuator/health/readiness" 2>/dev/null | grep -q '"status":"UP"'; do
  if [ "$(date +%s)" -ge "${deadline}" ]; then
    echo "[smoke] FAIL: readiness not UP within ${TIMEOUT}s"
    curl -s "${BASE_URL}/actuator/health/readiness" || true
    echo
    exit 1
  fi
  sleep 2
done
echo "[smoke] readiness UP"

# 2) Liveness UP.
if curl -fsS "${BASE_URL}/actuator/health/liveness" 2>/dev/null | grep -q '"status":"UP"'; then
  echo "[smoke] liveness UP"
else
  echo "[smoke] FAIL: liveness not UP"
  exit 1
fi

# 3) Operations endpoint returns 200.
code="$(curl -s -o /dev/null -w '%{http_code}' "${BASE_URL}/api/v1/operations")"
if [ "${code}" = "200" ]; then
  echo "[smoke] GET /api/v1/operations -> 200"
else
  echo "[smoke] FAIL: GET /api/v1/operations -> ${code}"
  exit 1
fi

echo "[smoke] PASS"
