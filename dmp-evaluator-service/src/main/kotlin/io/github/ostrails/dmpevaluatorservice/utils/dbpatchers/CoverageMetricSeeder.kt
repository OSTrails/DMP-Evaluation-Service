package io.github.ostrails.dmpevaluatorservice.utils.dbpatchers

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

// One OSTrails pilot metric implemented by one DCSCoverageEvaluator function. Each spec seeds a TestRecord
// and a MetricRecord with fixed ids, so seeding stays idempotent across deployments.
data class CoverageMetricSpec(
    val metricId: String,
    val testId: String,
    val abbreviation: String,
    val title: String,
    val description: String,
    val function: String,
    val keyword: String,
    val isApplicableFor: String = "https://vocabularies.coar-repositories.org/resource_types/c_ab20/",
)

@Component
class CoverageMetricSeeder(
    private val mongoTemplate: MongoTemplate,
) {
    companion object {
        private val log: Logger = LoggerFactory.getLogger(CoverageMetricSeeder::class.java)

        const val EVALUATOR = "DCSCoverageEvaluator"
        private const val SUPPORTED_BY = "https://github.com/OSTrails/DMP-Evaluation-Service"
        private const val DATASET = "https://schema.org/Dataset"

        val specs: List<CoverageMetricSpec> = listOf(
            CoverageMetricSpec(
                metricId = "6ac62fd3616d0a2f64000001", testId = "6ac62fd3616d0a2f64000002",
                abbreviation = "data.lice.co.1",
                title = "Dataset License Declared",
                description = "Checks that every dataset in the maDMP declares a license (license_ref) on at least one of its distributions. Fails if any dataset has no distributions or none of them declares a license.",
                function = "datasetLicenseDeclared",
                keyword = "license, dataset, distribution, DCS",
                isApplicableFor = DATASET,
            ),
            CoverageMetricSpec(
                metricId = "6ac62fd3616d0a2f64000003", testId = "6ac62fd3616d0a2f64000004",
                abbreviation = "data.shar.co.1",
                title = "Data License is Present",
                description = "Checks that every distribution of every dataset in the maDMP declares a license (license_ref). Fails if any distribution has no license or a dataset has no distributions.",
                function = "distributionLicensePresent",
                keyword = "license, distribution, data sharing, DCS",
                isApplicableFor = DATASET,
            ),
            CoverageMetricSpec(
                metricId = "6ac62fd3616d0a2f64000005", testId = "6ac62fd3616d0a2f64000006",
                abbreviation = "store.cov.1",
                title = "Data Storage Location mentioned in the DMP",
                description = "Checks that every distribution of every dataset declares where it is stored: a host with a title, url or host_id (DCS 1.3). Fails if any distribution has no host, or a host without any of these fields.",
                function = "storageLocationDeclared",
                keyword = "storage, host, repository, distribution, DCS",
                isApplicableFor = DATASET,
            ),
            CoverageMetricSpec(
                metricId = "6ac62fd3616d0a2f64000007", testId = "6ac62fd3616d0a2f64000008",
                abbreviation = "data.info.cov.2",
                title = "Dataset File Format Specified",
                description = "Checks that every distribution of every dataset in the maDMP specifies at least one file format (format). Fails if any distribution has no format or a dataset has no distributions.",
                function = "distributionFormatSpecified",
                keyword = "file format, media type, distribution, DCS",
                isApplicableFor = DATASET,
            ),
            CoverageMetricSpec(
                metricId = "6ac62fd3616d0a2f64000009", testId = "6ac62fd3616d0a2f6400000a",
                abbreviation = "repo.feas.2",
                title = "Long-Term Preservation Dataset",
                description = "Checks that every dataset in the maDMP provides a long-term preservation statement (preservation_statement). Fails if any dataset has no preservation statement.",
                function = "preservationStatementPresent",
                keyword = "preservation, long-term, dataset, DCS",
                isApplicableFor = DATASET,
            ),
            CoverageMetricSpec(
                metricId = "6ac62fd3616d0a2f6400000b", testId = "6ac62fd3616d0a2f6400000c",
                abbreviation = "ethics.co.1",
                title = "Ethical Issues Status Declared",
                description = "Checks that the maDMP declares whether ethical issues exist (ethical_issues_exist). Passes for 'yes' or 'no', is indeterminate for 'unknown', and fails if the field is missing or invalid.",
                function = "ethicalIssuesStatusDeclared",
                keyword = "ethics, ethical issues, DMP, DCS",
            ),
            CoverageMetricSpec(
                metricId = "6ac62fd3616d0a2f6400000d", testId = "6ac62fd3616d0a2f6400000e",
                abbreviation = "secur.co.1",
                title = "Security Measures Implementation",
                description = "Checks that every dataset in the maDMP declares at least one security and privacy measure (security_and_privacy with a title). Fails if any dataset declares none.",
                function = "securityMeasuresDeclared",
                keyword = "security, privacy, dataset, DCS",
                isApplicableFor = DATASET,
            ),
            CoverageMetricSpec(
                metricId = "6ac62fd3616d0a2f6400000f", testId = "6ac62fd3616d0a2f64000010",
                abbreviation = "store.comp.1",
                title = "Alignment of Storage and Backup with Information Sensitivity",
                description = "Checks that every dataset declaring personal or sensitive data ('yes') also declares security and privacy measures (security_and_privacy). Datasets declaring 'no' for both are skipped; 'unknown' or missing flags are indeterminate, as is a DMP with no such datasets.",
                function = "sensitiveDataProtected",
                keyword = "sensitive data, personal data, security, storage, DCS",
                isApplicableFor = DATASET,
            ),
            CoverageMetricSpec(
                metricId = "6ac62fd3616d0a2f64000011", testId = "6ac62fd3616d0a2f64000012",
                abbreviation = "data.pid.cov.1",
                title = "Repository Supports Persistent Identifiers for Datasets",
                description = "Checks that the repository (host) of every distribution declares at least one persistent identifier system (pid_system, e.g. doi or handle). Fails if any distribution has no host or its host declares no PID system.",
                function = "repositoryPidSystemDeclared",
                keyword = "persistent identifier, PID system, repository, host, DCS",
                isApplicableFor = DATASET,
            ),
            CoverageMetricSpec(
                metricId = "6ac62fd3616d0a2f64000013", testId = "6ac62fd3616d0a2f64000014",
                abbreviation = "data.info.cov.1",
                title = "Dataset Type Specified",
                description = "Checks that every dataset in the maDMP specifies its type (type). Fails if any dataset has no type.",
                function = "datasetTypeSpecified",
                keyword = "dataset type, dataset, DCS",
                isApplicableFor = DATASET,
            ),
            CoverageMetricSpec(
                metricId = "6ac62fd3616d0a2f64000015", testId = "6ac62fd3616d0a2f64000016",
                abbreviation = "data.info.cov.3",
                title = "Dataset Size Specified",
                description = "Checks that every distribution of every dataset in the maDMP specifies its size in bytes (byte_size, a non-negative integer). Fails if any distribution has no or an invalid size, or a dataset has no distributions.",
                function = "distributionSizeSpecified",
                keyword = "size, byte size, distribution, DCS",
                isApplicableFor = DATASET,
            ),
            CoverageMetricSpec(
                metricId = "6ac62fd3616d0a2f64000017", testId = "6ac62fd3616d0a2f64000018",
                abbreviation = "meta.stand.comp.1",
                title = "Metadata Standards Used",
                description = "Checks that every dataset in the maDMP declares at least one metadata standard (metadata[].metadata_standard_id). Fails if any dataset declares none.",
                function = "metadataStandardDeclared",
                keyword = "metadata standard, metadata, dataset, DCS",
                isApplicableFor = DATASET,
            ),
            CoverageMetricSpec(
                metricId = "6ac62fd3616d0a2f64000019", testId = "6ac62fd3616d0a2f6400001a",
                abbreviation = "data.exteresource.co.2",
                title = "Metadata Standard Specified in the DMP",
                description = "Checks that the maDMP specifies at least one metadata standard (metadata[].metadata_standard_id) on any of its datasets. Fails if no dataset declares a metadata standard.",
                function = "metadataStandardInDmp",
                keyword = "metadata standard, external resources, DMP, DCS",
            ),
            CoverageMetricSpec(
                metricId = "6ac62fd3616d0a2f6400001b", testId = "6ac62fd3616d0a2f6400001c",
                abbreviation = "qc.qual.1",
                title = "Quality Control Methods Stated",
                description = "Checks that every dataset in the maDMP states at least one quality control method (data_quality_assurance). Fails if any dataset states none.",
                function = "qualityAssuranceDeclared",
                keyword = "quality control, quality assurance, dataset, DCS",
                isApplicableFor = DATASET,
            ),
            CoverageMetricSpec(
                metricId = "6ac62fd3616d0a2f6400001d", testId = "6ac62fd3616d0a2f6400001e",
                abbreviation = "store.co.1",
                title = "Back up Frequency",
                description = "Checks that the host of every distribution states its backup frequency (host.backup_frequency). Fails if any distribution has no host or its host states no backup frequency.",
                function = "backupFrequencyDeclared",
                keyword = "backup, storage, host, DCS",
                isApplicableFor = DATASET,
            ),
            CoverageMetricSpec(
                metricId = "6ac62fd3616d0a2f6400001f", testId = "6ac62fd3616d0a2f64000020",
                abbreviation = "role.pid.co.1",
                title = "Contributors and Organisations PIDs",
                description = "Checks that every contributor in the maDMP is identified by a persistent identifier (contributor_id of type orcid or isni) and that every declared affiliation has an organisation identifier (affiliation_id of type ror, grid or isni, DCS 1.3). Fails if there are no contributors or any contributor or affiliation lacks a PID.",
                function = "contributorPidsDeclared",
                keyword = "contributor, ORCID, ROR, persistent identifier, affiliation, DCS",
            ),
            CoverageMetricSpec(
                metricId = "6ac62fd3616d0a2f64000021", testId = "6ac62fd3616d0a2f64000022",
                abbreviation = "repo.co.5",
                title = "Certification of Repository",
                description = "Checks that the host of every distribution declares a recognised repository certification (host.certified_with, e.g. coretrustseal). Fails if any distribution has no host, or its host declares no or an unrecognised certification.",
                function = "repositoryCertified",
                keyword = "certification, CoreTrustSeal, repository, host, DCS",
                isApplicableFor = DATASET,
            ),
            CoverageMetricSpec(
                metricId = "6ac62fd3616d0a2f64000023", testId = "6ac62fd3616d0a2f64000024",
                abbreviation = "data.pid.cov.2",
                title = "Trusted Repository Referenced",
                description = "Checks that the host of every distribution references its re3data registry entry (host.host_id of type re3data, DCS 1.3). Fails if any distribution has no host or its host has no re3data identifier. Registration itself is verified online by the RE3data compliance test.",
                function = "trustedRepositoryReferenced",
                keyword = "re3data, trusted repository, host identifier, DCS",
                isApplicableFor = DATASET,
            ),
            CoverageMetricSpec(
                metricId = "6ac62fd3616d0a2f64000025", testId = "6ac62fd3616d0a2f64000026",
                abbreviation = "data.shar.op.1",
                title = "Data Access Status Open for the Dataset",
                description = "Checks that every distribution of every dataset in the maDMP is openly accessible (data_access is 'open'). Fails if any distribution is restricted, closed, shared (deprecated) or declares no data access.",
                function = "distributionAccessOpen",
                keyword = "open access, data access, distribution, DCS",
                isApplicableFor = DATASET,
            ),
        )
    }

    @PostConstruct
    fun seed() {
        specs.forEach { spec ->
            seedIfMissing(spec.toTestRecord(), "tests", spec.testId)
            seedIfMissing(spec.toMetricRecord(), "metrics", spec.metricId)
        }
    }

    private fun CoverageMetricSpec.toTestRecord() = TestRecord(
        id = testId,
        title = title,
        description = description,
        license = "https://creativecommons.org/licenses/by/4.0/",
        version = "1.0.0",
        keyword = keyword,
        abbreviation = abbreviation,
        status = "active",
        supportedBy = SUPPORTED_BY,
        isApplicableFor = isApplicableFor,
        evaluator = EVALUATOR,
        functionEvaluator = function,
        metricImplemented = metricId,
    )

    private fun CoverageMetricSpec.toMetricRecord() = MetricRecord(
        id = metricId,
        title = title,
        description = description,
        version = "1.0.0",
        testAssociated = listOf(testId),
        abbreviation = abbreviation,
        supportedBy = SUPPORTED_BY,
        isApplicableFor = isApplicableFor,
        keyword = keyword,
        hasBenchmark = emptyList(),
        license = "http://creativecommons.org/licenses/by/2.0/",
        status = "active",
    )

    private inline fun <reified T : Any> seedIfMissing(record: T, collectionName: String, id: String) {
        if (mongoTemplate.exists<T>(Query(Criteria.where("_id").`is`(id)), collectionName)) {
            log.info("CoverageMetricSeeder: '$id' already exists in '$collectionName', skipping.")
        } else {
            mongoTemplate.insert(record, collectionName)
            log.info("CoverageMetricSeeder: inserted '$id' into '$collectionName'.")
        }
    }
}
