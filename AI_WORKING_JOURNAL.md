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
