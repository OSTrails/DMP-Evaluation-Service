package io.github.ostrails.dmpevaluatorservice.service

import io.github.ostrails.dmpevaluatorservice.database.model.BenchmarkRecord
import io.github.ostrails.dmpevaluatorservice.database.repository.BenchmarkRepository
import io.github.ostrails.dmpevaluatorservice.exceptionHandler.DatabaseException
import io.github.ostrails.dmpevaluatorservice.exceptionHandler.ResourceNotFoundException
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Service

@Service
class BenchmarService(
    private val benchmarkRepository: BenchmarkRepository,
){

    suspend fun createBenchmark(benchmark: BenchmarkRecord): BenchmarkRecord {
        val newBenchmark = BenchmarkRecord(
            description = benchmark.description,
            title = benchmark.title,
            version = benchmark.version,
            hasAssociatedMetric = benchmark.hasAssociatedMetric,
        )
        return benchmarkRepository.save(newBenchmark).awaitSingle()
    }

    suspend fun getBenchmarks():List<BenchmarkRecord> {
        try {
            return benchmarkRepository.findAll().asFlow().toList()
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

    suspend fun getbenchmarkByTitle(title: String): BenchmarkRecord {
        return benchmarkRepository.findByTitle(title).awaitFirstOrNull() ?: throw ResourceNotFoundException("There is no benchmark with the title $title")
    }

}