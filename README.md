# Chubb APAC claims backend assessment

A small modular monolith replacing shared inbox/spreadsheet claim handling with explicit, traceable workflows. Claimants submit, track and answer information requests; officers pick up, review, approve/settle or reject claims; managers retrieve workload and outstanding liability. Frontend is out of scope.

## Stack

Java 17, Spring Boot 3.5.7, Maven 3.9.11 Wrapper, Spring Web/Validation/Data JPA, PostgreSQL 16, Flyway, Spring Kafka, OpenAPI/Swagger, Actuator, JUnit 5/Mockito, Docker Compose. Springdoc 2.8.x is compatible with Boot 3.5.x ([official matrix](https://springdoc.org/v2/)). Kafka uses the [official Apache JVM image](https://kafka.apache.org/39/getting-started/docker/).

## Prerequisites and build

- JDK **17** and `JAVA_HOME` pointing to it; no global Maven installation required.
- Internet on the first build for Maven/dependencies.
- Docker Engine/Desktop with **Linux containers**, working engine access and Docker Compose v2 or later for the full stack.
- Available localhost ports 8080, 5432 and 9092.

Windows PowerShell:

```powershell
.\mvnw.cmd clean verify
.\mvnw.cmd test
```

Linux/macOS:

```bash
chmod +x mvnw
./mvnw clean verify
./mvnw test
```

The default suite runs domain, database-backed H2 application/API, transaction/concurrency, real HTTP smoke, mocked publisher failure-path tests and an **embedded Kafka KRaft integration test**. The broker integration needs no Docker: it verifies committed outbox publication, actual message consumption and the published marker. It does not validate the Compose broker configuration. H2 is **test-only**, PostgreSQL mode; it is not proof of PostgreSQL compatibility. The explicit PostgreSQL/Testcontainers test requires Docker and fails if the engine is unavailable:

```powershell
.\mvnw.cmd "-DpostgresIT=true" "-Dtest=PostgresIntegrationTest" test
```

For Bash, use `./mvnw -DpostgresIT=true -Dtest=PostgresIntegrationTest test`. The PostgreSQL test is deliberately skipped in the default suite and reported as such.

## Run the complete local stack

First copy the credential template and choose a local password (the `.env` file is Git-ignored):

```powershell
Copy-Item .env.example .env
# Edit DB_PASSWORD in .env
docker compose up --build
```

On Linux/macOS use `cp .env.example .env`. Compose starts PostgreSQL and a single Kafka KRaft broker, explicitly creates the lifecycle topic, and then starts the application. The multi-stage Docker build runs the default tests with Java 17. Published ports bind to loopback. PostgreSQL data persists in a named volume; Kafka storage is ephemeral for this local demonstration.

- Health: http://localhost:8080/actuator/health
- Swagger UI: http://localhost:8080/swagger-ui/index.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

```powershell
docker compose logs -f app
docker compose down
```

`docker compose down` preserves the database. Changing the password after initial database creation also requires changing the stored PostgreSQL user's password; modifying `.env` alone does not do that.

## Run the JVM application against local dependencies

```powershell
docker compose up -d postgres kafka kafka-init
$env:DB_PASSWORD = '<same value as .env>'
.\mvnw.cmd spring-boot:run
```

Alternatively run `java -jar target/claims-0.0.1-SNAPSHOT.jar` after packaging. Bash: `DB_PASSWORD='<local password>' ./mvnw spring-boot:run`. Default dependencies are `localhost:5432` and `localhost:9092`; the Compose application uses internal service addresses. If using a separately provisioned broker, create `claims.lifecycle.v1` explicitly (3 partitions, appropriate replication for that environment).

Configuration: `DB_URL`, `DB_USER`, required `DB_PASSWORD`, `KAFKA_BOOTSTRAP_SERVERS`, `CLAIMS_TOPIC`, `EVENTS_ENABLED`. Set `EVENTS_ENABLED=false` only when deliberately testing without publication; outbox rows remain durable/pending.

## API usage

Create a claim (amounts are USD reporting currency). `curl` below denotes curl; on Windows use `curl.exe` or the PowerShell smoke script.

```bash
curl -i -X POST http://localhost:8080/api/claims \
  -H 'Content-Type: application/json' -H 'X-Correlation-ID: demo-request' \
  -d '{"claimType":"MOTOR","market":"SG","claimantName":"Demo claimant","incidentDescription":"Collision at junction","incidentDate":"2026-10-01","estimatedLiability":1000.00}'
```

Returns **201** with `Location`, claim ID/number, `SUBMITTED` status and version `0`. Substitute its ID below. Every mutation requires the current `expectedVersion`; use the returned version for the next command.

```bash
curl http://localhost:8080/api/claims/CLAIM_ID
curl 'http://localhost:8080/api/claims?unassigned=true&page=0&size=20'
curl 'http://localhost:8080/api/claims?assignedOfficerId=officer-1&status=UNDER_REVIEW'
curl -X POST http://localhost:8080/api/claims/CLAIM_ID/assignment \
  -H 'Content-Type: application/json' \
  -d '{"expectedVersion":0,"officerId":"officer-1","officerName":"Demo officer"}'
curl -X POST http://localhost:8080/api/claims/CLAIM_ID/review \
  -H 'Content-Type: application/json' -d '{"expectedVersion":1}'
curl http://localhost:8080/api/claims/CLAIM_ID/status
curl http://localhost:8080/api/claims/CLAIM_ID/history
curl http://localhost:8080/api/exposure
curl http://localhost:8080/api/workload
```

Complete create → assign → review → request/answer information → approve → settle demonstration:

```powershell
.\scripts\smoke.ps1
```

The script creates a new demo claim on each invocation. It verifies health, settlement and seven history entries, then retrieves operational views. Inspect Kafka events independently:

```powershell
docker compose exec kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:19092 --topic claims.lifecycle.v1 --from-beginning --timeout-ms 10000
```

| Endpoint | Contract |
|---|---|
| `POST /api/claims` | Create: type, market, claimant, incident/date, estimate |
| `GET /api/claims/{id}` | Detail including information requests and decision |
| `GET /api/claims/{id}/status` | Status, version, updatedAt |
| `GET /api/claims/{id}/history` | Ordered lifecycle history |
| `GET /api/claims` | Optional status/officer/unassigned; page >=0, size 1–100 |
| `POST /api/claims/{id}/assignment` | expectedVersion, officerId, officerName |
| `POST /api/claims/{id}/review` | expectedVersion |
| `POST /api/claims/{id}/information-requests` | expectedVersion, question |
| `POST /api/claims/{id}/information-requests/{requestId}/response` | expectedVersion, response |
| `POST /api/claims/{id}/assessment` | expectedVersion, estimatedLiability, approvedSettlementAmount, reason |
| `POST /api/claims/{id}/settlement` | expectedVersion; records completion using approved amount |
| `POST /api/claims/{id}/rejection` | expectedVersion, reason |
| `GET /api/workload` | Open counts by officer/status, unassigned count, terminal counts/average completion seconds |
| `GET /api/exposure` | USD, totalOutstandingExposure, openClaimCount |

Commands return **200** updated detail; malformed/invalid input **400**, missing claim **404**, invalid transition/stale version **409**. Errors consistently contain timestamp, status, code, safe message, correlationId and fieldErrors. Error responses do not echo supplied values. Request correlation IDs accept only bounded alphanumeric/underscore/hyphen strings; otherwise one is generated.

## Business assumptions

- MOTOR and PROPERTY; provisional markets **AU, NZ, SG, HK, MY, TH**. This is an assessment assumption, not confirmed Chubb scope.
- All amounts already normalized to **USD** by the caller. Nonnegative, at most two fractional digits. No FX conversion and no local-currency aggregation.
- An estimate is supplied at submission and may change during assessment. Exposure includes every nonterminal status, including APPROVED; empty exposure is zero.
- One open information request at a time. A provided response returns the claim to review. Additional cycles are permitted.
- Assessment approves; approved amount may differ from the estimate. Settlement records completion and does not initiate payment.
- Officer IDs are external identifiers. No officer registry, reassignment or terminal reopening.
- Workload is outstanding work. Performance is lifetime settled/rejected count and average report-to-terminal elapsed seconds, not officer productivity or SLA analytics. With no terminal claims, average is null.
- Incident dates use the domain's UTC date; application timestamps use UTC.

## Consistency and known limitations

Claim changes, lifecycle history and outbox inserts share a database transaction. Publication is retried asynchronously and waits for Kafka acknowledgement. A crash between acknowledgement and commit can duplicate an event; consumers must deduplicate `eventId` and handle `aggregateVersion`. Kafka producer idempotence is not end-to-end exactly once. Events intentionally omit claimant names/narratives and evidence.

The publisher assumes **one application instance**, holds a bounded batch transaction while waiting for Kafka, and uses fixed-delay retry without dead-letter tooling. A persistently failing oldest event blocks later events. There is no general strict event-ordering guarantee, consumer implementation, event retention job, or outbox backlog health/metrics. Kafka downtime does not block REST commands, but Actuator UP alone does not establish event delivery health. Database state is the authoritative reporting source.

This is a local assessment with no authentication; production needs OAuth2/OIDC/JWT, claimant ownership and officer/manager role checks. There are no attachments, policy adjudication, payment integration, notifications or UI. The single broker and plaintext listeners are local configuration; production needs replication, TLS/SASL, secrets management and broker operations. History traces state/operation/version but does not establish authenticated actor identity. Claims containing personal information require production retention/access controls.

## Validation record

The latest `mvnw.cmd clean verify` passed on Java **17.0.20.1** with **23 passing tests**, zero failures/errors and one explicit PostgreSQL test skipped. The executable JAR was packaged. The standalone application also started using test-only H2 configuration on port 18080; the PowerShell smoke script completed all seven lifecycle commands, history, exposure and workload checks. Health/OpenAPI/Swagger passed in the real HTTP test. The included Kafka integration test passed against a real embedded KRaft broker, verifying consumed event contents, privacy boundaries and the durable published marker.

`docker compose config --quiet` passed. `docker compose up --build -d` could not start because access to the Docker engine named pipe was denied. Explicitly enabling the PostgreSQL/Testcontainers test also failed because no usable Docker environment was available. **PostgreSQL runtime, container image build and publication to the Compose Kafka broker remain unverified.** A Compose configuration parse is not container startup validation. See `AI_WORKING_JOURNAL.md` for the full evidence and corrections.

## Production evolution

Confirm markets/currency rules; introduce authenticated authorization and ownership; verify PostgreSQL and Kafka end-to-end in CI; add consumer idempotency and transactional integration contracts. Evolve publisher to CDC or coordinated multi-instance polling with per-claim sequencing, backoff/dead-letter recovery, retention and backlog alerts. Measure query plans and consider materialized reporting only when necessary. Extract services only when deployment/team boundaries justify the complexity.

See [ARCHITECTURE.md](ARCHITECTURE.md) for diagrams/ADRs and [AI_WORKING_JOURNAL.md](AI_WORKING_JOURNAL.md) for the actual development/validation record. Git checkpoints reflect incremental work, not a synthetic single final commit.
