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
| 21 | GitHub Actions successful run | ✅ VERIFIED | CI run #5 on `main` succeeded (`build-test` → `image-scan` → `publish`, 10m 38s). See note below — the exact run URL is refreshed after the history-rewrite force-push. |
| 22 | GitHub Actions blocked release after failure | ⛔ NOT VERIFIED | Local proof exists (`failure-gate-demo.txt`, intentional test fails the build); the **CI-level** blocked-release still needs the **Failure Gate Demo** workflow to be run once (never triggered yet). |
| 23 | GHCR published image tags | ✅ VERIFIED | Published to `ghcr.io/hamido11/apex-operations-api`, multi-arch (`linux/amd64`, `linux/arm64`), tag `sha-<commit>` + `latest`. Refreshed to the post-cleanup tag after the force-push. |
| 24 | Trivy running inside CI | ✅ VERIFIED | Trivy image + dependency scan ran in CI run #5's `image-scan` job and gated the pipeline (verified locally too, item 14). |
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

## How to verify the CI items (21–24)

> Items 21/23/24 were confirmed on CI run #5 / the current GHCR package. Because the corporate-email
> history rewrite force-pushes new commit SHAs, a fresh CI run and `sha-<commit>` tag supersede them;
> the links above are refreshed to that run before final submission. Item 22 still needs one manual
> Failure Gate Demo run.

1. Open the **Actions** tab: the `CI` workflow runs `build-test` → `image-scan` → `publish`.
2. Manually run the **Failure Gate Demo** workflow: `required-check` fails and `release` is **skipped**.
3. Confirm the published image under the repo's **Packages** (GHCR) with `sha-<commit>` tags.
