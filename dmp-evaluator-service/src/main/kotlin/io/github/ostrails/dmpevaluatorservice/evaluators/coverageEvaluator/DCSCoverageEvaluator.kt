package io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator

import io.github.ostrails.dmpevaluatorservice.database.model.Evaluation
import io.github.ostrails.dmpevaluatorservice.database.model.EvaluationReport
import io.github.ostrails.dmpevaluatorservice.database.model.TestRecord
import io.github.ostrails.dmpevaluatorservice.model.PluginInfo
import io.github.ostrails.dmpevaluatorservice.plugin.EvaluatorPlugin
import kotlinx.serialization.json.JsonObject
import org.springframework.stereotype.Component

// Coverage checks for the OSTrails pilot metrics: each test verifies that a DMP Common Standard (DCS 1.2/1.3)
// field is declared for every relevant dataset, distribution or contributor.
@Component
class DCSCoverageEvaluator : EvaluatorPlugin {

    override fun supports(t: String): Boolean = t == getPluginIdentifier()

    override fun getPluginIdentifier(): String = "DCSCoverageEvaluator"

    override fun getPluginInformation(): PluginInfo = PluginInfo(
        pluginId = getPluginIdentifier(),
        description = "Evaluator to perform DMP Common Standard coverage tests for the OSTrails pilot metrics",
        functions = listOf()
    )

    override val functionMap: Map<String, (JsonObject, String, TestRecord) -> Evaluation> = mapOf(
    )

    override fun evaluate(maDMP: Map<String, Any>, config: Map<String, Any>, tests: List<String>, report: EvaluationReport): List<Evaluation> =
        emptyList()

    private fun generatedBy(function: String) = "${this::class.qualifiedName}::$function"
}
