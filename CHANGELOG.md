# Changelog

All notable changes to the *OSTrails DMP Evaluation Service*
will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).

*For each release, use the following sub-sections:*

- *Added (for new features)*
- *Changed (for changes in existing functionality)*
- *Deprecated (for soon-to-be removed features)*
- *Removed (for now removed features)*
- *Fixed (for any bug fixes)*
- *Security (in case of vulnerabilities)*

## [Unreleased]

### Added
- New evaluator tests: `dmpIdValid` (QualityOfActionsEvaluator), `datasetPersistentIdentifierPresent`
  (DCSCompletenessEvaluator), and `datasetLicenseIsOpen` / `datasetRepositoryIsInRe3data`
  (ComplianceEvaluator, including RE3data repository lookup).
- Structured `guidance` field on evaluation results (`EvaluatorUtils.extractAssessmentTarget`,
  `buildDatasetLabel`), populated per-dataset for the license, RE3data, and persistent-identifier
  tests to explain failures/indeterminate results.
- `ExternalBenchmarkPlugin` interface extending `EvaluatorPlugin` for plugins that
  return multiple `Evaluation` results per call (one per test result per dataset), with
  dispatch routing added to `EvaluationService` and `PluginManagerService`.
- `FAIRChampionEvaluator` integration with the external FAIR Champion benchmark API:
  evaluates the FAIRness of each dataset declared in a maDMP and produces one
  `Evaluation` document per FAIR test per dataset.
- `FairChampionResponseUtils`: parser that maps FAIR Champion JSON-LD `resultset`
  responses to `List<Evaluation>`.
- `fairChampionBenchmarkEndpoint` configuration property for the FAIR Champion
  benchmark API endpoint.
- `TestResultSetJsonLD` model for the FAIR Testing Requirements (FTR)-aligned
  benchmark response envelope.
- README rewritten with an architecture overview, plugin system, data model, API
  reference, and configuration documentation.
- GitHub Actions CI workflow (`.github/workflows/ci.yml`, issue #17): runs
  `mvnw clean verify` against a MongoDB service container on every push to
  `main` and every pull request targeting `main`, plus a report-only ktlint
  style check.

### Changed
- `Evaluation`, `EvaluationReport`, `BenchmarkRecord`, `MetricRecord`, and
  `AlgorithmRecord` models realigned with the FTR/JSON-LD output specification:
  fields renamed/retyped (`identifier`, `description`, `value`, `generatedAtTime`,
  `assessmentTarget`, `wasGeneratedBy`, `guidance`; `affectedElements` is now
  `List<String>`) and a structured `Guidance`/`GuidanceEntry` type was introduced.
- `EvaluationController`, `EvaluationManagerService`, `EvaluationService`,
  `BenchmarService`, `MetricService`, `Benchmark`, `Metric`, and `TestResultJsonLD`
  updated to produce and consume the FTR-aligned JSON-LD output.
- `EvaluationService.generateTestsResultsFromBenchmark`: plugins implementing
  `ExternalBenchmarkPlugin` are now routed through `benchmarkFunctionMap` (returns
  `List<Evaluation>`); existing evaluators using `functionMap` are unaffected.
- `FairChampionService`: extended with `assessBenchmark(guid)` method that POSTs
  to the FAIR Champion benchmark endpoint and returns a structured `JsonObject`.
- Removed unused imports/autowiring in `MetricService`, and the redundant `generated`
  field assignment in `ComplianceEvaluator.checkFormatFile`.

### Fixed
- `GET /plugins` was missing the function names for `ExternalBenchmarkPlugin`
  implementations; `PluginManagerService` now resolves functions from
  `benchmarkFunctionMap` for those plugins.
- Corrected the FAIR Champion evaluator's plugin description text.

## [1.0.0] 2025-MM-DD
