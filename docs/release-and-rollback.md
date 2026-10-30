# Release & Rollback

## Principles

- Images are **versioned and immutable**. The running image is selected by the `APP_IMAGE`
  environment variable, consumed by `compose.yaml` (`image: ${APP_IMAGE:-apex-operations-api:dev}`).
- CI publishes images tagged with the **commit SHA** (`sha-<commit>`) and, on releases, the
  semantic version. Deployments reference these immutable tags, never a bare `latest`.
- A deploy is only "successful" once the service passes health + smoke checks. A rollback is only
  "successful" once the recovered service passes the same checks — **never faked**.

## Release procedure

```bash
# Build/obtain the image
docker build -t apex-operations-api:1.0.0 .
# or pull a CI-published image: docker pull ghcr.io/<owner>/apex-operations-api:sha-<commit>

# Deploy and verify
./scripts/deploy.sh apex-operations-api:1.0.0
```

`deploy.sh`:
1. records the previously running image as the rollback target (`.deploy-state/previous`),
2. sets `APP_IMAGE` and runs `docker compose up -d`,
3. runs `scripts/smoke-test.sh` (readiness, liveness, API) and fails loudly if unhealthy.

## Rollback procedure

```bash
./scripts/rollback.sh                         # to the last good version
./scripts/rollback.sh apex-operations-api:1.0.0   # to a specific version
```

Rollback forces `APP_SIMULATE_FAILURE=false`, redeploys the target, and verifies recovery.

## Demonstrated scenario (captured in evidence/phase5-release)

1. **Deploy v1** (`apex-operations-api:1.0.0`) → healthy, smoke passes.
2. **Deploy v2** (`apex-operations-api:2.0.0`) → healthy, smoke passes.
3. **Inject controlled failure** (`./scripts/simulate-failure.sh`) → redeploys v2 with
   `APP_SIMULATE_FAILURE=true`; readiness goes **DOWN** (503); deploy verification **fails**
   (failure detected, not silently accepted).
4. **Roll back** (`./scripts/rollback.sh`) → back to v1; readiness **UP**; recovery verified.

> Honesty note: v1 and v2 are the same build tagged twice, to demonstrate image-reference switching
> and rollback mechanics. The failure is injected via a controlled, reversible readiness toggle
> rather than shipping a real defect.

## Application rollback vs. database schema rollback

Rolling back the **application image** is cheap and safe. Rolling back the **database schema** is
not — a dropped/renamed column loses data and may break the running version.

**Strategy used here: backward-compatible, forward-only migrations (expand/contract).**

- `V2__add_operations_priority.sql` adds a `NOT NULL` column **with a default**, so:
  - the new app version works, and
  - the previous app version still works against the new schema (it simply ignores the column).
- Because migrations are additive, rolling the **app** from v2 back to v1 needs **no schema change**.

### Handling destructive migrations

- Prefer expand/contract: (1) add new structure, (2) migrate/dual-write, (3) switch reads,
  (4) stop writing old, (5) drop old **only after** the old app version is fully retired.
- Never auto-undo a destructive migration on rollback. If a bad migration ships, **roll forward**
  with a corrective migration.
- Always back up before destructive changes: `docker compose exec db pg_dump -U apex apex > backup.sql`.
- Flyway is forward-only here by design (no `undo` relied upon), which matches safe production
  practice.
