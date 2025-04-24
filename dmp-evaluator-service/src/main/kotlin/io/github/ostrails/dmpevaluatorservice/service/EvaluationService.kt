package io.github.ostrails.dmpevaluatorservice.service

import io.github.ostrails.dmpevaluatorservice.database.model.BenchmarkRecord
import io.github.ostrails.dmpevaluatorservice.database.model.TestRecord
import kotlinx.serialization.json.*
import io.github.ostrails.dmpevaluatorservice.exceptionHandler.ResourceNotFoundException
import org.springframework.stereotype.Service

@Service
class EvaluationService(
    private val metricService: MetricService,
    private val testService: TestService
) {

    suspend fun runBenchmark(maDMP: JsonObject, benchmark: BenchmarkRecord) {

        val testsToExecute = testsToExecute(benchmark)
    }

    suspend fun testsToExecute(benchmark: BenchmarkRecord): List<TestRecord> {
        val metricsIds = benchmark.metrics ?: throw ResourceNotFoundException("Metrics for ${benchmark.title} is empty")
        val metrics = metricService.findMultipleMetrics(metricsIds)
        val testsIds = metrics.mapNotNull { it.testAssociated }.flatten()
        val tests = testService.findMultipleTests(testsIds)
        return tests
    }


}
