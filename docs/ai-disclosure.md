# AI-Use Disclosure

AI assistance is permitted by the assessment and is disclosed here with specific uses, per the
instructions. This document reflects what actually happened during implementation.

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

Before the live review, the candidate intends to personally re-run the key commands (at minimum
`./mvnw verify`, `docker compose up -d --wait`, the deploy/rollback scripts, and the DB failure
investigation) to confirm the results firsthand and be ready to demonstrate them.

## Executed in CI (GitHub Actions)

The pipeline has run for real on GitHub Actions in the candidate's repo
(`github.com/hamido11/apex-operations-api`). After a git-history rewrite to remove a corporate email
address, the work was pushed and opened as **PR #2**; its CI run **succeeded** (`build-test` →
`image-scan`, with `publish` correctly skipped on a pull request — Trivy ran inside `image-scan`).
Screenshots are under `evidence/phase4-ci/`. PR #2 was then **merged to `main`** (`3207d07`), and the
push-to-`main` run executes the `publish` job (multi-arch image to GHCR,
`ghcr.io/hamido11/apex-operations-api`). Early CI runs failed on Trivy vulnerability-DB rate-limiting
and were fixed iteratively (see commit history and [runbook.md](runbook.md) §9). The one CI item
**not yet executed** is the **Failure Gate Demo** workflow (the clean CI-level
blocked-release-after-failure proof); it stays **NOT VERIFIED** in
[assessment-evidence.md](assessment-evidence.md) until run once.

## Design decisions reviewed and understood by the candidate

The candidate has reviewed and can defend the core decisions summarized in
[decision-memo.md](decision-memo.md): the gated pipeline and why `publish` depends on tests; non-root
+ digest-pinned images; Flyway `validate` + expand/contract migrations; the `APP_IMAGE`
deploy/rollback mechanism; readiness gating on the DB; and the Trivy gating policy.

## Peer review and remediation

A second AI assistant performed an independent code review. Its findings were **verified against the
repository** and the real ones fixed: HTTP error-handling status codes (4xx vs 500), a missing
database-outage alert (`app_database_up` gauge + `DatabaseDown` rule), deploy/rollback image-integrity
guards, stale evidence re-captured on the current image, and documentation accuracy. The reviewer's
optional patch was **not applied**; the fixes here were written independently (also AI-assisted via
Claude Code) and verified with `./mvnw verify` plus live container demos.

## Material modifications by the candidate after AI generation

The candidate directed the work phase-by-phase, reviewed outputs at each gate, and made specific
edits including: removing an inaccurate tool reference from an earlier draft of this disclosure, and
authoring `.env.example` directly. The remaining generated artifacts were accepted after review
rather than substantially rewritten — stated honestly here rather than overclaiming independent
authorship.
