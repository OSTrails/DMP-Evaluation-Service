# DMP Evaluation Service

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

### Assessment (`/assess`)

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/assess/benchmark` | Run all tests in a benchmark against a maDMP file. Returns `List<Evaluation>`. |
| `POST` | `/assess/benchmark/json-ld` | Same as above, returns FTR-compliant JSON-LD (`TestResultSetJsonLD`). |
| `POST` | `/assess/test` | Run a single test against a maDMP file. Returns `Evaluation`. |
| `POST` | `/assess/test/JsonLD` | Same as above, returns JSON-LD (`TestResultJsonLD`). |
| `POST` | `/assess/mappingRDF` | Convert a maDMP file to RDF using RML mappings. |
| `GET` | `/assess` | List all stored evaluations. |
| `GET` | `/assess/report/{reportId}/full` | Retrieve a full evaluation report with all linked results. |

All `/assess` multipart endpoints accept:
- `maDMP` — the maDMP JSON file (FilePart)
- `benchmark` or `test` — the ID of the benchmark or test to run (String)
- `reportId` — optional; a new report is created if omitted

### Benchmarks (`/benchmarks`)

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/benchmarks` | Create a new benchmark |
| `GET` | `/benchmarks/list` | List all benchmarks |
| `GET` | `/benchmarks/list/jsonLD` | List all benchmarks as JSON-LD |
| `GET` | `/benchmarks/info/{benchmarkId}` | Get benchmark details |
| `GET` | `/benchmarks/{benchmarkId}` | Get benchmark as JSON-LD |
| `POST` | `/benchmarks/edit/{benchmarkId}` | Update benchmark metadata |
| `POST` | `/benchmarks/metrics/{benchmarkId}` | Add metrics to a benchmark |
| `POST` | `/benchmarks/{benchmarkId}/delete/metric` | Remove a metric from a benchmark |
| `POST` | `/benchmarks/list/filter` | Filter benchmarks by a list of IDs |
| `DELETE` | `/benchmarks/{benchmarkId}` | Delete a benchmark |

### Metrics (`/metrics`)

Provides CRUD operations for `MetricRecord` documents. Metrics group related tests and are referenced by benchmarks.

### Tests (`/tests`)

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/tests` | Create a new test record |
| `GET` | `/tests/info` | List all tests |
| `GET` | `/tests/info/{testId}` | Get a specific test |
| `GET` | `/tests/{testId}` | Get test as JSON-LD |
| `GET` | `/tests/list` | List tests as JSON-LD |
| `GET` | `/tests/metrics/{metricId}` | Get tests belonging to a metric |
| `POST` | `/tests/{testId}` | Update a test record |
| `POST` | `/tests/{testId}/addEvaluator` | Attach an evaluator plugin to a test |
| `DELETE` | `/tests/{testId}` | Delete a test record |

### Plugins (`/plugins`)

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/plugins` | List all registered evaluator plugins |
| `GET` | `/plugins/{evaluatorId}` | Get information about a specific plugin |

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

Configuration is managed via `src/main/resources/application.yml`. The key properties are:

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
    unpayWallEmail:       dmpEvaluationService@test.com
  test:
    endpointURL: ${TEST_URL}
  metric:
    endpointURL: ${METRIC_URL}
  benchmark:
    endpointURL: ${BENCHMARK_URL}
```

The following environment variables must be set at runtime:

| Variable | Description |
|----------|-------------|
| `TEST_URL` | Base URL for test endpoint resolution |
| `METRIC_URL` | Base URL for metric endpoint resolution |
| `BENCHMARK_URL` | Base URL for benchmark endpoint resolution |

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

```bash
# 1. Clone the repository
git clone https://github.com/OSTrails/DMP-Evaluation-Service.git
cd DMP-Evaluation-Service/dmp-evaluator-service

# 2. Start MongoDB
docker-compose up -d

# 3. Build the project
./mvnw clean install

# 4. Run the application
./mvnw spring-boot:run
```

> On Windows CMD use `mvnw.cmd` instead of `./mvnw`.

The service will be available at `http://localhost:8080`.
Swagger UI: `http://localhost:8080/swagger-ui.html`

---

MongoDB must be running before executing integration tests.

---

## Output Format

Evaluation results are compatible with the [FAIR Assessment Output Specification](https://github.com/OSTrails/FAIR_assessment_output_specification).

The `/json-ld` endpoints return `TestResultSetJsonLD` or `TestResultJsonLD` objects structured as JSON-LD, aligned with the FTR (FAIR Test Result) vocabulary. Each result includes:

- Test identifier and description
- Result status: `PASS`, `FAIL`, `ERROR`, `INDERTERMINATED`, or `NOT_APPLICABLE`
- Score and weighted score (where applicable)
- Linked `reportId` for cross-referencing results within a benchmark run
