package io.github.ostrails.dmpevaluatorservice.evaluators.feasibilityEvaluator

import io.github.ostrails.dmpevaluatorservice.database.model.Evaluation
import io.github.ostrails.dmpevaluatorservice.database.model.EvaluationReport
import io.github.ostrails.dmpevaluatorservice.model.PluginInfo
import io.github.ostrails.dmpevaluatorservice.model.ResultTestEnum
import io.github.ostrails.dmpevaluatorservice.plugin.EvaluatorPlugin
import org.springframework.stereotype.Component
import java.util.*

@Component
class FeasibilityEvaluator: EvaluatorPlugin {

    override fun supports(t: String): Boolean = t == getPluginIdentifier()

    override fun getPluginIdentifier(): String {
        return "FeasibilityEvaluator"
    }

    override fun getPluginInformation(): PluginInfo {
        return PluginInfo(
            pluginId = getPluginIdentifier(),
            description = "Evaluator to perform Feasibility tests",
            tests = listOf()
        )
    }

    override val functionMap = mapOf(
        "evaluateCoherentLicense" to  ::evaluateCoherentLicense
    )


    override fun evaluate(maDMP: Map<String, Any>, config: Map<String, Any>, tests: List<String>, report: EvaluationReport): List<Evaluation> {
        val evaluationsResults = tests.map { test ->
            Evaluation(
                evaluationId = UUID.randomUUID().toString(),
                result = ResultTestEnum.PASS,
                title = "Testing ",
                details = "Auto-generated evaluation of the test" + test ,
                reportId = report.reportId,
                generated = "${this::class.qualifiedName}:: evaluate",

            )
        }
        return evaluationsResults
    }

    fun evaluateCoherentLicense(
        maDMP: Any,
        reportId: String,
        testId: String
    ): Evaluation {
        return Evaluation(
            evaluationId = UUID.randomUUID().toString(),
            result = ResultTestEnum.PASS,
            details = "Auto-generated evaluation of the test",
            title = "Testing ",
            reportId = reportId,
            generated = "${this::class.qualifiedName}:: evaluateCoherentLicense",
            outputFromTest = testId
        )
    }

}