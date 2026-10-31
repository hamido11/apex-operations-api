# Assessment Evidence

Genuine, captured evidence for each required item. Nothing here is fabricated. Items that have not
actually been executed are marked **NOT VERIFIED**.

> Local demo note: on the machine used to capture this evidence, host ports 8080 and 3000 were
> already taken by unrelated services, so the API was exercised on **8081** and Grafana on **3001**
> via `API_PORT` / `GRAFANA_PORT`. The committed defaults remain 8080 / 9090 / 3000 for a clean
> environment. This is a host-port remap only; nothing else differs.
>
> All runtime evidence below was (re-)captured on **2026-10-31** against the current image
> (embedded **Tomcat 10.1.61**, i.e. after the security remediation — confirmed by
> `evidence/phase5-release/deployed-tomcat-version.txt`), so the deploy/rollback, troubleshooting and
> monitoring captures match the exact artifact being submitted.

| # | Required evidence | Status | File / location |
|---|---|---|---|
| 1 | Java 21 version | ✅ VERIFIED | `evidence/phase2-application/java-and-maven-version.txt` |
| 2 | Successful Maven build | ✅ VERIFIED | `evidence/phase2-application/build-success.txt` — full `./mvnw -B clean verify`, ends in `BUILD SUCCESS` |
| 3 | Successful automated tests (16 integration + 5 unit = 21) | ✅ VERIFIED | `build-success.txt` (surefire 5, failsafe 16, 0 failures/errors) |
| 4 | Test reports | ✅ VERIFIED | `evidence/phase2-application/*.txt` (surefire/failsafe) |
| 5 | Intentional failed test detected | ✅ VERIFIED | `evidence/phase2-application/failure-gate-demo.txt` (exit 1, BUILD FAILURE) |
| 6 | Docker image build | ✅ VERIFIED | multi-stage build; `evidence/phase3-containers/image-size.txt` |
| 7 | Non-root execution | ✅ VERIFIED | `evidence/phase3-containers/nonroot-uid.txt`, `check-nonroot-script.txt` (uid 10001) |
| 8 | Docker Compose service status | ✅ VERIFIED | `evidence/phase3-containers/compose-ps.txt` (both healthy) |
| 9 | Compose config validation | ✅ VERIFIED | `evidence/phase3-containers/compose-config-valid.txt` |
| 10 | API response | ✅ VERIFIED | `evidence/phase3-containers/api-responses.txt` (incl. `X-Request-Id`) |
| 11 | Health/readiness response | ✅ VERIFIED | `api-responses.txt`, `evidence/phase5-release/05-post-rollback-readiness.txt` |
| 12 | Database persistence | ✅ VERIFIED | `evidence/phase3-containers/persistence-after-restart.txt` (survives recreate) |
| 13 | Structured JSON logs + correlation id | ✅ VERIFIED | `evidence/phase3-containers/structured-json-logs-sample.txt` (`traceId`) |
| 14 | Trivy scan output | ✅ VERIFIED (local) | `evidence/phase4-ci/trivy-image-scan-BEFORE-fix.txt` / `-AFTER-fix.txt` / `trivy-critical-gate.txt` |
| 15 | Successful deployment | ✅ VERIFIED | `evidence/phase5-release/01-deploy-v1.txt`, `02-deploy-v2.txt` |
| 16 | Controlled failure / failed deploy detected | ✅ VERIFIED | `evidence/phase5-release/03-simulate-failure.txt` (readiness DOWN) |
| 17 | Successful rollback | ✅ VERIFIED | `evidence/phase5-release/04-rollback.txt` |
| 18 | Recovery verification | ✅ VERIFIED | `evidence/phase5-release/05-post-rollback-readiness.txt` (UP) |
| 19 | Failure investigation (DB outage) | ✅ VERIFIED | `evidence/troubleshooting/` (readiness DOWN → root cause in logs → recovered) |
| 20 | Monitoring (Prometheus + Grafana) | ✅ VERIFIED | `evidence/phase5-monitoring/` (target up, datasource + dashboard) |
| 21 | GitHub Actions successful run | ✅ VERIFIED | PR #2 CI run **succeeded** (`build-test` → `image-scan`; `publish` correctly skipped on a PR) — see `evidence/phase4-ci/ci-pr-all-checks-passed.png` and `ci-pr-run-summary-gated-pipeline.png`. Merged to `main` as `3207d07`, which triggers the push-to-`main` run that also runs `publish`. |
| 22 | GitHub Actions blocked release after failure | ⛔ NOT VERIFIED | Local proof exists (`failure-gate-demo.txt`, intentional test fails the build); the **CI-level** blocked-release still needs the **Failure Gate Demo** workflow to be run once (never triggered yet). |
| 23 | GHCR published image tags | ⚠ CAPTURE AFTER MERGE | `publish` runs only on push to `main`/tags, so it is **skipped** in the PR screenshots (correct). PR #2 is now merged (`main` = `3207d07`), so the push-to-`main` run executes `publish` → `ghcr.io/hamido11/apex-operations-api`, multi-arch (`linux/amd64`, `linux/arm64`), `sha-<commit>` + `latest`. **Attach the main-push run's `publish` job + the GHCR Packages page** to close this item. |
| 24 | Trivy running inside CI | ✅ VERIFIED | The **Build image & Trivy scan** job runs in CI — green in `evidence/phase4-ci/ci-pr-all-checks-passed.png` / `ci-pr-run-summary-gated-pipeline.png`; a run where it **failed** (and `publish` stayed skipped) is in `ci-pr-scan-failed-publish-skipped.png`. Also verified locally (item 14). |
| 25 | Clean-environment reproduction | ✅ VERIFIED | `evidence/phase6-repro/` — fresh `git clone` → `./mvnw spotless:check verify` (BUILD SUCCESS) → image build → isolated stack healthy with Flyway-seeded data |

