# Troubleshooting

| Symptom | Likely cause | Fix |
|---|---|---|
| `Bind for 0.0.0.0:8080 failed: port is already allocated` | Another process/container uses 8080 | `API_PORT=8081 docker compose up -d` (also `PROMETHEUS_PORT`, `GRAFANA_PORT`) |
| Integration tests fail to start Docker / Testcontainers | Colima socket not discovered | `export DOCKER_HOST="unix://$HOME/.colima/default/docker.sock"` and `export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE="/var/run/docker.sock"` |
| `readiness` returns 503, `db: DOWN` | PostgreSQL unreachable/stopped | `docker compose up -d db`; wait for healthy (see runbook §5) |
| `readiness` 503 with `simulatedFailure: DOWN` | `APP_SIMULATE_FAILURE=true` is set | unset it / redeploy; this flag is for the rollback demo only |
| App exits at startup: `Schema-validation` error | Entity/DB schema mismatch (JPA `validate`) | ensure Flyway migrations ran; do not hand-edit the schema |
| `./mvnw` not executable | lost executable bit | `chmod +x mvnw` |
| Wrong Java version during build | `JAVA_HOME` not set to 21 | `export JAVA_HOME="$(brew --prefix openjdk@21)/libexec/openjdk.jdk/Contents/Home"` |
| Spotless check fails in CI | code not formatted | run `./mvnw spotless:apply` and commit |
| Grafana login fails | non-default password | default is `admin`/`admin` (or your `GRAFANA_PASSWORD`) |

## Diagnosing by correlation id

Each response includes an `X-Request-Id` header, echoed as `traceId` in the JSON logs:

```bash
rid=$(curl -s -D - -o /dev/null localhost:8080/api/v1/operations | awk '/X-Request-Id/{print $2}' | tr -d '\r')
docker compose logs api | grep "$rid"
```

## Worked example: database outage (captured)

See [../evidence/troubleshooting/](../evidence/troubleshooting/):

1. `docker compose stop db` → `GET /actuator/health/readiness` returns **503**, `db: DOWN`.
2. Logs show the root cause chain: `CannotGetJdbcConnectionException` → Hikari
   `Connection is not available, request timed out after 10007ms` → `PSQLException` →
   `UnknownHostException: db` (the DB container was stopped).
3. `docker compose start db` → readiness returns **200 UP**; service recovered automatically.
