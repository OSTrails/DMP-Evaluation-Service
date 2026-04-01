package io.github.ostrails.dmpevaluatorservice.service

import io.github.ostrails.dmpevaluatorservice.database.model.BenchmarkRecord
import io.github.ostrails.dmpevaluatorservice.database.model.Evaluation
import io.github.ostrails.dmpevaluatorservice.database.model.TestRecord
import io.github.ostrails.dmpevaluatorservice.exceptionHandler.ResourceNotFoundException
import io.github.ostrails.dmpevaluatorservice.model.metric.IdWrapper
import io.github.ostrails.dmpevaluatorservice.model.testResult.*
import io.github.ostrails.dmpevaluatorservice.plugin.EvaluatorPlugin
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.*
import org.springframework.stereotype.Service
import org.springframework.plugin.core.PluginRegistry
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*

@Service
class EvaluationService(
    private val metricService: MetricService,
    private val testService: TestService,
    private val pluginRegistry: PluginRegistry<EvaluatorPlugin, String>,
) {
    private val log: Logger = LoggerFactory.getLogger(EvaluationService::class.java)


    suspend fun testsToExecute(benchmark: BenchmarkRecord): List<TestRecord> {
        val metricsIds = benchmark.hasAssociatedMetric ?: throw ResourceNotFoundException("Metrics for ${benchmark.title} is empty")
        val metrics = metricService.findMultipleMetrics(metricsIds)
        val testsIds = metrics.mapNotNull { it.testAssociated }.flatten()
        val tests = testService.findMultipleTests(testsIds)
        return tests
    }


    suspend fun generateTestsResultsFromBenchmark(benchmark: BenchmarkRecord, maDMP: JsonObject, reportId:String): List<Evaluation> = coroutineScope{
        val tests = testsToExecute(benchmark)
        val testsEvaluations = tests.map { test ->
            async<Evaluation?> {
                generateTestResultFromTest(test, maDMP, reportId)
            }
        }
        testsEvaluations.awaitAll().filterNotNull()
    }

    suspend fun generateTestResultFromTest(test: TestRecord, maDMP: JsonObject, reportId:String): Evaluation? {
        val evaluatorId = test.evaluator ?: return null
        val functionName = test.functionEvaluator ?: return null
        val plugin = pluginRegistry.getPluginFor(evaluatorId).orElse(null) ?: return null
        val functionTest = plugin.functionMap[functionName] ?: return null
        return try {
            test.let { functionTest(maDMP, reportId, it) }
        }catch (e:Exception){
            log.error("Error running test: ${test.title} ", e)
            null
        }
    }

    fun buildEvalutionResultJsonLD(evaluation: Evaluation): TestResultJsonLD {
        val resultId = "urn:dmpEvaluationService:${evaluation.evaluationId}"
        val testId = evaluation.outputFromTest ?: "https://example.org/test/default"
        val resourceId = evaluation.generated?.takeIf { it.isNotBlank() } ?: "https://example.org/resource/default"
        val activityId = "urn:ostrails:testexecutionactivity:${UUID.randomUUID()}"

        val testResult = TestResultGraph(
            id = resultId,
            identifier = IdWrapper(resultId),
            title = LangLiteral(value = "${evaluation.title} OUTPUT"),
            description = LangLiteral(value = evaluation.details),
            license = IdWrapper("https://creativecommons.org/publicdomain/zero/1.0/"),
            resultValue = LangLiteral(value = evaluation.result.name.lowercase()),
            generatedAt = TypedLiteral("xsd:dateTime", evaluation.timestamp.toString()),
            log = LangLiteral(value = evaluation.log),
            completion = TypedLiteral("xsd:int", (evaluation.completion ?: 100).toString()),
            outputFromTest = IdWrapper(testId),
            assessmentTarget = IdWrapper(resourceId),
            wasGeneratedBy = IdWrapper(activityId)
        )

        val testExecution = TestExecutionActivity(
            id = activityId,
            associatedWith = IdWrapper(testId),
            generated = IdWrapper(resultId),
            used = IdWrapper(resourceId)
        )

        val resource = Entity(id = resourceId)

        return TestResultJsonLD(
            graph = listOf(testExecution, testResult, resource)
        )
    }

    fun buildTestResultSetJsonLD(
        evaluations: List<Evaluation>,
        benchmarkTitle: String,
        reportId: String
    ): TestResultSetJsonLD {
        val setId = "urn:dmpEvaluationService:$reportId"
        val assessmentTargetId = evaluations.firstOrNull()?.generated?.takeIf { it.isNotBlank() }
            ?: "https://example.org/resource/default"

        val individualResults = evaluations.map { buildEvalutionResultJsonLD(it) }
        val memberIds = evaluations.map { IdWrapper("urn:dmpEvaluationService:${it.evaluationId}") }

        val setGraph = TestResultSetGraph(
            id = setId,
            identifier = IdWrapper(setId),
            title = LangLiteral(value = "Assessment results for: $benchmarkTitle"),
            assessmentTarget = IdWrapper(assessmentTargetId),
            hadMember = memberIds,
            generatedAtTime = TypedLiteral("xsd:dateTime", Instant.now().toString()),
            license = IdWrapper("https://creativecommons.org/publicdomain/zero/1.0/")
        )

        return TestResultSetJsonLD(graph = listOf(setGraph) + individualResults.flatMap { it.graph })
    }
}
