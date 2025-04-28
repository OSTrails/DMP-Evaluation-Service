package io.github.ostrails.dmpevaluatorservice.plugin

import io.github.ostrails.dmpevaluatorservice.database.model.Evaluation
import io.github.ostrails.dmpevaluatorservice.database.model.EvaluationReport
import io.github.ostrails.dmpevaluatorservice.model.PluginInfo

interface EvaluatorPlugin: ConfigurablePlugin<String, PluginInfo> {
    fun evaluate(
        maDMP: Map<String, Any>,
        config: Map<String, Any>,
        tests: List<String>,
        report: EvaluationReport
    ): List<Evaluation>

    override fun supports(t: String): Boolean = t == getPluginIdentifier()
    val functionMap: Map<String, (Any) -> Evaluation>
    //val functionMap: Map<String, (Map<String, Any>, Map<String, Any>, EvaluationReport) -> Evaluation>

}