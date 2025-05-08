package io.github.ostrails.dmpevaluatorservice.evaluators.completenessEvaluator

import io.github.ostrails.dmpevaluatorservice.database.model.Evaluation
import io.github.ostrails.dmpevaluatorservice.database.model.EvaluationReport
import io.github.ostrails.dmpevaluatorservice.model.PluginInfo
import io.github.ostrails.dmpevaluatorservice.model.ResultTestEnum
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
                    result = ResultTestEnum.PASS,
                    title = "Testing ",
                    details = "Auto-generated evaluation of the test" + test ,
                    reportId = report.reportId,
                    generated = "${this::class.qualifiedName}:: evaluate"
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
        maDMP: Any,
        reportId: String
    ): Evaluation {
        return Evaluation(
            evaluationId = UUID.randomUUID().toString(),
            result = ResultTestEnum.PASS,
            details = "Auto-generated evaluation of the test",
            title = "Testing ",
            reportId = reportId,
            generated = "${this::class.qualifiedName}:: evaluateStructure"
        )
    }

    fun evaluateMetadata(
        maDMP: Any,
        reportId: String,
    ): Evaluation {
        return Evaluation(
            evaluationId = UUID.randomUUID().toString(),
            result = ResultTestEnum.FAIL,
            details = "Auto-generated evaluation of the test",
            reportId = reportId,
            title = "Testing ",
            generated = "${this::class.qualifiedName}:: evaluateMetadata"
        )
    }
}