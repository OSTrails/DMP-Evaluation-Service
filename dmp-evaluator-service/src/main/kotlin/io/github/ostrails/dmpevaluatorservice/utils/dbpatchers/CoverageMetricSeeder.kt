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
