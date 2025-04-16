package io.github.ostrails.dmpevaluatorservice.service

import io.github.ostrails.dmpevaluatorservice.database.model.BenchmarkRecord
import io.github.ostrails.dmpevaluatorservice.database.repository.BenchmarkRepository
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.data.annotation.Id
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
            metrics = benchmark.metrics,
        )
        return benchmarkRepository.save(newBenchmark).awaitSingle()
    }

    suspend fun getBenchmarks():List<BenchmarkRecord> {
        return benchmarkRepository.findAll().asFlow().toList()
    }

    suspend fun addMetric(benchmarkId: String, metricsId: List<String>): BenchmarkRecord {
        val benchmark = benchmarkRepository.findById(benchmarkId).awaitSingle()
        val updateBenchmark = benchmark.copy(metrics = benchmark.metrics?.plus(metricsId))
        return benchmarkRepository.save(updateBenchmark).awaitSingle()
    }


}