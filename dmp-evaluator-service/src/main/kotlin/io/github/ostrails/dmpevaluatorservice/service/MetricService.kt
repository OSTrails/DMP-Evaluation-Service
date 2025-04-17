package io.github.ostrails.dmpevaluatorservice.service

import io.github.ostrails.dmpevaluatorservice.database.model.MetricRecord
import io.github.ostrails.dmpevaluatorservice.database.repository.MetricRepository
import io.github.ostrails.dmpevaluatorservice.exceptionHandler.ResourceNotFoundException
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Service
import kotlin.jvm.Throws

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

        return metricRepository.findById(metricId).awaitFirstOrNull() ?: throw ResourceNotFoundException("There is no metric with the ID $metricId")
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
        val metric = metricRepository.findById(metricId).awaitSingle()
        val testsToAdd = tests.filterNot { it in tests }
        val updateMetric = metric.copy(testAssociated = metric.testAssociated?.plus(testsToAdd) ?: tests)
        return metricRepository.save(updateMetric).awaitSingle()
    }
}

//val benchmark = benchmarkRepository.findById(benchmarkId).awaitSingle()
//val updateBenchmark = benchmark.copy(metrics = benchmark.metrics?.plus(metricsId))
//return benchmarkRepository.save(updateBenchmark).awaitSingle()