> Reproduction caught a real defect: the candidate's **global** gitignore (`~/.gitignore_global`)
> contained `*.xml`, which had silently excluded `pom.xml` from the first commit. Fixed by
> force-adding `pom.xml` and adding `!pom.xml` to the repo `.gitignore`, so the repo is self-contained
> on any machine. This is why the clean-clone test matters.

## Security remediation evidence (highlight)

Trivy initially found **6 CRITICAL + 6 HIGH** fixable/known CVEs (`trivy-image-scan-BEFORE-fix.txt`).
Remediation:
- Bumped `tomcat.version → 10.1.61`, `jackson-bom.version → 2.21.8`, `postgresql.version → 42.7.14`
  via Spring Boot-managed property overrides in `pom.xml` (clears the fixable CRITICAL/HIGH).
- The only remaining CRITICALs (`CVE-2026-47884`, `CVE-2026-47890` in spring-webmvc) have **no fix on
  the Spring Framework 6.2.x line** and are **not exploitable** here (no XSLT/SSE MVC views); they are
  documented in `.trivyignore` with justification and a review date.

Result: the CI CRITICAL gate passes with **0 unaccepted findings**
(`trivy-critical-gate.txt`, exit 0; `trivy-image-scan-AFTER-fix.txt`, clean).

## Review-driven hardening (2026-10-31)

A peer review flagged defects that were verified, fixed, and re-evidenced:

| Area | Fix | Evidence |
|---|---|---|
| **Error handling** — a non-numeric id, unknown path, wrong method or wrong content type previously returned **HTTP 500** | `GlobalExceptionHandler` now extends `ResponseEntityExceptionHandler`, so Spring MVC's own exceptions map to the correct 4xx (400/404/405/415). Five new tests added. | `evidence/phase2-application/error-handling-http-status.txt` (live service: 400/404/405/415, each an RFC 7807 ProblemDetail) + `OperationControllerIT` |
| **DB-outage alerting** — no alert caught the database outage (`ApiInstanceDown` can't: the app stays scrapable) | Added `app_database_up` gauge (scheduled connectivity probe) + a `DatabaseDown` Prometheus alert | `evidence/phase5-monitoring/db-outage-alert-firing.json` (DatabaseDown **firing**), `alert-contrast.txt` (ApiInstanceDown **inactive** during the same outage), `evidence/troubleshooting/app-database-up-{down,recovered}.txt` |
| **Deploy/rollback integrity** — Compose could silently build current source and mislabel it as the target version; a failed deploy could become a rollback target | `deploy.sh` now refuses an absent image (pull-or-fail, `--no-build`) and records rollback state only after health passes | `evidence/phase5-release/01-05*.txt` re-captured on the fixed image |
| **Stale evidence** — deploy/rollback logs were from the pre-fix build (Tomcat 10.1.55) | Re-ran the full release demo on the current image | `evidence/phase5-release/deployed-tomcat-version.txt` → `Apache Tomcat/10.1.61` |

## CI screenshots (GitHub Actions)

Captured from the real repository (`github.com/hamido11/apex-operations-api`), PR #2:

| File | Shows |
|---|---|
| `evidence/phase4-ci/ci-pr-all-checks-passed.png` | Green PR run on `bab4ef1`: **Build, lint & test** ✅ + **Build image & Trivy scan** ✅; **Publish to GHCR** skipped (correct — publish never runs on a PR). |
| `evidence/phase4-ci/ci-pr-run-summary-gated-pipeline.png` | Run summary: the gated pipeline `build-test → image-scan → publish`, `publish` skipped on the `pull_request` event. |
| `evidence/phase4-ci/ci-pr-scan-failed-publish-skipped.png` | An earlier PR run where the **Trivy scan step** of the `image-scan` job failed (`build-test` stayed green) — shown as honest CI history. See [runbook.md](runbook.md) §9 for the common transient Trivy-DB rate-limit; re-running the job went green (`ci-pr-all-checks-passed.png`). |

> **Honest reading of the failing-scan shot:** on a pull request, `publish` is skipped *regardless* of
> the scan result (it is guarded by `if: event != pull_request`), so this image does **not** by itself
> prove "a failure blocked the release." It only shows a red scan surfacing as a red check. The clean
> proof of the release gate is item 22 — the **Failure Gate Demo** workflow — which is still **NOT
> VERIFIED** until run once.

## How to close the remaining CI items (22–23)

The corporate-email rewrite is complete: merged `main` (`3207d07`) contains **0** occurrences of the
old corporate address (every commit is `…@users.noreply.github.com`).

1. **Item 23 (GHCR publish):** PR #2 is merged, so the push-to-`main` run executes `publish`. Open
   **Actions → the `main` run → `Publish image to GHCR` job**, and the repo **Packages** page; attach
   both. The new image tag is `sha-<the merge/commit SHA>`.
2. **Item 22 (blocked release after failure):** **Actions → Failure Gate Demo → Run workflow.**
   `required-check` fails and the dependent `release` job is **skipped** — the clean gate proof.
