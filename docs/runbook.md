# Runbook

Operational procedures for the Apex Operations API. All commands are run from the repository root.
Defaults assume host port 8080; override with `API_PORT` if that port is busy.

> Colima users: `export DOCKER_HOST="unix://$HOME/.colima/default/docker.sock"` before Docker/Compose
> commands if the default context is not Colima.

## 1. Initial deployment

```bash
cp .env.example .env                         # optional; Compose has built-in defaults
docker build -t apex-operations-api:1.0.0 -t apex-operations-api:dev .
./scripts/deploy.sh apex-operations-api:1.0.0
```

`deploy.sh` brings the stack up, waits for health, and runs the smoke test. Success is printed only
if health/smoke checks pass.

## 2. Subsequent releases

```bash
# New version by local tag...
docker build -t apex-operations-api:2.0.0 .
./scripts/deploy.sh apex-operations-api:2.0.0

# ...or a published, immutable image from CI:
./scripts/deploy.sh ghcr.io/<owner>/apex-operations-api:sha-<commit>
```

The previously deployed image is recorded as the rollback target in `.deploy-state/previous`.

## 3. Health verification

```bash
curl -s localhost:8080/actuator/health | python3 -m json.tool
curl -s localhost:8080/actuator/health/readiness
curl -s localhost:8080/actuator/health/liveness
docker compose ps                 # STATUS should show (healthy)
./scripts/smoke-test.sh           # readiness + liveness + API 200
```

## 4. Rollback

```bash
./scripts/rollback.sh                       # to last recorded previous version
./scripts/rollback.sh apex-operations-api:1.0.0   # to an explicit version
```

Rollback forces `APP_SIMULATE_FAILURE=false` and verifies the recovered service; it exits non-zero
(no false success) if recovery fails. See [release-and-rollback.md](release-and-rollback.md) for the
distinction between application-image rollback and database-schema rollback.

## 5. Database recovery

If PostgreSQL is down (readiness reports `db: DOWN`):

```bash
docker compose ps                 # is db running/healthy?
docker compose logs db --tail 50  # inspect DB logs
docker compose start db           # or: docker compose up -d db
# wait for healthy, then:
curl -s localhost:8080/actuator/health/readiness   # expect UP
```

Data persists in the `pgdata` volume across container restarts/recreation. **Do not** run
`docker compose down -v` unless you intend to delete the database volume. Back up with:

```bash
docker compose exec db pg_dump -U apex apex > backup.sql
```

## 6. API troubleshooting

```bash
docker compose logs api --tail 100            # structured JSON logs
docker compose logs api | grep '"level":"ERROR"'
# correlate a client request by its X-Request-Id:
docker compose logs api | grep '<request-id-from-response-header>'
```

See [troubleshooting.md](troubleshooting.md) for specific symptoms.

## 7. Container troubleshooting

```bash
docker compose ps
docker inspect --format '{{json .State.Health}}' apex-operations-api-1 | python3 -m json.tool
docker stats --no-stream
docker run --rm --entrypoint id apex-operations-api:1.0.0 -u   # confirm non-root (10001)
```

## 8. Log inspection

Logs are structured JSON (ECS) on stdout. Useful filters:

```bash
docker compose logs -f api                                  # follow
docker compose logs api | grep '"logger":"com.apex.operations'  # app logs only
```

## 9. Incident response (quick checklist)

1. **Assess**: `docker compose ps`; check `/actuator/health` and `/actuator/health/readiness`.
2. **Identify blast radius**: is it the API (liveness down), a dependency (readiness down, db DOWN),
   or a bad release (just deployed)?
3. **Mitigate first**:
   - Bad release → `./scripts/rollback.sh` (fastest safe action).
   - DB down → restart DB (section 5); the API auto-recovers once the pool reconnects.
   - Resource exhaustion → `docker stats`, check Hikari pool / JVM heap metrics.
4. **Diagnose**: inspect logs by `traceId`; capture the error and the readiness JSON.
5. **Verify recovery**: readiness UP, smoke test passes.
6. **Record**: note timeline, root cause, and follow-ups. For secret exposure, treat as a security
   incident (see [security.md](security.md)): rotate the credential, invalidate sessions, review
   access, and purge the secret from history.
