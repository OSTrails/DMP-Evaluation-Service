package io.github.ostrails.dmpevaluatorservice.service

import io.github.ostrails.dmpevaluatorservice.database.model.MetricRecord
import io.github.ostrails.dmpevaluatorservice.database.repository.MetricRepository
import io.github.ostrails.dmpevaluatorservice.exceptionHandler.ResourceNotFoundException
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Service

@Service
class MetricService(
    val metricRepository: MetricRepository
) {

    suspend fun createMetric(metric: MetricRecord): MetricRecord {
        return metricRepository.save(metric).awaitSingle()
    }

    suspend fun listMetrics(): List<MetricRecord> {
        return metricRepository.findAll().collectList().awaitSingle()
    }

    suspend fun metricDetail(metricId: String): MetricRecord {
        return metricRepository.findById(metricId).awaitSingle() ?: throw ResourceNotFoundException("Metric with id $metricId not found")
    }

    suspend fun deleteMetric(metricId: String): String? {
        val record = metricRepository.findById(metricId).awaitFirstOrNull()
        if (record != null) {
            metricRepository.deleteById(metricId).awaitFirstOrNull()
            return record.id
        }else{
            return null
        }
    }

    suspend fun addTests(metricId: String, tests:List<String>): MetricRecord {
        val testsToAdd: List<String>
        val metric = metricRepository.findById(metricId).awaitFirstOrNull() ?: throw ResourceNotFoundException("Metric with id $metricId not found")
        if (metric.testAssociated != null) {
            testsToAdd = tests.filterNot { it in metric.testAssociated }
        }else testsToAdd = tests
        val updateMetric = metric.copy(testAssociated = metric.testAssociated?.plus(testsToAdd) ?: tests)
        return metricRepository.save(updateMetric).awaitSingle()
    }

    suspend fun deleteTest(metricId: String, tests: List<String>): MetricRecord{
        val metric = metricRepository.findById(metricId).awaitFirstOrNull() ?: throw ResourceNotFoundException("Metric with id $metricId not found")
        if (metric.testAssociated != null && metric.testAssociated.isNotEmpty()) {
            val testFiltered = metric.testAssociated.filterNot { it in tests }
            val updateMetric = metric.copy(testAssociated = testFiltered)
            return metricRepository.save(updateMetric).awaitSingle()
        }else {
            throw ResourceNotFoundException("There is not tests to delete in this metric")
        }
    }

    suspend fun findMultipleMetrics(metricIds: List<String>): List<MetricRecord> {
        val metrics = metricRepository.findByIdIn(metricIds).collectList().awaitSingle() ?: throw ResourceNotFoundException("Metrics with the ids ${metricIds} not found")
        return metrics
    }

    suspend fun addBenchMark(metricId: String, benchMarkIds: List<String>): MetricRecord{
        val benchmarkToAdd: List<String>
        val metric = metricRepository.findById(metricId).awaitFirstOrNull() ?: throw ResourceNotFoundException("Metric with id $metricId not found")
        if (metric.hasBenchmark != null) {
            benchmarkToAdd = benchMarkIds.filterNot { it in metric.hasBenchmark }
        }else benchmarkToAdd = benchMarkIds
        val updateMetric = metric.copy(hasBenchmark = metric.hasBenchmark?.plus(benchmarkToAdd) ?: benchMarkIds)
        return metricRepository.save(updateMetric).awaitSingle()
    }
}

