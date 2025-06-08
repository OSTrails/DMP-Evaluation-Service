package io.github.ostrails.dmpevaluatorservice.service

import io.github.ostrails.dmpevaluatorservice.database.model.MetricRecord
import io.github.ostrails.dmpevaluatorservice.database.repository.BenchmarkRepository
import io.github.ostrails.dmpevaluatorservice.database.repository.MetricRepository
import io.github.ostrails.dmpevaluatorservice.exceptionHandler.ResourceNotFoundException
import io.github.ostrails.dmpevaluatorservice.model.metric.*
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Service
import java.util.*

@Service
class MetricService(
    val metricRepository: MetricRepository,
    val benchmarkRepository: BenchmarkRepository,
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


    suspend fun getMetricDetailJsonLD(metricId: String): MetricJsonLD {
        val metric = metricRepository.findById(metricId).awaitFirstOrNull() ?: throw ResourceNotFoundException("There is no record with id $metricId")
        val result = metricJsonLD(metric)
        return result
    }

    suspend fun metricJsonLD(metric: MetricRecord): MetricJsonLD {
        val benchMarksIds = metric.hasBenchmark
        val benchmarks = benchMarksIds?.let { benchmarkRepository.findAllByBenchmarkIdIn(it).collectList().awaitSingle() }
            ?: listOf()
        val graphEntry = metric.let {
            MetricGraphEntry(
                id = "urn:dmpEvaluationService:${it.id}",
                type = "dqv:Metric",
                title = LangLiteral("en", metric.title),
                description = LangLiteral("en", metric.description),
                version = metric.version,
                label = metric.abbreviation,
                abbreviation = metric.abbreviation,
                landingPage = (it.landingPage ?: it.landingPage)?.let { it1 -> IdWrapper(it1) },
                keyword = metric.keyword?.split(",")?.map { LangLiteral("en", it.trim()) },
                hasTest = metric.testAssociated?.map { IdWrapper("urn:dmpEvaluationService:${it}") } ?: listOf(),
                hasBenchmark = metric.hasBenchmark?.map { IdWrapper("urn:dmpEvaluationService:${it}")  } ?: listOf()  ,
                license = IdWrapper("http://creativecommons.org/licenses/by/2.0/"),
            )
        }
        val benchmarkgrapgh = benchmarks.map { BenchmarkMini(
            id = "urn:dmpEvaluationService:${it.benchmarkId}",
            title = "benchmark ${it.title}",
            description = it.description,
        )  }

        return MetricJsonLD (graph = benchmarkgrapgh + graphEntry)
    }
}

