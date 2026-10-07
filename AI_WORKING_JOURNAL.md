# AI working journal

This is a running record of actual interactions. AI proposals are not represented as manual human decisions. No prompts, tests or commits are fabricated.

## 1 — 2026-10-07: Initial master architecture/implementation prompt
Task/prompt summary: The user supplied the Chubb APAC take-home master prompt: Java 17, Spring Boot 3, Maven, PostgreSQL, Kafka, modular monolith; explicit claim lifecycle, information flow, workload/exposure, validation/concurrency, meaningful tests, Docker, OpenAPI, README/ARCHITECTURE and an honest running AI journal. Time box 3–5 hours. Inspect first and provide ten-part design review before coding. Do not fabricate tests/history or make commits without authorization.

AI recommendation: Domain-focused packages; intent-based REST; SQL reporting; explicit approval before settlement; lightweight transactional outbox with at-least-once caveats.
Accepted: The user subsequently asked to build the proposed project with required tests and structure.
Challenged/refined by AI: Monetary aggregation across markets requires a denomination; use explicitly assumed USD rather than summing unrelated currencies. Add explicit start-review endpoint and targeted request-response endpoint. No Kafka consumer added solely for demonstration.
Manual human changes: None observed.
Result: Empty repository confirmed. Java 21 installed, Maven missing, Docker engine permission denied. No implementation existed at review time.

## 2 — 2026-10-07: Build authorization and checkpoint commits
Prompt summary: User authorized implementation, requested successful build/unit tests, then explicitly requested commits at checkpoints; after interruption asked to continue building.
AI recommendation/accepted scope: Architecture/bootstrap, domain, API/reporting, outbox, tests/deployment/docs checkpoints. Download Maven temporarily and add official Maven Wrapper; compile with Java release 17. Use actual validation evidence.
Challenge: Docker CLI presence is not proof of engine access. Docker/PostgreSQL checks must be reported independently.
Manual human changes: None observed.
Result: Architecture written before code implementation. Maven 3.9.11 downloaded outside repository. Compatibility checked against official Spring/springdoc documentation. Implementation is now proceeding.

## 3 — 2026-10-07: Domain and API milestones
Task: Implement explicit state transitions, persistence, commands and reporting.
AI recommendation: JPA aggregate operations, expected-version checks, typed command records, centralized error DTOs; parameterized/constant SQL aggregates rather than loading all claims.
Accepted: Within authorized design. No new human feedback/manual changes observed.
Challenged by evidence: Maven wrapper generation through PowerShell truncated the unquoted version argument to `3`; wrapper downloaded a nonexistent distribution. Corrected distributionUrl explicitly to 3.9.11. Initial domain compilation passed using temporary Maven; API/reporting compilation also passed.
Result: Architecture/bootstrap commit 882cc43; domain/persistence commit 61c885d. Domain creation, assignment, information flow, approval/settlement and rejection are implemented. No runtime verification claimed yet.

## 4 — 2026-10-07: Durable events and first test execution
Task: Ensure database/Kafka consistency and prove core behavior.
AI recommendation: Persist history/outbox in command transaction, wait for broker acknowledgement before marking publication, leave failed records pending. Keep publisher single-instance and document duplicate/retry limits.
Challenge/refinement: No consumer was invented to claim integration functionality; event payload omits claimant name and free-text evidence. Stop a failed batch rather than attempt every row against an unavailable broker.
Accepted: Authorized MVP implementation; no manual human edits observed.
Validation: Maven Wrapper compilation passed after URL correction. First `mvnw.cmd test` on installed Java 21 passed 18 tests with zero failures/errors. Domain rules, MockMvc APIs, H2-backed application flow/exposure/workload, and Mockito publisher success/failure tests passed. H2 is not PostgreSQL; mocked acknowledgement is not real Kafka delivery. Downloaded Java 17 outside repository for exact runtime validation. Docker engine still reports permission denied.

