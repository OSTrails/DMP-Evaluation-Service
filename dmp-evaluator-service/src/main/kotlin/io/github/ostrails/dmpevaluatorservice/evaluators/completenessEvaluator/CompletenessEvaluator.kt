package io.github.ostrails.dmpevaluatorservice.evaluators.completenessEvaluator

import io.github.ostrails.dmpevaluatorservice.database.model.Evaluation
import io.github.ostrails.dmpevaluatorservice.database.model.EvaluationReport
import io.github.ostrails.dmpevaluatorservice.model.Benchmark
import io.github.ostrails.dmpevaluatorservice.model.PluginInfo
import io.github.ostrails.dmpevaluatorservice.plugin.EvaluatorPlugin
import org.springframework.stereotype.Component
import java.util.*


@Component
class CompletenessEvaluator: EvaluatorPlugin {

    override fun supports(dimension: String): Boolean = dimension == "completeness"

    override fun evaluate(maDMP: Map<String, Any>, config: Map<String, Any>, tests: List<String>, report: EvaluationReport): List<Evaluation> {
        val evaluationsResults = tests.map { test ->
            Evaluation(
                    evaluationId = UUID.randomUUID().toString(),
                    completenessScore = (1..10).random(),
                    details = "Auto-generated evaluation of the test" + test ,
                    reportId = report.reportId
            )

        }
        return evaluationsResults
    }

    override fun getPluginIdentifier(): String {
        return "EvaluatorTest "
    }

    override fun getPluginInformation(): PluginInfo {
        val becnhmark1 = Benchmark(
            id = "Benchmark ID",
            title = "Test Benchmark for sturcture code",
            description = "This benchmark is only to fill the data object ",
            version = "0.0.1"
        )
        return PluginInfo(
            pluginId = "Completeness",
            description = "Evaluator to perform completeness tests",
            benchmarks = listOf(becnhmark1)
        )
    }
}