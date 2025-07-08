package io.github.ostrails.dmpevaluatorservice.service

import io.github.ostrails.dmpevaluatorservice.database.model.BenchmarkRecord
import io.github.ostrails.dmpevaluatorservice.database.model.MetricRecord
import io.github.ostrails.dmpevaluatorservice.database.repository.BenchmarkRepository
import io.github.ostrails.dmpevaluatorservice.exceptionHandler.DatabaseException
import io.github.ostrails.dmpevaluatorservice.exceptionHandler.ResourceNotFoundException
import io.github.ostrails.dmpevaluatorservice.model.benchmark.*
import io.github.ostrails.dmpevaluatorservice.model.metric.LangLiteral
import io.github.ostrails.dmpevaluatorservice.utils.ConfigurationBenchmarkVariables
import io.github.ostrails.dmpevaluatorservice.utils.ConfigurationMetricVariables
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Service
import java.util.*

@Service
class BenchmarService(
    private val benchmarkRepository: BenchmarkRepository,
    private val metricService: MetricService,
    val configurationBenchmarkVariables: ConfigurationBenchmarkVariables,
    val configurationMetricVariables: ConfigurationMetricVariables,
){

    suspend fun createBenchmark(benchmark: BenchmarkRecord): BenchmarkRecord {
        return benchmarkRepository.save(benchmark).awaitSingle()
    }

    suspend fun getBenchmarks():List<BenchmarkRecord> {
        try {
            return benchmarkRepository.findAll().asFlow().toList()
        }catch (e:Exception){
            throw DatabaseException("There is a error with the database trying to get the benchmarks ${e.message}")
        }
    }

    suspend fun getBenchmarksIds():List<String> {
        try {
            val benchmarks =  benchmarkRepository.findAll().asFlow().toList()
            return benchmarks.map { configurationBenchmarkVariables.endpointURL + "/" + it.benchmarkId.toString() }
        }catch (e:Exception){
            throw DatabaseException("There is a error with the database trying to get the benchmarks ${e.message}")
        }
    }

    suspend fun addMetric(benchmarkId: String, metricsId: List<String>): BenchmarkRecord {
        val metricToAdd:List<String>
        val benchmark = benchmarkRepository.findById(benchmarkId).awaitFirstOrNull() ?: throw ResourceNotFoundException("There is no record with id $benchmarkId")
        if (benchmark.hasAssociatedMetric !=  null){
            metricToAdd = metricsId.filterNot { it in benchmark.hasAssociatedMetric }
        }else metricToAdd = metricsId
        val updateBenchmark = benchmark.copy(hasAssociatedMetric = benchmark.hasAssociatedMetric?.plus(metricToAdd) ?: metricsId)
        return benchmarkRepository.save(updateBenchmark).awaitSingle()
    }

    suspend fun deleteMetric(benchmarkId: String, metricsId: List<String>): BenchmarkRecord {
        val benchmark = benchmarkRepository.findById(benchmarkId).awaitFirstOrNull() ?: throw ResourceNotFoundException("Metric with id $benchmarkId not found")
        if (benchmark.hasAssociatedMetric != null && benchmark.hasAssociatedMetric.isNotEmpty()) {
            val metricFiltered = benchmark.hasAssociatedMetric.filterNot { it in metricsId }
            val updateBenchmark = benchmark.copy(hasAssociatedMetric = metricFiltered ?: null)
            return benchmarkRepository.save(updateBenchmark).awaitSingle()
        }else {
            throw ResourceNotFoundException("There is not metric to delete in this benchmark")
        }
    }

    suspend fun deleteBenchmark(benchmarkId: String): String? {
        val benchmark = benchmarkRepository.findById(benchmarkId).awaitFirstOrNull() ?: throw ResourceNotFoundException("There is no benchmark with the ID $benchmarkId")
        try {
            benchmarkRepository.delete(benchmark).awaitFirstOrNull()
        }catch (e:Exception){
            throw DatabaseException("There is a error with the database trying to delete the record ${benchmarkId}   ${e.message}")
        }
        return benchmarkId

    }

    suspend fun getBenchmarkDetail(benchmarkId: String): BenchmarkRecord{
        return benchmarkRepository.findById(benchmarkId).awaitFirstOrNull() ?: throw ResourceNotFoundException("There is no benchmark with the ID $benchmarkId")
    }

    suspend fun getBenchmarskDetail(benchmarkIds: List<String>): List<BenchmarkRecord>{
        return benchmarkRepository.findAllByBenchmarkIdIn(benchmarkIds).collectList().awaitSingle() ?: throw ResourceNotFoundException("There is no benchmark with the ID $benchmarkIds")
    }

    suspend fun benchmarkByTitle(title: String): BenchmarkRecord {
        return benchmarkRepository.findByTitle(title).awaitFirstOrNull() ?: throw ResourceNotFoundException("There is no benchmark with the title $title")
    }

    suspend fun getBenchmarkDetailJsonLD(benchmarkId: String): BenchmarkJsonLD{
        val benchmark = benchmarkRepository.findById(benchmarkId).awaitFirstOrNull() ?: throw ResourceNotFoundException("There is no record with id $benchmarkId")
        val result = toJsonLD(benchmark)
        return result
    }



    suspend fun updateBenchmark(benchmarkId: String, benchmarkRequest: BenchmarkUpdateRequest): BenchmarkRecord {
        val benchmark = benchmarkRepository.findById(benchmarkId).awaitFirstOrNull() ?: throw ResourceNotFoundException("There is no record with id $benchmarkId")
        val updateBenchmark = benchmark.copy(
            title = benchmarkRequest.title ?: benchmark.title,
            version = benchmarkRequest.version ?: benchmark.version,
            description = benchmarkRequest.description ?: benchmark.description,
            keyword = benchmarkRequest.keyword ?: benchmark.keyword,
            abbreviation = benchmarkRequest.abbreviation ?: benchmark.abbreviation,
            landingPage = benchmarkRequest.landingPage ?: benchmark.landingPage,
            theme = benchmarkRequest.theme ?: benchmark.theme,
            status = benchmarkRequest.status ?: benchmark.status,
            creator = benchmarkRequest.creator ?: benchmark.creator
            )
        return benchmarkRepository.save(updateBenchmark).awaitSingle()
    }


    suspend fun toJsonLD(benchmark: BenchmarkRecord): BenchmarkJsonLD {
        val graphEntry = BenchmarkGraphEntry(
            id = configurationBenchmarkVariables.endpointURL + "/" + benchmark.benchmarkId,
            title = LangLiteral("en",  benchmark.title),
            description = LangLiteral("en", benchmark.description),
            version = benchmark.version,
            label = benchmark.abbreviation,
            abbreviation = benchmark.abbreviation,
            status = benchmark.status,
            landingPage = benchmark.landingPage?.let { IdWrapper("urn:dmpEvaluationService:${it}") },
            keyword = benchmark.keyword?.split(",")?.map { LangLiteral("en", it.trim()) },
            associatedMetric = benchmark.hasAssociatedMetric?.map { IdWrapper(configurationMetricVariables.endpointURL + "/" + it) },
            hasAlgorithm = benchmark.algorithms?.map { IdWrapper(it) }
        )

        val metrics = benchmark.hasAssociatedMetric?.let { metricService.findMultipleMetrics(benchmark.hasAssociatedMetric) }
        val metricsLD = metrics?.map(::toMetricLDEntry)

        if (metricsLD != null) {
            return BenchmarkJsonLD (graph = metricsLD + graphEntry)
        }else {
            return BenchmarkJsonLD (graph = listOf(graphEntry))
        }
    }

    fun toMetricLDEntry(metric: MetricRecord): MetricLDEntry {
        val id = configurationMetricVariables.endpointURL + "/" + metric.id
        return MetricLDEntry(
            id = id,
            identifier = IdWrapper(id),
            label = metric.title,
            abbreviation = metric.abbreviation ?: metric.title
        )
    }

}