package io.github.ostrails.dmpevaluatorservice.evaluators.externalEvaluator

import io.github.ostrails.dmpevaluatorservice.database.model.Evaluation
import io.github.ostrails.dmpevaluatorservice.database.model.EvaluationReport
import io.github.ostrails.dmpevaluatorservice.model.PluginInfo
import io.github.ostrails.dmpevaluatorservice.plugin.EvaluatorPlugin
import org.springframework.stereotype.Component
import java.util.*

@Component
class FAIRChampionEvaluator: EvaluatorPlugin {

    override fun supports(t: String): Boolean = t == getPluginIdentifier()

    override val functionMap = mapOf(
        "evaluateStructure" to ::evaluateStructure,
        "evaluateMetadata" to ::evaluateMetadata
    )

    override fun evaluate(
        maDMP: Map<String, Any>,
        config: Map<String, Any>,
        tests: List<String>,
        report: EvaluationReport
    ): List<Evaluation> {
        TODO("Not yet implemented")
    }

    override fun getPluginIdentifier(): String {
        return "FAIR_Champion"
    }

    override fun getPluginInformation(): PluginInfo {        return PluginInfo(
        pluginId = getPluginIdentifier(),
        description = "Evaluator to perform completeness tests",
        tests = listOf()
    )
    }



    fun evaluateStructure(
        maDMP: Any
    ): Evaluation {
        return Evaluation(
            evaluationId = UUID.randomUUID().toString(),
            result = (1..10).random(),
            details = "Auto-generated evaluation of the test",
            reportId = "Testeds",

            //reportId = report.reportId
        )
    }

    fun evaluateMetadata(
        maDMP: Any
    ): Evaluation {
        return Evaluation(
            evaluationId = UUID.randomUUID().toString(),
            result = (1..10).random(),
            details = "Auto-generated evaluation of the test",
            reportId = "tested"
        )
    }

}