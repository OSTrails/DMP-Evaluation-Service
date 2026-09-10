package io.github.ostrails.dmpevaluatorservice.service

import io.github.ostrails.dmpevaluatorservice.auth.checkOwnership
import io.github.ostrails.dmpevaluatorservice.database.model.MetricRecord
import io.github.ostrails.dmpevaluatorservice.database.repository.MetricRepository
import io.github.ostrails.dmpevaluatorservice.exceptionHandler.ResourceNotFoundException
import io.github.ostrails.dmpevaluatorservice.model.metric.*
import io.github.ostrails.dmpevaluatorservice.utils.ConfigurationBenchmarkVariables
import io.github.ostrails.dmpevaluatorservice.utils.ConfigurationMetricVariables
import io.github.ostrails.dmpevaluatorservice.utils.ConfigurationTestVariables
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.awaitSingle
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class MetricService(
    val metricRepository: MetricRepository,
    val configurationMetricVariables: ConfigurationMetricVariables,
    val configurationTestVariables: ConfigurationTestVariables,
    val configurationBenchmarkVariables: ConfigurationBenchmarkVariables
) {

    private val log: Logger = LoggerFactory.getLogger(MetricService::class.java)

    suspend fun createMetric(request: MetricCreateRequest, callerClientId: String): MetricRecord {
        val metric = MetricRecord(
            title = request.title,
            description = request.description,
            version = request.version,
            testAssociated = null,
            keyword = request.keyword,
            abbreviation = request.abbreviation,
            landingPage = request.landingPage,
            theme = request.theme,
            status = request.status,
            isApplicableFor = request.isApplicableFor,
            supportedBy = request.supportedBy,
            hasBenchmark = null,
            license = request.license,
            inDimension = request.inDimension,
            createdBy = callerClientId
        )
        val saved = metricRepository.save(metric).awaitSingle()
        log.info("Created metric '${saved.id}'")
        return saved
    }

    suspend fun listMetrics(): List<MetricRecord> {
        val metrics = metricRepository.findAll().collectList().awaitSingle()
        return metrics.map {
            it.copy(
                hasBenchmark = it.hasBenchmark?.map { configurationBenchmarkVariables.endpointURL + "/" + it },
                testAssociated = it.testAssociated?.map { configurationTestVariables.endpointURL + "/" + it }
            )
        }
    }

    suspend fun listMetricsIds(): List<String?> {
        val metrics = metricRepository.findAll().collectList().awaitSingle()
        return metrics.map { configurationMetricVariables.endpointURL + "/" + it.id }
    }

    suspend fun metricDetail(metricId: String): MetricRecord {
        return metricRepository.findById(metricId).awaitSingle() ?: run {
            log.warn("Metric '$metricId' not found")
            throw ResourceNotFoundException("Metric with id $metricId not found")
        }
    }

    suspend fun updateMetric(metricId: String, metricRequest: MetricUpdateRequest, callerClientId: String, isAdmin: Boolean): MetricRecord {
        val metric = metricRepository.findById(metricId).awaitFirstOrNull() ?: run {
            log.warn("Metric '$metricId' not found")
            throw ResourceNotFoundException("Metric with id $metricId not found")
        }
        checkOwnership(metric.createdBy, callerClientId, isAdmin)
        log.debug("Updating metric '$metricId'")
        val updateMetric = metric.copy(
            title = metricRequest.title ?: metric.title,
            version = metricRequest.version ?: metric.version,
            description = metricRequest.description ?: metric.description,
            keyword = metricRequest.keyword ?: metric.keyword,
            abbreviation = metricRequest.abbreviation ?: metric.abbreviation,
            landingPage = metricRequest.landingPage ?: metric.landingPage,
            theme = metricRequest.theme ?: metric.theme,
            status = metricRequest.status ?: metric.status,
            isApplicableFor = metricRequest.isApplicableFor ?: metric.isApplicableFor,
            supportedBy = metricRequest.supportedBy ?: metric.supportedBy,
        )
        return metricRepository.save(updateMetric).awaitSingle()
    }

    suspend fun deleteMetric(metricId: String): String? {
        val record = metricRepository.findById(metricId).awaitFirstOrNull()
        if (record != null) {
            metricRepository.deleteById(metricId).awaitFirstOrNull()
            log.info("Deleted metric '$metricId'")
            return record.id
        } else {
            log.warn("Metric '$metricId' not found, nothing deleted")
            return null
        }
    }

    suspend fun addTests(metricId: String, tests: List<String>, callerClientId: String = "", isAdmin: Boolean = true): MetricRecord {
        val testsToAdd: List<String>
        val metric = metricRepository.findById(metricId).awaitFirstOrNull() ?: run {
            log.warn("Metric '$metricId' not found")
            throw ResourceNotFoundException("Metric with id $metricId not found")
        }
        if (callerClientId.isNotEmpty()) checkOwnership(metric.createdBy, callerClientId, isAdmin)
        if (metric.testAssociated != null) {
            testsToAdd = tests.filterNot { it in metric.testAssociated }
        } else testsToAdd = tests
        val updateMetric = metric.copy(testAssociated = metric.testAssociated?.plus(testsToAdd) ?: tests)
        log.debug("Adding ${testsToAdd.size} test(s) to metric '$metricId'")
        return metricRepository.save(updateMetric).awaitSingle()
    }

    suspend fun deleteTest(metricId: String, tests: List<String>, callerClientId: String, isAdmin: Boolean): MetricRecord {
        val metric = metricRepository.findById(metricId).awaitFirstOrNull() ?: run {
            log.warn("Metric '$metricId' not found")
            throw ResourceNotFoundException("Metric with id $metricId not found")
        }
        checkOwnership(metric.createdBy, callerClientId, isAdmin)
        if (metric.testAssociated != null && metric.testAssociated.isNotEmpty()) {
            val testFiltered = metric.testAssociated.filterNot { it in tests }
            val updateMetric = metric.copy(testAssociated = testFiltered)
            log.debug("Removing ${tests.size} test(s) from metric '$metricId'")
            return metricRepository.save(updateMetric).awaitSingle()
        } else {
            log.warn("Metric '$metricId' has no tests to delete")
            throw ResourceNotFoundException("There is not tests to delete in this metric")
        }
    }

    suspend fun findMultipleMetrics(metricIds: List<String>): List<MetricRecord> {
        val metrics = metricRepository.findByIdIn(metricIds).collectList().awaitSingle() ?: run {
            log.warn("No metrics found for ids $metricIds")
            throw ResourceNotFoundException("Metrics with the ids ${metricIds} not found")
        }
        return metrics
    }

    suspend fun addBenchMark(metricId: String, benchMarkIds: List<String>, callerClientId: String, isAdmin: Boolean): MetricRecord {
        val benchmarkToAdd: List<String>
        val metric = metricRepository.findById(metricId).awaitFirstOrNull() ?: run {
            log.warn("Metric '$metricId' not found")
            throw ResourceNotFoundException("Metric with id $metricId not found")
        }
        checkOwnership(metric.createdBy, callerClientId, isAdmin)
        if (metric.hasBenchmark != null) {
            benchmarkToAdd = benchMarkIds.filterNot { it in metric.hasBenchmark }
        } else benchmarkToAdd = benchMarkIds
        val updateMetric = metric.copy(hasBenchmark = metric.hasBenchmark?.plus(benchmarkToAdd) ?: benchMarkIds)
        log.debug("Adding ${benchmarkToAdd.size} benchmark(s) to metric '$metricId'")
        return metricRepository.save(updateMetric).awaitSingle()
    }

    suspend fun getMetricsJsonLD(): List<MetricJsonLD?> {
        val metrics = metricRepository.findAll().collectList().awaitSingle()
        return metrics.map { it.id?.let { id -> getMetricDetailJsonLD(id) } }
    }

    suspend fun getMetricDetailJsonLD(metricId: String): MetricJsonLD {
        val metric = metricRepository.findById(metricId).awaitFirstOrNull() ?: run {
            log.warn("Metric '$metricId' not found")
            throw ResourceNotFoundException("There is no record with id $metricId")
        }
        return metricJsonLD(metric)
    }

    suspend fun metricJsonLD(metric: MetricRecord): MetricJsonLD {
        val metricUrl = configurationMetricVariables.endpointURL + "/" + metric.id
        return MetricJsonLD(
            id = metricUrl,
            identifier = IdWrapper(metricUrl),
            type = "ftr:Metric",
            title = LangLiteral("en", metric.title),
            description = LangLiteral("en", metric.description),
            version = metric.version,
            label = metric.abbreviation,
            abbreviation = metric.abbreviation,
            landingPage = metric.landingPage?.let { IdWrapper(it) },
            keyword = metric.keyword?.split(",")?.map { LangLiteral("en", it.trim()) },
            hasTest = metric.testAssociated?.map { IdWrapper(configurationTestVariables.endpointURL + "/" + it) } ?: listOf(),
            hasBenchmark = metric.hasBenchmark?.map { IdWrapper(configurationBenchmarkVariables.endpointURL + "/" + it) } ?: listOf(),
            isApplicableFor = metric.isApplicableFor?.let { IdWrapper(it) },
            supportedBy = metric.supportedBy?.let { IdWrapper(it) },
            license = IdWrapper("http://creativecommons.org/licenses/by/2.0/"),
        )
    }

}
