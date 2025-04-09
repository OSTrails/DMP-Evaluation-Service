package io.github.ostrails.dmpevaluatorservice.evaluators.externalevaluator

import io.github.ostrails.dmpevaluatorservice.database.model.Evaluation
import io.github.ostrails.dmpevaluatorservice.database.model.EvaluationReport
import io.github.ostrails.dmpevaluatorservice.model.PluginInfo
import io.github.ostrails.dmpevaluatorservice.plugin.EvaluatorPlugin
import org.springframework.stereotype.Component

@Component
class FAIRChampionEvaluator: EvaluatorPlugin {
    override fun evaluate(
        maDMP: Map<String, Any>,
        config: Map<String, Any>,
        tests: List<String>,
        report: EvaluationReport
    ): List<Evaluation> {
        TODO("Not yet implemented")
    }

    override fun getPluginIdentifier(): String {
        return "External evaluator FAIR Champion"
    }

    override fun getPluginInformation(): PluginInfo {        return PluginInfo(
        pluginId = "Completeness",
        description = "Evaluator to perform completeness tests",
        benchmarks = listOf()
    )
    }

    override fun supports(dimension: String): Boolean = dimension == "FAIR"

}