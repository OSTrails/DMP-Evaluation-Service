# DMP Evaluation Service

[![CI](https://github.com/OSTrails/DMP-Evaluation-Service/actions/workflows/ci.yml/badge.svg)](https://github.com/OSTrails/DMP-Evaluation-Service/actions/workflows/ci.yml)

A Spring Boot 3 + WebFlux (reactive) REST microservice for the semi-automated evaluation of Data Management Plans (DMPs). It supports both machine-actionable DMPs (maDMPs) in JSON format compliant with the [DMP Common Standard (DCS)](https://github.com/RDA-DMP-Common/RDA-DMP-Common-Standard) and traditional narrative-style DMPs.

This service is a key component of the [OSTrails project](https://ostrails.eu/) and reflects input from research funders, institutional policy frameworks, and research support needs.

**Authors:** , Andres Mauricio, Tomasz Miksa, Elli Papadopoulou.

---

## Table of Contents

- [Evaluation Dimensions](#evaluation-dimensions)
- [Architecture Overview](#architecture-overview)
- [Plugin System](#plugin-system)
- [Data Model](#data-model)
- [API Reference](#api-reference)
- [External Integrations](#external-integrations)
- [Configuration](#configuration)
- [Requirements](#requirements)
- [Running the Service](#running-the-service)
- [Continuous Integration](#continuous-integration)
- [Output Format](#output-format)

---

## Evaluation Dimensions

Evaluation is organised across six core dimensions:

| Dimension | Description |
|-----------|-------------|
| **Content Completeness** | Verifies whether all required DMP sections are present, adequate, and consistent. |
| **RDM Coverage** | Assesses how thoroughly the DMP addresses key RDM areas: collection, documentation, storage, access, sharing, and preservation. |
| **Openness** | Examines open-access support for data, metadata, and outputs, including licensing, embargo periods, and access restrictions. |
| **FAIRness** | Evaluates alignment with the FAIR Principles (Findable, Accessible, Interoperable, Reusable), covering metadata richness, licensing, and persistent identifiers. |
| **Policy Alignment** | Measures adherence to institutional, funder, and legal policies (e.g., GDPR, data-sharing mandates). |
| **Standards Compliance** | Evaluates adherence to recognised structural and content standards (e.g., DMP Common Standard), supporting interoperability and machine-readability. |

---

## Architecture Overview

```
Client
  │
  │  POST /assess/benchmark   (multipart: maDMP file + benchmarkId)
  ▼
EvaluationController
  │
  ▼
EvaluationManagerService          ← orchestration layer
  │  1. Creates EvaluationReport
  │  2. Resolves Benchmark → Metrics → Tests
  │  3. Dispatches per-test to EvaluationService
  ▼
EvaluationService
  │  Looks up EvaluatorPlugin via Spring PluginRegistry
  │  Calls plugin.functionMap[functionName](maDMP, reportId, testRecord)
  ▼
EvaluatorPlugin (one of several implementations)
  │
  ├── CompletenessEvaluator
  ├── DCSCompletenessEvaluator
  ├── ComplianceEvaluator
  ├── FeasibilityEvaluator
  ├── FAIRChampionEvaluator   ← calls external FAIR Champion API
  └── QualityOfActionsEvaluator
  │
  ▼
MongoDB
  Stores: Evaluation, EvaluationReport, BenchmarkRecord,
          MetricRecord, TestRecord, AlgorithmRecord
```

**Request flow:**
1. Client POSTs a maDMP file with a benchmark ID to `/assess/benchmark` (or `/assess/test` for a single test).
2. `EvaluationController` delegates to `EvaluationManagerService`.
3. `EvaluationManagerService` creates an `EvaluationReport`, resolves which tests to run from the benchmark→metric→test chain, and dispatches to `EvaluationService`.
4. `EvaluationService` looks up the correct `EvaluatorPlugin` via Spring's `PluginRegistry` and invokes the matching function from `functionMap`.
5. Results are persisted as `Evaluation` documents linked via `reportId`, then returned to the client as JSON or JSON-LD.

---

## Plugin System

Evaluator plugins are the core extensibility mechanism. Adding a new evaluator requires only implementing one interface and registering it as a Spring `@Component`.

### EvaluatorPlugin interface

```kotlin
interface EvaluatorPlugin : ConfigurablePlugin<String, PluginInfo> {
    val functionMap: Map<String, (JsonObject, reportId: String, testId: TestRecord) -> Evaluation>

    override fun supports(t: String): Boolean = t == getPluginIdentifier()
}
```

- `functionMap` maps test function names (stored in `TestRecord`) to their evaluation implementations.
- `supports()` routes incoming requests to the correct plugin via `pluginId`.
- Spring's `@EnablePluginRegistries(EvaluatorPlugin::class)` enables `PluginRegistry<EvaluatorPlugin, String>` injection across the service layer.

### Available plugins

| Plugin | `pluginId` | Description |
|--------|-----------|-------------|
| `CompletenessEvaluator` | `completeness` | Checks presence and adequacy of DMP fields |
| `DCSCompletenessEvaluator` | `dcs-completeness` | DCS-specific completeness checks |
| `ComplianceEvaluator` | `compliance` | Validates compliance against recognised standards |
| `FeasibilityEvaluator` | `feasibility` | Assesses feasibility of described DMP actions |
| `FAIRChampionEvaluator` | `fair-champion` | Delegates FAIR tests to the external FAIR Champion API |
| `QualityOfActionsEvaluator` | `quality-of-actions` | Evaluates quality and specificity of DMP actions |

### Adding a new evaluator

1. Create a class implementing `EvaluatorPlugin` and annotate it with `@Component`.
2. Implement `getPluginIdentifier()` returning a unique string.
3. Populate `functionMap` with one entry per test function.
4. Create `TestRecord` entries in MongoDB pointing to your `pluginId` and function names.

---

## Data Model

```
BenchmarkRecord
  └── metricIds[]  ──►  MetricRecord
                          └── testIds[]  ──►  TestRecord
                                               ├── pluginId
                                               └── functionName  ──►  EvaluatorPlugin.functionMap

EvaluationReport  (one per benchmark run)
  └── reportId  ◄──  Evaluation  (one per test result)
```

### MongoDB documents

| Document | Collection | Description |
|----------|-----------|-------------|
| `BenchmarkRecord` | `benchmarks` | A named set of metrics representing an evaluation framework |
| `MetricRecord` | `metrics` | A named quality metric containing a list of tests |
| `TestRecord` | `tests` | A single evaluable test; references a plugin and function |
| `AlgorithmRecord` | `algorithms` | Algorithm metadata used by evaluators |
| `EvaluationReport` | `evaluationReports` | Top-level report created per benchmark run |
| `Evaluation` | `evaluations` | Individual test result, linked to a report by `reportId` |

Test results use the `ResultTestEnum` enum: `PASS`, `FAIL`, `ERROR`, `INDERTERMINATED`, `NOT_APPLICABLE`.

---

## API Reference

Swagger UI is available at `http://localhost:8080/swagger-ui.html` when the service is running.

### Authentication

Most read (`GET`) endpoints and assessment (`POST /assess/**`) endpoints are **public** — no token required.

All management endpoints (creating, editing, or deleting benchmarks, metrics, and tests) require a **JWT Bearer token**.

One exception: `POST /benchmarks/list/filter` is public too — it's a read (lookup benchmarks by a list of IDs), just implemented as `POST` because it needs a request body.

#### Roles

| Role | Can create / update | Can delete | Can manage clients |
|------|--------------------|-----------|--------------------|
| `ADMIN` | Yes | Yes | Yes |
| `WRITER` | Yes (own records only) | No | No |

#### Obtaining a token

```http
POST /auth/token
Content-Type: application/json

{ "clientId": "admin", "clientSecret": "your-password" }
```

Response:
```json
{ "accessToken": "eyJ...", "tokenType": "Bearer", "expiresIn": 3600 }
```

Use the token in subsequent requests:
```http
Authorization: Bearer eyJ...
```

Tokens expire after **1 hour**. Request a new one using the same endpoint.

#### Client management (`/admin/clients`) — ADMIN only

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/admin/clients` | Register a new client with a role (`ADMIN` or `WRITER`) |
| `GET` | `/admin/clients` | List all registered clients |
| `DELETE` | `/admin/clients/{clientId}` | Revoke a client |
| `PUT` | `/admin/clients/{clientId}/reset-secret` | Reset a client's secret |

#### Self-service (`/auth`)

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/auth/token` | Obtain a JWT token |
| `PUT` | `/auth/change-secret` | Change your own secret (requires current secret + valid token) |

#### Ownership rule

A `WRITER` can only update records they created. An `ADMIN` can update any record. Records that existed before authentication was enabled have no owner and can only be updated by an `ADMIN`.

### Rate Limiting

Every request — public or authenticated, any method or path — is subject to a per-IP rate limit, enforced before authentication or routing. This protects the API from being overwhelmed by intentional abuse or a misbehaving script (issue #15).

The limiter uses a token-bucket algorithm (via [Bucket4j](https://github.com/bucket4j/bucket4j)): each source IP gets a bucket of `capacity` tokens that refill continuously at a rate of `refillTokens` per `refillDurationSeconds`. Successful responses include a header showing tokens left:

```http
X-RateLimit-Remaining: 42
```

Exceeding the limit returns:

```http
HTTP/1.1 429 Too Many Requests
Retry-After: 12

{
  "code": "RATE_LIMIT_EXCEEDED",
  "message": "Too many requests from your IP address — please slow down and retry after 12 seconds",
  "timestamp": "2026-09-02T10:15:00Z",
  "path": "/benchmarks"
}
```

Default: **100 requests/minute per IP**. Fully configurable via environment variables — see [Environment variables](#environment-variables) below — including a kill switch (`RATE_LIMIT_ENABLED=false`) to disable it entirely.

For the full design writeup (token-bucket internals, library choice, diagrams), see [`dmp-evaluator-service/RATE_LIMITING.md`](dmp-evaluator-service/RATE_LIMITING.md).

### Assessment (`/assess`)

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/assess/benchmark` | Run all tests in a benchmark against a maDMP file. Returns `List<EvaluationResponse>`. |
| `POST` | `/assess/benchmark/json-ld` | Same as above, returns FTR-compliant JSON-LD (`TestResultSetJsonLD`). |
| `POST` | `/assess/test` | Run a single test against a maDMP file. Returns `EvaluationResponse`. |
| `POST` | `/assess/test/JsonLD` | Same as above, returns JSON-LD (`TestResultJsonLD`). |
| `GET` | `/assess` | List all stored evaluations. |
| `GET` | `/assess/report/{reportId}/full` | Retrieve a full evaluation report with all linked results. |

Responses use `EvaluationResponse`/`EvaluationReportInfo` (`identifier` fields) rather than exposing the raw `Evaluation`/`EvaluationReport` documents — migrated to the DTO pattern established for `Benchmark`/`Metric`/`Test` as part of issue #19.

All `/assess` multipart endpoints accept:
- `maDMP` — the maDMP JSON file (FilePart)
- `benchmark` or `test` — the ID of the benchmark or test to run (String)
- `reportId` — optional; a new report is created if omitted

### Benchmarks (`/benchmarks`)

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/benchmarks` | Create a new benchmark — body is `BenchmarkCreateRequest` (title/description/version required; server sets the ID and `createdBy`) |
| `GET` | `/benchmarks/list` | List all benchmarks |
| `GET` | `/benchmarks/list/jsonLD` | List all benchmarks as JSON-LD |
| `GET` | `/benchmarks/info/{benchmarkId}` | Get benchmark details |
| `GET` | `/benchmarks/{benchmarkId}` | Get benchmark as JSON-LD |
| `PUT` | `/benchmarks/{benchmarkId}` | Update benchmark metadata |
| `POST` | `/benchmarks/metrics/{benchmarkId}` | Add metrics to a benchmark |
| `POST` | `/benchmarks/{benchmarkId}/delete/metric` | Remove a metric from a benchmark |
| `POST` | `/benchmarks/list/filter` | Filter benchmarks by a list of IDs — public despite the `POST` verb; it's a read (needs a body for the ID list), not a write |
| `DELETE` | `/benchmarks/{benchmarkId}` | Delete a benchmark |

Responses use `BenchmarkResponse` (`identifier`/`scoringFunction` fields) rather than exposing the raw `BenchmarkRecord` document — migrated to the DTO pattern established for `Test` as part of issue #19.

### Metrics (`/metrics`)

Metrics group related tests and are referenced by benchmarks.

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/metrics` | Create a new metric — body is `MetricCreateRequest` (title/description/version required; server sets the ID and `createdBy`) |
| `GET` | `/metrics` | List all metric IDs |
| `GET` | `/metrics/list` | List all metrics |
| `GET` | `/metrics/list/jsonLD` | List all metrics as JSON-LD |
| `GET` | `/metrics/info/{metricId}` | Get a specific metric |
| `GET` | `/metrics/{metricId}` | Get metric as JSON-LD |
| `PUT` | `/metrics/{metricId}` | Update metric metadata |
| `POST` | `/metrics/addTests/{metricId}` | Add tests to a metric |
| `POST` | `/metrics/addBenchmarks/{metricId}` | Add benchmarks to a metric |
| `POST` | `/metrics/delete/test/{metricId}` | Remove a test from a metric |
| `DELETE` | `/metrics/{metricId}` | Delete a metric |

Responses use `MetricResponse` (`identifier` field) rather than exposing the raw `MetricRecord` document — migrated to the DTO pattern established for `Test`/`Benchmark` as part of issue #19.

### Tests (`/tests`)

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/tests` | Create a new test record — body is `TestCreateRequest` (title/description/license/version required; server sets the ID, `createdBy`, `repository`, and `endpointURL`). Optionally include `metricImplemented`/`evaluator`/`functionEvaluator` to link it to a metric in the same call — equivalent to a separate `addEvaluator` call, including registering the reverse link on the metric. Fails with `404` (and creates nothing) if `metricImplemented` doesn't exist. |
| `GET` | `/tests/info` | List all tests |
| `GET` | `/tests/info/{testId}` | Get a specific test |
| `GET` | `/tests/{testId}` | Get test as JSON-LD |
| `GET` | `/tests/list` | List tests as JSON-LD |
| `GET` | `/tests/metrics/{metricId}` | Get tests belonging to a metric |
| `PUT` | `/tests/{testId}` | Update a test record |
| `POST` | `/tests/{testId}/addEvaluator` | Attach an evaluator plugin to a test |
| `DELETE` | `/tests/{testId}` | Delete a test record |

Responses use `TestResponse` (`identifier` field, matching `Benchmark`/`Metric`) rather than exposing the raw `TestRecord` document — the first entity migrated as part of issue #19 ("Inconsistent API and exposed entities"); the others are tracked to follow the same pattern.

### Plugins (`/plugins`)

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/plugins` | List all registered evaluator plugins |
| `GET` | `/plugins/{pluginId}` | Get information about a specific plugin |

---

## External Integrations

| Service | Purpose | Config key |
|---------|---------|-----------|
| **FAIR Champion** | Delegates FAIR-related tests to the external OSTrails FAIR evaluation API | `dmp.global.fairChampionEndPoint` |
| **Unpaywall** | Looks up open-access status of publications referenced in a DMP | `dmp.global.unpayWallEndPoint` |
| **RML Mapper** | Maps maDMP JSON to RDF/Turtle using RML mapping files in `src/main/resources/rmlmappings/` | Internal (`ToRDFService`) |
| **Eclipse RDF4J** | In-memory RDF store with inference support for RDF operations | Internal |

---

## Configuration

Configuration is managed via `src/main/resources/application.yml`.

### Environment variables

All variables marked **Required** will cause the service to fail to start if missing.

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `JWT_SECRET` | **Yes** | — | Base64-encoded 256-bit HMAC key used to sign and verify JWT tokens. Generate with `openssl rand -base64 32`. |
| `ADMIN_CLIENT_SECRET` | **Yes** | — | Password for the bootstrap admin client created on first startup. |
| `ADMIN_CLIENT_ID` | No | `admin` | Username for the bootstrap admin client. |
| `ADMIN_DISPLAY_NAME` | No | `System Administrator` | Display name for the bootstrap admin. |
| `TEST_URL` | **Yes** | — | Base URL used when building test endpoint links (e.g. `http://localhost:8080/tests`). |
| `METRIC_URL` | **Yes** | — | Base URL used when building metric endpoint links. |
| `BENCHMARK_URL` | **Yes** | — | Base URL used when building benchmark endpoint links. |
| `RATE_LIMIT_ENABLED` | No | `true` | Kill switch for the per-IP rate limiter. Set to `false` to disable it entirely. |
| `RATE_LIMIT_CAPACITY` | No | `100` | Max tokens per IP bucket — i.e. the largest burst a single IP can send instantly. |
| `RATE_LIMIT_REFILL_TOKENS` | No | `100` | Tokens added back per refill period. |
| `RATE_LIMIT_REFILL_DURATION_SECONDS` | No | `60` | Length of the refill period, in seconds. Default config = 100 requests/minute/IP. |

### Fixed configuration

```yaml
server:
  port: 8080

spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/dmp-evaluator

dmp:
  global:
    fairChampionEndPoint: https://tests.ostrails.eu/assess/test/
    unpayWallEndPoint:    https://api.unpaywall.org/v2/
    unpayWallEmail:       dmpEvalutionService@test.com

jwt:
  expiration-seconds: 3600   # tokens expire after 1 hour
```

### Local development profile

Instead of setting environment variables manually every time, create a local override file that Spring Boot loads automatically when the `local` profile is active. This file is excluded from git via `.gitignore`.

1. Edit `src/main/resources/application-local.yml` and fill in your values:
   ```yaml
   jwt:
     secret: <output of: openssl rand -base64 32>

   admin:
     client-secret: my-local-admin-password

   dmp:
     test:
       endpointURL: http://localhost:8080/tests
     metric:
       endpointURL: http://localhost:8080/metrics
     benchmark:
       endpointURL: http://localhost:8080/benchmarks
   ```

2. Run with the profile active:
   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
   ```

> **Never commit `application-local.yml`** — it is listed in `.gitignore` to prevent accidental exposure of secrets.

---

## Requirements

| Requirement | Version |
|-------------|---------|
| Java | 17 or higher (required by Spring Boot 3.x) |
| MongoDB | Any (6.0 recommended via Docker) |
| Docker | For running MongoDB via Docker Compose |
| Maven | 3.8+ (or use the included `./mvnw` wrapper) |

---

## Running the Service

### Local development (recommended)

```bash
# 1. Clone the repository
git clone https://github.com/OSTrails/DMP-Evaluation-Service.git
cd DMP-Evaluation-Service/dmp-evaluator-service

# 2. Start MongoDB
docker-compose up -d

# 3. Edit src/main/resources/application-local.yml with your local values
#    (see the Configuration section above)

# 4. Build the project
./mvnw clean install -DskipTests

# 5. Run with the local profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

> On Windows CMD/PowerShell use `mvnw.cmd` instead of `./mvnw`.

On first startup the service will automatically create an ADMIN client using the credentials from `application-local.yml`. You will see this log line:
```
Bootstrap: admin client 'admin' created successfully
```

The service will be available at `http://localhost:8080`.
Swagger UI: `http://localhost:8080/swagger-ui.html`

### Production / CI

Set all required environment variables in your runtime environment (Docker, Kubernetes, CI pipeline) and run without a profile override:

```bash
./mvnw spring-boot:run
# or deploy the fat JAR produced by: ./mvnw clean package
```

---

MongoDB must be running before executing integration tests.

---

## Continuous Integration

Every push to `main` and every pull request targeting `main` runs the GitHub Actions workflow defined in [`.github/workflows/ci.yml`](.github/workflows/ci.yml):

| Step | Purpose | Blocking? |
|------|---------|-----------|
| Build & test (`./mvnw --batch-mode clean verify`) | Compiles the service and runs the full test suite against a MongoDB 6.0 service container spun up for the job | Yes — a failure fails the pipeline |
| ktlint style check | Reports Kotlin style violations across `src/**/*.kt` | No — currently report-only (`continue-on-error`) while the codebase is brought into line with ktlint's rules |

The job runs on `ubuntu-latest` with Java 17 (Temurin) and Maven dependency caching. No manual MongoDB setup is needed in CI — the workflow provisions its own MongoDB container and waits for it to become healthy before running tests.

---

## Output Format

Evaluation results are compatible with the [FAIR Assessment Output Specification](https://github.com/OSTrails/FAIR_assessment_output_specification).

The `/json-ld` endpoints return `TestResultSetJsonLD` or `TestResultJsonLD` objects structured as JSON-LD, aligned with the FTR (FAIR Test Result) vocabulary. Each result includes:

- Test identifier and description
- Result status: `PASS`, `FAIL`, `ERROR`, `INDERTERMINATED`, or `NOT_APPLICABLE`
- Score and weighted score (where applicable)
- Linked `reportId` for cross-referencing results within a benchmark run
