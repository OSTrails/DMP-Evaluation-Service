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
