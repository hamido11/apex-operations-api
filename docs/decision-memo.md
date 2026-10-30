# Decision Memo

Concise rationale for the main technical decisions, including trade-offs and what would change
before production.

## Stack choices

- **Java 21 + Spring Boot 3.5 (Maven)** — Java 21 is the required/LTS runtime; Spring Boot gives
  batteries-included Actuator health/metrics, validation, and first-class Testcontainers support,
  which maximizes DevOps signal for minimal application code. Maven with a **wrapper** pins the build
  tool so the build is reproducible regardless of the host's `mvn`/`java`.
- **PostgreSQL + Flyway + JPA** — the brief allows a DB *or* a mock; a real PostgreSQL with Flyway
  migrations tells a stronger, honest operational story (schema versioning, migration rollback
  discussion, DB-outage investigation) while staying simple (one entity). JPA runs in `validate`
  mode so the schema is owned solely by Flyway.
- **Docker Compose** — simplest reproducible multi-service local runtime (API + DB + optional
  monitoring) with health-gated startup ordering and a persistent volume. No orchestrator complexity
  for a local assessment.
- **GitHub Actions** — required; native to the GitHub-hosted repo, free runners, easy GHCR
  integration, and good caching.

## Container & build

- **Multi-stage build** — compile in a full Maven/JDK image, ship only a minimal JRE + the app, for
  a smaller, lower-surface runtime image.
- **Layered jar** — better Docker layer caching (dependencies change rarely; app code often).
- **Non-root user** — reduces blast radius if the container is compromised; verified by a check.
- **Base images pinned by digest** — reproducible and tamper-evident; portable multi-arch (the index
  digest resolves per-platform, so the same Dockerfile builds arm64 locally and amd64 in CI).
- **Versioned images (SHA + semver)** — immutable, traceable deploys; `latest` only on releases.

## Release safety & rollback

- **Gated pipeline** — `publish` depends on test + scan jobs, so failing tests block any release.
  A separate manual failure-gate workflow proves this without leaving a red build in the normal path.
- **`APP_IMAGE`-driven deploy/rollback** — switching a single variable + verifying health is a
  simple, reliable rollback primitive that works identically locally and in CI-published images.
- **Rollback never faked** — scripts exit non-zero unless the recovered service passes health/smoke.

## Security trade-offs

- `health.show-details: always` is enabled for a clear demo; production should set `when-authorized`.
- Trivy gates on **fixable CRITICAL** (image) and is **report-only** for dependencies, balancing a
  real security gate against pipeline stability from unfixable upstream CVEs.
- Local secrets are plain dev defaults (documented); production uses a managed secret store.

## Operational trade-offs

- A single Compose host and a single API replica — adequate for the assessment; production would run
  multiple replicas behind a load balancer (the documented AWS/ECS path).
- The optional Prometheus/Grafana stack is behind a Compose profile so the core release/rollback path
  stays dependency-free and fast.

## What to improve before production

1. AuthN/AuthZ on the API and on Actuator/Grafana; restrict health detail exposure.
2. Real managed secrets (AWS Secrets Manager) and per-environment config.
3. Multiple replicas + load balancer + autoscaling; DB connection pool sizing tuned to load.
4. Centralized logs/metrics/traces (CloudWatch or an OTel pipeline) with Alertmanager routing.
5. Smaller runtime image (jlink/distroless) and SBOM generation + signing (e.g., cosign).
6. Database backup/restore automation and migration review gates.
