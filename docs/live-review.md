# Live Review Cheat-Sheet

A practical guide to run, demo, and explain the project in the 30–45 min review.

> Ports: defaults are API `8080`, Prometheus `9090`, Grafana `3000`. If those are busy on your
> machine, override per command, e.g. `API_PORT=8081 GRAFANA_PORT=3001 PROMETHEUS_PORT=9091 ...`.
> Set `DOCKER_HOST="unix://$HOME/.colima/default/docker.sock"` first if your Docker context isn't Colima.

---

## 1. What the app is (10-second version)

A small Spring Boot (Java 21) REST service that stores "operations" in PostgreSQL. It exists to
demonstrate the DevOps path — container, CI/CD, versioned release, health/metrics, failure
investigation, and rollback — not business complexity.

## 2. How a request flows

```
HTTP client
  → Tomcat (:8080 in-container)
  → CorrelationIdFilter        (assigns/propagates X-Request-Id → MDC traceId)
  → DispatcherServlet
  → OperationController         (@Valid request DTO)
  → OperationService           (@Transactional)
  → OperationRepository        (Spring Data JPA → Hibernate → HikariCP)
  → PostgreSQL
  ← OperationResponse (DTO) → JSON
```

- **Validation/errors**: bad input is caught by Bean Validation and `GlobalExceptionHandler`, which
  returns an RFC 7807 `ProblemDetail` (JSON) — never a stack trace to the client.
- **Logs**: every line is JSON (ECS) on stdout and includes the `traceId`, so you can trace one
  request end-to-end.
- **Schema**: Flyway runs `V1`/`V2` at startup; JPA only *validates* the schema (never generates it).
- **Health**: Actuator aggregates indicators into groups — `liveness` (is the app alive?) and
  `readiness` (can it serve traffic? → gated on the DB and the controlled `simulatedFailure` toggle).

## 3. How the DevOps pipeline flows

```
git push / PR → GitHub Actions
   build-test  (lint + unit + Testcontainers integration tests, upload reports)
        ↓ needs
   image-scan  (build image, Trivy image + dependency scan)
        ↓ needs
   publish     (push multi-arch image to GHCR)  ← runs ONLY on push to main/tag, never on PRs
```

If any test or scan fails, `publish` never runs — that's the release gate. The separate
**Failure Gate Demo** workflow proves it on demand.

---

## 4. Run it

```bash
export DOCKER_HOST="unix://$HOME/.colima/default/docker.sock"   # Colima only
# core only:
API_PORT=8081 docker compose up -d --wait
# with monitoring:
API_PORT=8081 PROMETHEUS_PORT=9090 GRAFANA_PORT=3001 docker compose --profile monitoring up -d --wait
docker compose ps
```

Stop: `docker compose --profile monitoring down`  (add `-v` to also wipe the DB volume).

## 5. Verify from the browser

| URL (adjust port) | What you see |
|---|---|
| http://localhost:8081/api/v1/operations | JSON array of operations (GET works in a browser) |
| http://localhost:8081/actuator/health | overall health = UP, with components |
| http://localhost:8081/actuator/health/readiness | readiness (UP; 503 if DB down) |
| http://localhost:8081/actuator/health/liveness | liveness = UP |
| http://localhost:8081/actuator/prometheus | raw metrics Prometheus scrapes |
| http://localhost:9090/targets | Prometheus → target `apex-operations-api` = **UP** |
| http://localhost:3001 | Grafana (admin / admin) → dashboard "Apex Operations API — Overview" |

> A browser can only do GET. For POST use curl (below) or a REST client.

## 6. Verify from the terminal / logs

```bash
B=http://localhost:8081

# Read
curl -s $B/api/v1/operations | python3 -m json.tool
curl -s $B/actuator/health/readiness | python3 -m json.tool

# Create (note the 201 + Location + X-Request-Id header)
curl -i -X POST $B/api/v1/operations -H 'Content-Type: application/json' \
  -d '{"name":"Deploy job","status":"IN_PROGRESS","description":"demo","priority":2}'

# Validation error → RFC 7807 ProblemDetail (400)
curl -s $B/api/v1/operations -H 'Content-Type: application/json' \
  -d '{"name":"","status":"PENDING","priority":0}' | python3 -m json.tool

# Logs (structured JSON). Trace one request by its id:
docker compose logs -f api
docker compose logs api | grep '<X-Request-Id from a response>'
```

## 7. The four demos to show

**A. Health + correlation**
```bash
curl -i $B/api/v1/operations           # grab the X-Request-Id header
docker compose logs api | grep <that-id>   # same id appears as traceId in the logs
```

**B. Failure investigation (DB outage)**
```bash
docker compose stop db
curl -s $B/actuator/health/readiness   # 503, db: DOWN
docker compose logs api | grep -i 'CannotGetJdbcConnection\|UnknownHostException' | tail
docker compose start db                # wait ~10s
curl -s $B/actuator/health/readiness   # back to UP
```
Root cause to say out loud: the app can't open a JDBC connection (Hikari times out at 10s → DNS
`UnknownHostException: db` because the DB container is gone); readiness takes the instance out of
rotation; liveness stays UP so the orchestrator doesn't needlessly kill it.

**C. Release → bad deploy → rollback**
```bash
docker tag apex-operations-api:dev apex-operations-api:1.0.0
docker tag apex-operations-api:dev apex-operations-api:2.0.0
API_PORT=8081 ./scripts/deploy.sh apex-operations-api:1.0.0   # healthy
API_PORT=8081 ./scripts/deploy.sh apex-operations-api:2.0.0   # healthy
API_PORT=8081 ./scripts/simulate-failure.sh                   # readiness DOWN → detected, deploy fails
API_PORT=8081 ./scripts/rollback.sh                           # back to 1.0.0, verified
```

**D. CI release gate** (after pushing to GitHub)
- Actions → run **Failure Gate Demo** → `required-check` fails, `release` is **skipped**.
- A normal push/PR shows the green `build-test` → `image-scan` → `publish` chain.

---

## 8. Talking points (defend the decisions)

- **Why the pipeline is safe**: `publish` `needs` the test + scan jobs, so a red test blocks the
  release. PRs run validation only (no publish creds). Actions are pinned to commit SHAs.
- **Rollback**: a single `APP_IMAGE` switch + health/smoke verification; the script refuses to
  report success unless the recovered service is actually healthy. App-image rollback is safe;
  schema rollback is not — so migrations are backward-compatible (expand/contract) and forward-only.
- **Security**: non-root container, digest-pinned base, only port 8080 exposed, DB internal-only,
  env-based secrets, Trivy scanning. Trivy found real CVEs → I bumped the fixable ones and documented
  the two unfixable (not-exploitable) spring-webmvc CVEs in `.trivyignore` with a review date.
- **Observability**: readiness is gated on the DB; JSON logs carry a `traceId`; metrics exported to
  Prometheus with a Grafana dashboard and alert rules.
- **Reproducibility**: Maven wrapper + digest-pinned images; a clean `git clone` builds and runs from
  the README alone (I verified this — it even caught a `pom.xml` that a global gitignore had excluded).
- **What I'd improve with more time**: AuthN/Z + restricted actuator details, managed secrets,
  multiple replicas behind a load balancer, smaller image (jlink/distroless), SBOM + image signing.
```
