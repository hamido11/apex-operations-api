# Assessment Evidence

Genuine, captured evidence for each required item. Nothing here is fabricated. Items that have not
actually been executed are marked **NOT VERIFIED**.

> Local demo note: on the machine used to capture this evidence, host ports 8080 and 3000 were
> already taken by unrelated services, so the API was exercised on **8081** and Grafana on **3001**
> via `API_PORT` / `GRAFANA_PORT`. The committed defaults remain 8080 / 9090 / 3000 for a clean
> environment. This is a host-port remap only; nothing else differs.

| # | Required evidence | Status | File / location |
|---|---|---|---|
| 1 | Java 21 version | ✅ VERIFIED | `evidence/phase2-application/java-and-maven-version.txt` |
| 2 | Successful Maven build | ✅ VERIFIED | build `BUILD SUCCESS`; `evidence/phase2-application/` |
| 3 | Successful automated tests (11 IT + 3 unit) | ✅ VERIFIED | `evidence/phase2-application/com.apex.*.txt` |
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
| 21 | GitHub Actions successful run | ⛔ NOT VERIFIED | requires `git push`; workflow authored at `.github/workflows/ci.yml` |
| 22 | GitHub Actions blocked release after failure | ⛔ NOT VERIFIED | requires triggering `.github/workflows/failure-gate.yml` after push |
| 23 | GHCR published image tags | ⛔ NOT VERIFIED | requires `git push` to main/tag; publish job authored |
| 24 | Trivy running inside CI | ⛔ NOT VERIFIED | runs on GitHub runner after push (verified locally, item 14) |
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

## How to verify the CI items (21–24)

1. Create a GitHub repository and push this code (see README).
2. Open the **Actions** tab: the `CI` workflow runs `build-test` → `image-scan` → `publish`.
3. Manually run the **Failure Gate Demo** workflow: `required-check` fails and `release` is **skipped**.
4. Confirm the published image under the repo's **Packages** (GHCR) with `sha-<commit>` tags.
