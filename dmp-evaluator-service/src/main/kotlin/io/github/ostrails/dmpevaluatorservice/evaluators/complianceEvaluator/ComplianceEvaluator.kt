package io.github.ostrails.dmpevaluatorservice.evaluators.complianceEvaluator

import io.github.ostrails.dmpevaluatorservice.database.model.Evaluation
import io.github.ostrails.dmpevaluatorservice.database.model.EvaluationReport
import io.github.ostrails.dmpevaluatorservice.database.model.TestRecord
import io.github.ostrails.dmpevaluatorservice.model.PluginInfo
import io.github.ostrails.dmpevaluatorservice.model.ResultTestEnum
import io.github.ostrails.dmpevaluatorservice.plugin.EvaluatorPlugin
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import org.springframework.stereotype.Component
import java.util.*

@Component
class ComplianceEvaluator: EvaluatorPlugin {

    override fun supports(t: String): Boolean = t == getPluginIdentifier()

    override fun getPluginIdentifier(): String {
        return "ComplianceEvaluator"
    }

    override fun getPluginInformation(): PluginInfo {
        return PluginInfo(
            pluginId = getPluginIdentifier(),
            description = "Evaluator to perform Feasibility tests",
            tests = listOf()
        )
    }


    override val functionMap = mapOf(
        "evaluateCoherentLicense" to ::evaluateLicenseCompliance,
        "checkFormatFile" to :: checkFormatFile

    )

    override fun evaluate(maDMP: Map<String, Any>, config: Map<String, Any>, tests: List<String>, report: EvaluationReport): List<Evaluation> {
        val evaluationsResults = tests.map { test ->
            Evaluation(
                evaluationId = UUID.randomUUID().toString(),
                result = ResultTestEnum.PASS,
                title = "Testing ",
                details = "Auto-generated evaluation of the test" + test ,
                reportId = report.reportId
            )
        }
        return evaluationsResults
    }

    fun evaluateLicenseCompliance(
        maDMP: Any,
        reportId: String,
        testRecord: TestRecord
    ): Evaluation {
        return Evaluation(
            evaluationId = UUID.randomUUID().toString(),
            result = ResultTestEnum.PASS,
            details = testRecord.description,
            title = testRecord.title,
            reportId = reportId,
            generated = "${this::class.qualifiedName}:: evaluateLicenseCompliance",
            outputFromTest = testRecord.id
        )
    }

    fun checkFormatFile(
    maDMP: Any,
    reportId: String,
    testRecord: TestRecord
    ): Evaluation {
        val json = maDMP as? JsonObject
            ?: return Evaluation(
                evaluationId = UUID.randomUUID().toString(),
                result = ResultTestEnum.INDERTERMINATED,
                title = testRecord.title,
                details = "Input is not a valid JsonObject",
                reportId = reportId,
                generated = "${this::class.qualifiedName}::checkFormatFile",
                outputFromTest = testRecord.id,
                log = "The provided maDMP could not be parsed as a JsonObject."
            )
        val extension = json["fileExtension"]?.jsonPrimitive?.contentOrNull?.lowercase()

        val result = if (extension == "json") ResultTestEnum.PASS else ResultTestEnum.FAIL
        val logMessage = if (extension != "json") {
            "File extension is '$extension'. Expected: 'json'."
        } else {
            "File extension is a valid json."
        }
        return Evaluation(
            evaluationId = UUID.randomUUID().toString(),
            result = result,
            title = testRecord.title,
            details = testRecord.description,
            reportId = reportId,
            generated = "${this::class.qualifiedName}::checkFormatFile",
            outputFromTest = testRecord.id,
            log = logMessage
        )
    }

}