## 5 — 2026-10-07: Java 17 verification and evidence-based self-review
Task: Verify exact target runtime, transaction boundaries and concurrency; prepare deployment artifacts.
AI recommendation: Add transaction tests outside a test-managed transaction, simulate recorder failure, and run two concurrent database transactions to prove JPA locking. Run a real embedded HTTP server for health, OpenAPI, Swagger and claim create/status.
Challenge/refinement: Separate Spring test contexts must not share a fixed H2 database; changed test datasource name to a random UUID. Added proper 415/406 handling. Applied Google Java Format 1.24.0 from a temporary external tool; no formatter runtime dependency added.
Accepted: Within authorized test/self-review scope. No manual human changes observed.
Validation: `mvnw.cmd clean verify` on Microsoft OpenJDK 17.0.20.1 passed: 23 discovered, 22 executed successfully, 1 explicitly skipped PostgreSQL test, zero failures/errors. Packaged executable JAR produced. Rollback, simultaneous pickup, real HTTP health/OpenAPI/Swagger and business API tests passed. `docker compose config --quiet` passed using a temporary environment password placeholder. `docker compose up --build -d` failed before startup with engine permission denied; no container image build or live Kafka claim is made.
Result: API/reporting checkpoint db443ff; transactional outbox checkpoint adc5fae. Dockerfile, Compose and credential template added; full smoke script prepared. Production limitations and event semantics documented.

## 6 — 2026-10-07: Standalone startup, smoke correction and final documentation
Task: Exercise the actual local application and review deployment/documentation claims.
AI recommendation: Run test-classpath application on port 18080 with explicit test-only configuration; execute the same lifecycle script provided to reviewers. Attempt the opt-in PostgreSQL check rather than implying it passed.
Evidence/challenge: The first standalone invocation did not load the test resource configuration and attempted unavailable localhost PostgreSQL; corrected invocation with explicit `spring.config.additional-location=file:./src/test/resources/`. H2 remains test-only and is not shipped in the executable JAR. The first script reached settlement but miscounted history because PowerShell wrapped the REST JSON array in an outer array; removed that wrapper. The corrected script passed all seven lifecycle commands/history and retrieved exposure/workload.
Accepted: Corrections based on actual failures. No manual human edits or additional user decisions observed.
Validation: Standalone Java 17 application started on 18080; corrected smoke passed. Temporary process stopped afterward. Explicit `-DpostgresIT=true -Dtest=PostgresIntegrationTest test` failed before application testing: no accessible Docker environment. This is an environment-blocked check, not a PostgreSQL success. Default tests rerun to leave accurate green reports. README/architecture now explicitly distinguish H2/HTTP evidence from blocked PostgreSQL/container/Kafka verification. Reviewed requirements, DTO boundaries, allowed transitions, transaction/outbox atomicity, index/pagination design, no PII logs, no committed passwords, and deferred functionality. Workload multi-query snapshot limitation documented.
Result: Test/self-review checkpoint 2173208. Final default test rerun passed on Java 17: 22 passed, one explicit PostgreSQL test skipped, zero failures/errors. Deployment/docs checkpoint completed with Docker artifacts, README, architecture/journal updates and an executable Maven Wrapper. Six logical commits exist; the working tree is clean after committing. Full-stack validation remains blocked as described above.

## 7 — 2026-10-07: User requested continuation; real Kafka verification
Prompt: "continue" after the implemented MVP and validation summary.
Task: Continue remaining runtime verification without expanding business scope.
AI recommendation: Recheck engine access; use Spring Kafka's test-only embedded KRaft broker to verify publication without Docker. Official Spring Kafka testing documentation consulted. PostgreSQL and container verification remain separate.
Evidence/challenge: Docker service is running but named-pipe access still returns permission denied. Rather than treating mocked acknowledgements as delivery proof, added one integration test that commits a claim/outbox, consumes the actual broker message, checks key/event/schema/aggregate version and omitted personal text, then verifies the published timestamp/attempt count and matching history event ID.
Accepted: Within requested continuation/verification scope. No human manual changes observed. Test dependency only; no production runtime framework or consumer added.
Validation: Targeted KafkaIntegrationTest passed on Java 17: one executed, zero failures/errors. Broker uses KRaft on ephemeral local ports and is stopped by test teardown. Full clean verification is being run with this test included; final result will be appended before the checkpoint commit. Docker/Compose/PostgreSQL remain blocked, not falsely marked validated.

Final result: `mvnw.cmd clean verify` passed on Java 17 with 24 discovered tests: 23 passed, one explicit PostgreSQL test skipped, zero failures/errors. Executable JAR repackaged successfully. README and architecture distinguish actual embedded broker delivery from unverified Compose broker configuration. Added a seventh logical checkpoint commit for Kafka verification; no production code changed in this continuation.
