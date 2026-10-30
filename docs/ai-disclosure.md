# AI-Use Disclosure

AI assistance is permitted by the assessment and is disclosed here with specific uses, per the
instructions. This document reflects what actually happened during implementation.

> Items marked **[CONFIRM]** must be reviewed/edited by the candidate before submission so the final
> disclosure is accurate to their own experience.

## Tools used

- **Claude Code (Anthropic, Opus 4.8)** — primary assistant for this project. Used in an interactive,
  phase-by-phase session to design the architecture, generate the application and infrastructure
  code and documentation, and to **run the build, tests, Docker, Compose, and demo commands in the
  candidate's local environment** (macOS + Colima) with the candidate's authorization, capturing the
  real output under `evidence/`.

## What was AI-assisted

- Project scaffolding: `pom.xml`, Maven Wrapper setup, package structure.
- Application code: entity/DTO/repository/service/controller, `GlobalExceptionHandler`,
  `CorrelationIdFilter`, `SimulatedFailureHealthIndicator`, `AppProperties`, `application.yml`,
  Flyway migrations.
- Tests: unit test, Testcontainers integration tests, the intentional failure-gate test.
- Containers: `Dockerfile` (multi-stage, non-root, pinned digests, layered jar), `compose.yaml`,
  `.dockerignore`, `.env.example`.
- CI/CD: `ci.yml`, `failure-gate.yml` (including action SHA pinning).
- Scripts: `deploy.sh`, `rollback.sh`, `smoke-test.sh`, `simulate-failure.sh`, `check-nonroot.sh`,
  `validate-compose.sh`.
- Monitoring: Prometheus config, alert rules, Grafana provisioning + dashboard.
- Documentation: this `docs/` set and the `README.md`.

## Commands and tests actually executed (and by whom)

All commands below were executed via Claude Code against the candidate's local machine during the
session, and their genuine output was captured under `evidence/` (not fabricated):

- `./mvnw -B verify` (unit + integration tests) and `./mvnw -Pfailure-gate test` (intentional fail).
- `docker build`, `docker run --entrypoint id` (non-root check), `docker compose up/down`,
  `docker compose config -q`.
- API/health/readiness `curl` calls; volume persistence check; structured-log capture.
- Full deploy → bad release → rollback demo; database-outage failure investigation.
- Monitoring profile bring-up (Prometheus target up, Grafana datasource/dashboard provisioned).

**[CONFIRM]** Before the live review, the candidate should personally re-run the key commands (at
minimum `./mvnw verify`, `docker compose up -d --wait`, the deploy/rollback scripts, and the DB
failure investigation) to confirm the results firsthand and be ready to demonstrate them.

## Not yet executed by anyone (as of writing)

- The **GitHub Actions CI runs** (green pipeline, blocked-release-after-failure, Trivy-in-CI, GHCR
  publish) require pushing to a GitHub repository. These are marked **NOT VERIFIED** in
  [assessment-evidence.md](assessment-evidence.md) until the candidate pushes and captures them.

## Design decisions reviewed and understood by the candidate

**[CONFIRM]** The candidate should confirm they understand and can defend the core decisions
summarized in [decision-memo.md](decision-memo.md): the gated pipeline and why `publish` depends on
tests; non-root + digest-pinned images; Flyway `validate` + expand/contract migrations; the
`APP_IMAGE` deploy/rollback mechanism; readiness gating on the DB; and the Trivy gating policy.

## Material modifications after AI generation

**[CONFIRM]** List any code or docs the candidate substantially changed after generation. If the
candidate accepted the generated artifacts largely as-is (after review), state that honestly rather
than claiming independent authorship.
