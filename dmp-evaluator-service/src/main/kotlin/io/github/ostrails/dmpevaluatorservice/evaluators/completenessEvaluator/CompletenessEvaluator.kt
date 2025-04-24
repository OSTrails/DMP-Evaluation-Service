package io.github.ostrails.dmpevaluatorservice.evaluators.completenessEvaluator

import io.github.ostrails.dmpevaluatorservice.database.model.Evaluation
import io.github.ostrails.dmpevaluatorservice.database.model.EvaluationReport
import io.github.ostrails.dmpevaluatorservice.model.PluginInfo
import io.github.ostrails.dmpevaluatorservice.plugin.EvaluatorPlugin
import org.springframework.stereotype.Component
import java.util.*


@Component
class CompletenessEvaluator: EvaluatorPlugin {

    override fun supports(t: String): Boolean = t == getPluginIdentifier()

    override val functionMap = mapOf(
        "evaluateStructure" to ::evaluateStructure,
        "evaluateMetadata" to ::evaluateMetadata
    )

    override fun evaluate(maDMP: Map<String, Any>, config: Map<String, Any>, tests: List<String>, report: EvaluationReport): List<Evaluation> {
        val evaluationsResults = tests.map { test ->
            Evaluation(
                    evaluationId = UUID.randomUUID().toString(),
                    result = (1..10).random(),
                    details = "Auto-generated evaluation of the test" + test ,
                    reportId = report.reportId
            )
        }
        return evaluationsResults
    }

    override fun getPluginIdentifier(): String {
        return "CompletenessEvaluator"
    }

    override fun getPluginInformation(): PluginInfo {
        return PluginInfo(
            pluginId = getPluginIdentifier(),
            description = "Evaluator to perform completeness tests",
            tests = listOf()
        )
    }

    fun evaluateStructure(
        maDMP: Map<String, Any>,
        config: Map<String, Any>,
        report: EvaluationReport
    ): Evaluation {
        return Evaluation(
            evaluationId = UUID.randomUUID().toString(),
            result = (1..10).random(),
            details = "Auto-generated evaluation of the test",
            reportId = report.reportId
        )
    }

    fun evaluateMetadata(
        maDMP: Map<String, Any>,
        config: Map<String, Any>,
        report: EvaluationReport
    ): Evaluation {
        return Evaluation(
            evaluationId = UUID.randomUUID().toString(),
            result = (1..10).random(),
            details = "Auto-generated evaluation of the test",
            reportId = report.reportId
        )
    }
}