package io.github.ostrails.dmpevaluatorservice.utils.dbpatchers

import io.github.ostrails.dmpevaluatorservice.database.model.BenchmarkRecord
import io.github.ostrails.dmpevaluatorservice.database.model.MetricRecord
import io.github.ostrails.dmpevaluatorservice.database.model.TestRecord
import jakarta.annotation.PostConstruct
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.exists
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Component

// Mirrors the benchmarks/metrics/tests already registered on the deployed
// OSTrails DMP Evaluation Service (https://ostrails-dmp-evaluation.arisnet.ac.at),
// reusing its exact document ids. That makes this idempotent against the real
// production database too - not just a fresh one - so a redeploy never creates
// a duplicate of an entity that's already there.
@Component
class BaseEntitySeeder(
    private val mongoTemplate: MongoTemplate,
) {
    companion object {
        private val log: Logger = LoggerFactory.getLogger(BaseEntitySeeder::class.java)

        // --- Benchmarks ---
        const val BENCHMARK_DMP_COMPLIANCE = "684843aa21dfc4211ca0cdcf"
        const val BENCHMARK_DMP_FAIRNESS = "68bfe7e0d66a3009c09e6832"
        const val BENCHMARK_RDM_COVERAGE = "68bff901d66a3009c09e6834"
        const val BENCHMARK_FWF_ORD = "69ef5cdfcde500798dbd1af8"
        const val BENCHMARK_FAIR_CHAMPION = "6a39901ec2178872c6d3322e"

        // --- Metrics ---
        const val METRIC_DMP_JSON_FORMAT = "6848512f21dfc4211ca0cdd1"
        const val METRIC_DCS_REQUIRED = "6848524c21dfc4211ca0cdd3"
        const val METRIC_DCS_TYPE_CHECK = "6848531d21dfc4211ca0cdd4"
        const val METRIC_RDM_ROLES_DEFINED = "68bf1f74d66a3009c09e682b"
        const val METRIC_OUTPUT_METADATA_CHECK = "68bf1fced66a3009c09e682c"
        const val METRIC_RDM_COST_DETAILS = "68bf212cd66a3009c09e682d"
        const val METRIC_FM_PUBOA_UP = "68bf2155d66a3009c09e682e"
        const val METRIC_FAIR_DMP_PID = "68bf2262d66a3009c09e682f"
        const val METRIC_DATA_PID_FEAS_3 = "69ef21bbcde500798dbd1af5"
        const val METRIC_DATA_SHAR_COMP_1 = "69ef21ffcde500798dbd1af6"
        const val METRIC_REPO_COMP_3 = "69ef2231cde500798dbd1af7"
        const val METRIC_FC_FAIRNESS = "6a398fb0c2178872c6d3322d"

        // --- Tests ---
        const val TEST_MACHINE_ACTIONABLE_FORMAT = "6848540b21dfc4211ca0cdd5"
        const val TEST_DMP_STRUCTURE_VALIDATION = "684854e921dfc4211ca0cdd8"
        const val TEST_MADMP_FIELD_FORMAT = "6848564621dfc4211ca0cdd9"
        const val TEST_ROLES_IN_RDM_DEFINED = "68bf0fdbd66a3009c09e6825"
        const val TEST_DATASET_ENTITY_COMPLETENESS = "68bf10f6d66a3009c09e6826"
        const val TEST_COST_ENTITY_COMPLETENESS = "68bf1129d66a3009c09e6827"
        const val TEST_PRESENCE_OF_COST_ENTITY = "68bf1165d66a3009c09e6828"
        const val TEST_OPEN_ACCESS_VIA_UNPAYWALL = "68bf118ad66a3009c09e6829"
        const val TEST_DMP_IDENTIFIER_STRUCTURE = "68bf137ed66a3009c09e682a"
        const val TEST_DATASET_PID_CHECK = "69eb2ae9cde500798dbd1af1"
        const val TEST_DATASET_OPEN_LICENSE_CHECK = "69ef1dd3cde500798dbd1af3"
        const val TEST_DATASET_REPOSITORY_RE3DATA = "69ef1e07cde500798dbd1af4"
        const val TEST_FAIR_CHAMPION_DATASET_FAIRNESS = "6a398f5dc2178872c6d3322c"
    }

    @PostConstruct
    fun seed() {
        // Tests/metrics/benchmarks reference each other only by id, so insert
        // order doesn't affect correctness - bottom-up mirrors the dependency
        // direction and matches the previous FairChampionSeeder convention.
        seedTests()
        seedMetrics()
        seedBenchmarks()
    }

    private fun seedTests() {
        val tests = listOf(
            TestRecord(
                id = TEST_MACHINE_ACTIONABLE_FORMAT,
                title = "Machine-Actionable Format Validation",
                description = "Checks whether the DMP is provided in a machine-actionable format, specifically JSON.",
                license = "https://opensource.org/license/mit",
                version = "0.0.1",
                keyword = "machine-actionable, format, JSON, DMP",
                type = "format",
                theme = "File Format Validation",
                supportedBy = "https://github.com/OSTrails/DMP-Evaluation-Service",
                isApplicableFor = "https://vocabularies.coar-repositories.org/resource_types/c_ab20/",
                evaluator = "ComplianceEvaluator",
                functionEvaluator = "checkFormatFile",
                metricImplemented = METRIC_DMP_JSON_FORMAT,
            ),
            TestRecord(
                id = TEST_DMP_STRUCTURE_VALIDATION,
                title = "DMP Structure Validation (JSON Schema)",
                description = "Validates the maDMP against the official JSON Schema definition to ensure structural integrity.",
                license = "https://opensource.org/license/mit",
                version = "0.0.1",
                keyword = "DMP, JSON Schema, structure, validation",
                type = "validation",
                theme = "DMP Compliance",
                supportedBy = "https://github.com/OSTrails/DMP-Evaluation-Service",
                isApplicableFor = "https://vocabularies.coar-repositories.org/resource_types/c_ab20/",
                evaluator = "DCSCompletenessEvaluator",
                functionEvaluator = "evaluateStructure",
                metricImplemented = METRIC_DCS_REQUIRED,
            ),
            TestRecord(
                id = TEST_MADMP_FIELD_FORMAT,
                title = "maDMP Field Format Compliance",
                description = "Checks that field values in the maDMP adhere to expected formats like date, email, enums, etc.",
                license = "https://opensource.org/license/mit",
                version = "0.0.1",
                keyword = "format, field validation, enum, date",
                type = "validation",
                theme = "DMP Compliance",
                supportedBy = "https://github.com/OSTrails/DMP-Evaluation-Service",
                isApplicableFor = "https://vocabularies.coar-repositories.org/resource_types/c_ab20/",
                evaluator = "DCSCompletenessEvaluator",
                functionEvaluator = "evaluateFormats",
                metricImplemented = METRIC_DCS_TYPE_CHECK,
            ),
            TestRecord(
                id = TEST_ROLES_IN_RDM_DEFINED,
                title = "Roles in RDM Defined",
                description = "Verifies that contributor roles related to data management are clearly defined in the DMP.",
                license = "https://opensource.org/license/mit",
                version = "0.0.1",
                keyword = "roles, contributor, RDM, DMP",
                type = "test",
                theme = "DMP Coverage",
                supportedBy = "https://github.com/OSTrails/DMP-Evaluation-Service",
                isApplicableFor = "https://vocabularies.coar-repositories.org/resource_types/c_ab20/",
                evaluator = "DCSCompletenessEvaluator",
                functionEvaluator = "contributorValuesPresent",
                metricImplemented = METRIC_RDM_ROLES_DEFINED,
            ),
            TestRecord(
                id = TEST_DATASET_ENTITY_COMPLETENESS,
                title = "Dataset Entity Completeness",
                description = "Checks that dataset entities in the maDMP contain all required fields, including distribution, identifiers, and personal data flags.",
                license = "https://opensource.org/license/mit",
                version = "0.0.1",
                keyword = "dataset, completeness, validation, DMP",
                type = "test",
                theme = "DMP Coverage",
                supportedBy = "https://github.com/OSTrails/DMP-Evaluation-Service",
                isApplicableFor = "https://vocabularies.coar-repositories.org/resource_types/c_ab20/",
                evaluator = "DCSCompletenessEvaluator",
                functionEvaluator = "datasetEntityValuesPresent",
                metricImplemented = METRIC_OUTPUT_METADATA_CHECK,
            ),
            TestRecord(
                id = TEST_COST_ENTITY_COMPLETENESS,
                title = "Cost Entity Completeness",
                description = "Checks that cost fields such as title, value, description, and currency_code are filled in.",
                license = "https://opensource.org/license/mit",
                version = "0.0.1",
                keyword = "cost, DMP, coverage, completeness",
                type = "test",
                theme = "DMP Coverage",
                supportedBy = "https://github.com/OSTrails/DMP-Evaluation-Service",
                isApplicableFor = "https://vocabularies.coar-repositories.org/resource_types/c_ab20/",
                evaluator = "DCSCompletenessEvaluator",
                functionEvaluator = "costEntityValuesPresent",
                metricImplemented = METRIC_RDM_COST_DETAILS,
            ),
            TestRecord(
                id = TEST_PRESENCE_OF_COST_ENTITY,
                title = "Presence of Cost Entity",
                description = "Ensures the presence of the 'cost' field in the maDMP.",
                license = "https://opensource.org/license/mit",
                version = "0.0.1",
                keyword = "cost, DMP, entity",
                type = "test",
                theme = "DMP Coverage",
                supportedBy = "https://github.com/OSTrails/DMP-Evaluation-Service",
                isApplicableFor = "https://vocabularies.coar-repositories.org/resource_types/c_ab20/",
                evaluator = "DCSCompletenessEvaluator",
                functionEvaluator = "costEntityPresent",
                metricImplemented = METRIC_RDM_COST_DETAILS,
            ),
            TestRecord(
                id = TEST_OPEN_ACCESS_VIA_UNPAYWALL,
                title = "Open Access Check via Unpaywall",
                description = "Uses the Unpaywall API to verify if the publication linked by DOI is open access.",
                license = "https://opensource.org/license/mit",
                version = "0.0.1",
                keyword = "open access, Unpaywall, DOI, repository",
                isApplicableFor = "https://vocabularies.coar-repositories.org/resource_types/c_ab20/",
                evaluator = "QualityOfActionsEvaluator",
                functionEvaluator = "evaluateOpenAccess",
                metricImplemented = METRIC_FM_PUBOA_UP,
            ),
            TestRecord(
                id = TEST_DMP_IDENTIFIER_STRUCTURE,
                title = "DMP Identifier Structure Validation",
                description = "Validates that the DMP includes a proper identifier and its associated type, ensuring that the maDMP is uniquely identifiable and adheres to standard structural requirements.",
                license = "https://opensource.org/license/mit",
                version = "0.0.1",
                keyword = "DMP, identifier, dmp_id, validation, metadata",
                type = "test",
                theme = "DMP FAIRNESS",
                supportedBy = "https://github.com/OSTrails/DMP-Evaluation-Service",
                isApplicableFor = "https://vocabularies.coar-repositories.org/resource_types/c_ab20/",
                evaluator = "QualityOfActionsEvaluator",
                functionEvaluator = "dmpIdValid",
                metricImplemented = METRIC_FAIR_DMP_PID,
            ),
            TestRecord(
                id = TEST_DATASET_PID_CHECK,
                title = "Dataset Persistent Identifier Check",
                description = "Checks that each dataset in the maDMP has a valid persistent identifier type (doi, handle, ark, url, other) and that the identifier is resolvable via HTTP GET. The test fails if any dataset has a missing, invalid, or unresolvable identifier.",
                license = "https://creativecommons.org/licenses/by/4.0/",
                version = "1.0.0",
                keyword = "persistent identifier, dataset, PID, resolvability",
                type = null,
                theme = null,
                supportedBy = "https://github.com/OSTrails/DMP-Evaluation-Service",
                isApplicableFor = null,
                evaluator = "DCSCompletenessEvaluator",
                functionEvaluator = "datasetPersistentIdentifierPresent",
                metricImplemented = METRIC_DATA_PID_FEAS_3,
            ),
            TestRecord(
                id = TEST_DATASET_OPEN_LICENSE_CHECK,
                title = "Dataset Open License Check",
                description = "Checks that every distribution of every dataset in the maDMP is assigned a recognized open license, verified against the Open Knowledge Foundation Open Definition license list. Fails if any distribution is missing a license or has a non-open license.",
                license = "https://creativecommons.org/licenses/by/4.0/",
                version = "1.0.0",
                keyword = "license, open license, dataset, open definition, compliance",
                type = null,
                theme = null,
                supportedBy = "https://github.com/OSTrails/DMP-Evaluation-Service",
                isApplicableFor = null,
                evaluator = "ComplianceEvaluator",
                functionEvaluator = "datasetLicenseIsOpen",
                metricImplemented = METRIC_DATA_SHAR_COMP_1,
            ),
            TestRecord(
                id = TEST_DATASET_REPOSITORY_RE3DATA,
                title = "Dataset Repository is Registered in RE3data",
                description = "Checks whether the repository declared in each dataset distribution (dmp.dataset[*].distribution[*].host.url) is registered in the RE3data global registry of research data repositories (re3data.org). The host URL is normalized to its domain, queried through the RE3data OpenSearch suggest endpoint to retrieve candidate repository names, matched against the full RE3data repository list to obtain IDs, and verified against the repository detail record. PASS if all dataset distributions reference a RE3data-registered repository; FAIL if any distribution is missing a host, has a blank URL, or the repository is not found in RE3data.",
                license = "https://creativecommons.org/licenses/by/4.0/",
                version = "1.0.0",
                keyword = "re3data, repository, dataset, compliance, FAIR",
                type = null,
                theme = "https://w3id.org/dpv#Compliance",
                supportedBy = "https://www.re3data.org/api/v1/",
                isApplicableFor = "https://schema.org/Dataset",
                evaluator = "ComplianceEvaluator",
                functionEvaluator = "datasetRepositoryIsInRe3data",
                metricImplemented = METRIC_REPO_COMP_3,
            ),
            TestRecord(
                id = TEST_FAIR_CHAMPION_DATASET_FAIRNESS,
                title = "FAIR Champion Dataset FAIRness Evaluation",
                description = "Evaluates the FAIRness of datasets declared in the maDMP by delegating to the external FAIR Champion service.",
                license = "https://creativecommons.org/licenses/by/4.0/",
                version = "1.0.0",
                keyword = "FAIR, dataset, FAIRness",
                type = null,
                theme = null,
                supportedBy = "https://github.com/OSTrails/DMP-Evaluation-Service",
                isApplicableFor = "https://schema.org/Dataset",
                evaluator = "FAIR_Champion",
                functionEvaluator = "evaluateFAIRnessDatasetByChampion",
                metricImplemented = METRIC_FC_FAIRNESS,
            ),
        )
        seedIfMissing(tests, "tests") { it.id }
    }

    private fun seedMetrics() {
        val metrics = listOf(
            MetricRecord(
                id = METRIC_DMP_JSON_FORMAT,
                title = "Machine-Actionable DMP Format Validation",
                description = "Verifies whether the DMP is provided in a machine-actionable format, such as JSON, ensuring automated processing and reuse.",
                version = "0.0.1",
                testAssociated = listOf(TEST_MACHINE_ACTIONABLE_FORMAT),
                abbreviation = "DMP-JSON-Format",
                supportedBy = "https://github.com/OSTrails/DMP-Evaluation-Service",
                isApplicableFor = "https://vocabularies.coar-repositories.org/resource_types/c_ab20/",
                keyword = "machine-actionable, format, JSON, automation",
                hasBenchmark = listOf(BENCHMARK_DMP_COMPLIANCE),
                license = "http://creativecommons.org/licenses/by/2.0/",
            ),
            MetricRecord(
                id = METRIC_DCS_REQUIRED,
                title = "Required Fields Compliance with DCS",
                description = "Ensures that all mandatory fields as defined by the DMP Common Standard are present and correctly structured.",
                version = "0.0.1",
                testAssociated = listOf(TEST_DMP_STRUCTURE_VALIDATION),
                abbreviation = "DCS-Required",
                supportedBy = "https://github.com/OSTrails/DMP-Evaluation-Service",
                isApplicableFor = "https://vocabularies.coar-repositories.org/resource_types/c_ab20/",
                keyword = "DMP, required fields, compliance, validation",
                hasBenchmark = listOf(BENCHMARK_DMP_COMPLIANCE),
                license = "http://creativecommons.org/licenses/by/2.0/",
            ),
            MetricRecord(
                id = METRIC_DCS_TYPE_CHECK,
                title = "Data Type Format Validation in maDMP",
                description = "Validates if input fields in the DMP conform to the data types specified by the DMP Common Standard (DCS).",
                version = "0.0.1",
                testAssociated = listOf(TEST_MADMP_FIELD_FORMAT),
                abbreviation = "DCS-Type-Check",
                supportedBy = "https://github.com/OSTrails/DMP-Evaluation-Service",
                isApplicableFor = "https://vocabularies.coar-repositories.org/resource_types/c_ab20/",
                keyword = "DMP, data types, validation, schema",
                hasBenchmark = listOf(BENCHMARK_DMP_COMPLIANCE),
                license = "http://creativecommons.org/licenses/by/2.0/",
            ),
            MetricRecord(
                id = METRIC_RDM_ROLES_DEFINED,
                title = "Defined Roles for Contributors in RDM",
                description = "Validates the presence and accuracy of contributor roles in the DMP, supporting RDM responsibility transparency.",
                version = "0.0.1",
                testAssociated = listOf(TEST_ROLES_IN_RDM_DEFINED),
                abbreviation = "RDM-Roles-Defined",
                supportedBy = "https://github.com/OSTrails/DMP-Evaluation-Service",
                isApplicableFor = "https://vocabularies.coar-repositories.org/resource_types/c_ab20/",
                keyword = "RDM, contributor, roles, responsibility, DMP",
                hasBenchmark = listOf(BENCHMARK_RDM_COVERAGE),
                license = "http://creativecommons.org/licenses/by/2.0/",
            ),
            MetricRecord(
                id = METRIC_OUTPUT_METADATA_CHECK,
                title = "Output Metadata Completeness",
                description = "Checks that all research outputs (datasets) include complete metadata such as identifier, type, license, repository, and host.",
                version = "0.0.1",
                testAssociated = listOf(TEST_DATASET_ENTITY_COMPLETENESS),
                abbreviation = "Output-Metadata-Check",
                supportedBy = "https://github.com/OSTrails/DMP-Evaluation-Service",
                isApplicableFor = "https://vocabularies.coar-repositories.org/resource_types/c_ab20/",
                keyword = "outputs, metadata, dataset, license, host, type",
                hasBenchmark = listOf(BENCHMARK_RDM_COVERAGE),
                license = "http://creativecommons.org/licenses/by/2.0/",
            ),
            MetricRecord(
                id = METRIC_RDM_COST_DETAILS,
                title = "RDM Cost Information Availability",
                description = "Checks if detailed cost elements (title, value, currency, description) related to data management are present in the DMP.",
                version = "0.0.1",
                testAssociated = listOf(TEST_COST_ENTITY_COMPLETENESS, TEST_PRESENCE_OF_COST_ENTITY),
                abbreviation = "RDM-Cost-Details",
                supportedBy = "https://github.com/OSTrails/DMP-Evaluation-Service",
                isApplicableFor = "https://vocabularies.coar-repositories.org/resource_types/c_ab20/",
                keyword = "cost, budget, expenses, RDM, DMP",
                hasBenchmark = listOf(BENCHMARK_RDM_COVERAGE),
                license = "http://creativecommons.org/licenses/by/2.0/",
            ),
            MetricRecord(
                id = METRIC_FM_PUBOA_UP,
                title = "FAIR Metric - Open Access Publication via Unpaywall (FM-PUBOA_UP)",
                description = "Ensures that at least one open access version of the publication is available via the Unpaywall service, promoting FAIR compliance (F4).",
                version = "0.0.1",
                testAssociated = listOf(TEST_OPEN_ACCESS_VIA_UNPAYWALL),
                abbreviation = "FM-PUBOA_UP",
                supportedBy = "https://github.com/OSTrails/DMP-Evaluation-Service",
                isApplicableFor = "https://vocabularies.coar-repositories.org/resource_types/c_ab20/",
                landingPage = "https://fairsharing.org/6449",
                keyword = "FAIR, open access, publication, unpaywall, F4",
                hasBenchmark = emptyList(),
                license = "http://creativecommons.org/licenses/by/2.0/",
            ),
            MetricRecord(
                id = METRIC_FAIR_DMP_PID,
                title = "FAIR DMP Output Persistence",
                description = "Verifies whether the Data Management Plan (DMP) is having an identified with a persistent identifier (PID), ensuring long-term accessibility, traceability, and FAIR compliance.",
                version = "0.0.1",
                testAssociated = listOf(TEST_DMP_IDENTIFIER_STRUCTURE),
                abbreviation = "FAIR-DMP-PID",
                supportedBy = "https://github.com/OSTrails/DMP-Evaluation-Service",
                isApplicableFor = "https://vocabularies.coar-repositories.org/resource_types/c_ab20/",
                keyword = "DMP, Outputs, maDMP, Metric, PID, Repository, FAIR, Persistence",
                hasBenchmark = listOf(BENCHMARK_DMP_FAIRNESS),
                license = "http://creativecommons.org/licenses/by/2.0/",
            ),
            MetricRecord(
                id = METRIC_DATA_PID_FEAS_3,
                title = "Dataset Persistent Identifier Resolvability",
                description = "Checks whether each dataset declared in the maDMP includes a valid persistent identifier type (doi, handle, ark, url, other) and that the identifier resolves successfully via HTTP.",
                version = "1.0.0",
                testAssociated = listOf(TEST_DATASET_PID_CHECK),
                abbreviation = "data.pid.feas.3",
                supportedBy = "https://github.com/OSTrails/DMP-Evaluation-Service",
                keyword = "persistent identifier, dataset, PID, resolvability, findability",
                hasBenchmark = emptyList(),
                license = "http://creativecommons.org/licenses/by/2.0/",
            ),
            MetricRecord(
                id = METRIC_DATA_SHAR_COMP_1,
                title = "Dataset Open License Compliance",
                description = "Verifies that every distribution of every dataset in the maDMP declares a recognized open license, validated against the Open Knowledge Foundation Open Definition license list.",
                version = "1.0.0",
                testAssociated = listOf(TEST_DATASET_OPEN_LICENSE_CHECK),
                abbreviation = "data.shar.comp.1",
                supportedBy = "https://github.com/OSTrails/DMP-Evaluation-Service",
                keyword = "license, open license, dataset, open definition, compliance, OKF",
                // Live production has this as [] even though the FWF-ORD benchmark
                // references it via hasAssociatedMetric - fixed here for consistency.
                hasBenchmark = listOf(BENCHMARK_FWF_ORD),
                license = "http://creativecommons.org/licenses/by/2.0/",
            ),
            MetricRecord(
                id = METRIC_REPO_COMP_3,
                title = "Trusted Repository Registration in RE3data",
                description = "Checks whether the repository declared for each dataset distribution is registered in the RE3data global registry of research data repositories, ensuring datasets are stored in a recognised and trusted repository.",
                version = "1.0.0",
                testAssociated = listOf(TEST_DATASET_REPOSITORY_RE3DATA),
                abbreviation = "repo.comp.3",
                supportedBy = "https://github.com/OSTrails/DMP-Evaluation-Service",
                isApplicableFor = "https://schema.org/Dataset",
                keyword = "re3data, repository, trusted, dataset, compliance, FAIR",
                // Same fix as above - referenced by FWF-ORD but stored as [] in production.
                hasBenchmark = listOf(BENCHMARK_FWF_ORD),
                license = "http://creativecommons.org/licenses/by/2.0/",
            ),
            MetricRecord(
                id = METRIC_FC_FAIRNESS,
                title = "Dataset FAIRness",
                description = "Measures the FAIRness of datasets declared in a maDMP using the FAIR Champion external evaluator.",
                version = "1.0.0",
                testAssociated = listOf(TEST_FAIR_CHAMPION_DATASET_FAIRNESS),
                abbreviation = "FC-FAIRNESS",
                supportedBy = "https://github.com/OSTrails/DMP-Evaluation-Service",
                isApplicableFor = "https://schema.org/Dataset",
                keyword = "FAIR, dataset",
                hasBenchmark = listOf(BENCHMARK_FAIR_CHAMPION),
                license = "http://creativecommons.org/licenses/by/2.0/",
            ),
        )
        seedIfMissing(metrics, "metrics") { it.id }
    }

    private fun seedBenchmarks() {
        val benchmarks = listOf(
            BenchmarkRecord(
                benchmarkId = BENCHMARK_DMP_COMPLIANCE,
                title = "Compliance with DMP Common Standards",
                description = "Ensures that the machine-actionable DMP follows the DMP Common Standard structure and field requirements, facilitating interoperability and quality assurance.",
                version = "0.0.1",
                hasAssociatedMetric = listOf(METRIC_DMP_JSON_FORMAT, METRIC_DCS_REQUIRED, METRIC_DCS_TYPE_CHECK),
                abbreviation = "DMP-COMPLIANCE",
                landingPage = "https://www.testing.com",
                keyword = "DMP, Common Standard, schema, compliance",
                status = "Active",
            ),
            BenchmarkRecord(
                benchmarkId = BENCHMARK_DMP_FAIRNESS,
                title = "DMP FAIRness",
                description = "FAIRness of outputs and services addressed in the DMP along with the DMP itself.",
                version = "0.0.1",
                hasAssociatedMetric = listOf(METRIC_FAIR_DMP_PID),
                abbreviation = "DMP FAIR",
                landingPage = "www.testing.com",
                keyword = "DMP",
                status = "Active",
            ),
            BenchmarkRecord(
                benchmarkId = BENCHMARK_RDM_COVERAGE,
                title = "Comprehensive RDM Activities Coverage",
                description = "Assesses the extent to which DMPs cover full research data management (RDM) lifecycle, roles, costs, data outputs, and best practices. Helps ensure alignment with funder and institutional policies.",
                version = "0.0.1",
                hasAssociatedMetric = listOf(METRIC_RDM_ROLES_DEFINED, METRIC_OUTPUT_METADATA_CHECK, METRIC_RDM_COST_DETAILS),
                abbreviation = "RDM-COVERAGE",
                keyword = "DMP, Benchmark, maDMP, RDM, policy, lifecycle",
                status = "Active",
            ),
            BenchmarkRecord(
                benchmarkId = BENCHMARK_FWF_ORD,
                title = "FWF Criteria for Open Research Data",
                description = "Benchmark derived from the FWF Austrian Science Fund criteria for open research data. All research data and its metadata must be findable, accessible, interoperable, and reusable (FAIR Principles). Repositories must be listed in RE3data or hold a recognised certification (e.g., CoreTrustSeal). Data must be deposited under open licenses allowing unrestricted reuse (e.g., CC BY or equivalent). Deposited datasets must be citable via a persistent identifier (e.g., DOI), in accordance with the Joint Declaration of Data Citation Principles.",
                version = "1.0.0",
                hasAssociatedMetric = listOf(METRIC_DATA_SHAR_COMP_1, METRIC_REPO_COMP_3),
                abbreviation = "FWF-ORD",
                landingPage = "https://www.fwf.ac.at/en/research-funding/open-access-policy/research-data-management",
                keyword = "FWF, open research data, FAIR, repository, RE3data, open license, persistent identifier, DOI",
                status = "Active",
            ),
            BenchmarkRecord(
                benchmarkId = BENCHMARK_FAIR_CHAMPION,
                title = "FAIR Champion Benchmark",
                description = "Benchmark that evaluates the FAIRness of maDMP datasets using the FAIR Champion external service.",
                version = "1.0.0",
                hasAssociatedMetric = listOf(METRIC_FC_FAIRNESS),
                abbreviation = "FC-BENCHMARK",
                keyword = "FAIR, benchmark, dataset",
                status = "active",
            ),
        )
        seedIfMissing(benchmarks, "benchmarks") { it.benchmarkId }
    }

    private inline fun <reified T : Any> seedIfMissing(records: List<T>, collectionName: String, idOf: (T) -> String?) {
        for (record in records) {
            val id = idOf(record) ?: continue
            val exists = mongoTemplate.exists<T>(Query(Criteria.where("_id").`is`(id)), collectionName)
            if (!exists) {
                mongoTemplate.insert(record, collectionName)
                log.info("BaseEntitySeeder: inserted '$id' into '$collectionName'.")
            } else {
                log.info("BaseEntitySeeder: '$id' already exists in '$collectionName', skipping.")
            }
        }
    }
}
