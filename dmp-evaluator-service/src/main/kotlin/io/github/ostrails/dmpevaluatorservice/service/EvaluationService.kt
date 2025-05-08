package io.github.ostrails.dmpevaluatorservice.service

import io.github.ostrails.dmpevaluatorservice.database.model.BenchmarkRecord
import io.github.ostrails.dmpevaluatorservice.database.model.Evaluation
import io.github.ostrails.dmpevaluatorservice.database.model.TestRecord
import io.github.ostrails.dmpevaluatorservice.exceptionHandler.ResourceNotFoundException
import io.github.ostrails.dmpevaluatorservice.plugin.EvaluatorPlugin
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.JsonObject
import org.springframework.stereotype.Service
import org.springframework.plugin.core.PluginRegistry
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Service
class EvaluationService(
    private val metricService: MetricService,
    private val testService: TestService,
    private val pluginRegistry: PluginRegistry<EvaluatorPlugin, String>,
) {
    private val log: Logger = LoggerFactory.getLogger(EvaluationService::class.java)


    suspend fun testsToExecute(benchmark: BenchmarkRecord): List<TestRecord> {
        val metricsIds = benchmark.metrics ?: throw ResourceNotFoundException("Metrics for ${benchmark.title} is empty")
        val metrics = metricService.findMultipleMetrics(metricsIds)
        val testsIds = metrics.mapNotNull { it.testAssociated }.flatten()
        val tests = testService.findMultipleTests(testsIds)
        return tests
    }


    suspend fun generateTestsResults(benchmark: BenchmarkRecord, maDMP: JsonObject, reportId:String): List<Evaluation> = coroutineScope{
        val tests = testsToExecute(benchmark)
        val testsEvaluations = tests.mapNotNull { test ->
            val evaluatorId = test.evaluator ?: return@mapNotNull null
            val functionName = test.functionEvaluator ?: return@mapNotNull null
            val plugin = pluginRegistry.getPluginFor(evaluatorId).orElse(null) ?: return@mapNotNull null
            val functionTest = plugin.functionMap[functionName] ?: return@mapNotNull null
            async<Evaluation?> {
                try {
                    functionTest(maDMP, reportId)
                }catch (e:Exception){
                    log.error("Error running test: ${test.title} ", e)
                    null
                }
            }
        }
        testsEvaluations.awaitAll().filterNotNull()
    }

}
