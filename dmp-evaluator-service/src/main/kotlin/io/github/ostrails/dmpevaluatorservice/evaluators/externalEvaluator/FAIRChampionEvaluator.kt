package io.github.ostrails.dmpevaluatorservice.evaluators.externalEvaluator

import io.github.ostrails.dmpevaluatorservice.database.model.Evaluation
import io.github.ostrails.dmpevaluatorservice.database.model.EvaluationReport
import io.github.ostrails.dmpevaluatorservice.database.model.TestRecord
import io.github.ostrails.dmpevaluatorservice.model.PluginInfo
import io.github.ostrails.dmpevaluatorservice.model.ResultTestEnum
import io.github.ostrails.dmpevaluatorservice.plugin.ExternalBenchmarkPlugin
import io.github.ostrails.dmpevaluatorservice.service.externalConections.FairChampionService
import io.github.ostrails.dmpevaluatorservice.utils.parseFairChampionResponse
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.springframework.stereotype.Component
import java.util.*

@Component
class FAIRChampionEvaluator(
    private val fairChampionService: FairChampionService,
) : ExternalBenchmarkPlugin {

    override fun supports(t: String): Boolean = t == getPluginIdentifier()
    override fun getPluginIdentifier(): String = "FAIR_Champion"

    override fun getPluginInformation(): PluginInfo = PluginInfo(
        pluginId = getPluginIdentifier(),
        description = "Delegates FAIR benchmark evaluation to the external FAIR Champion service to evaluate FAIRness of datasets",
        functions = listOf()
    )

    override val functionMap: Map<String, (JsonObject, String, TestRecord) -> Evaluation> = emptyMap()

    override val benchmarkFunctionMap = mapOf(
        "evaluateFAIRnessDatasetByChampion" to ::evaluateFAIRnessDatasetByChampion,
    )

    override fun evaluate(
        maDMP: Map<String, Any>,
        config: Map<String, Any>,
        tests: List<String>,
        report: EvaluationReport,
    ): List<Evaluation> = TODO("Not yet implemented")

    fun evaluateFAIRnessDatasetByChampion(
        maDMP: JsonObject,
        reportId: String,
        testRecord: TestRecord,
    ): List<Evaluation> {
        val datasets = extractDatasetIds(maDMP)

        if (datasets.isNullOrEmpty()) {
            return listOf(Evaluation(
                evaluationId = UUID.randomUUID().toString(),
                result = ResultTestEnum.FAIL,
                title = testRecord.title,
                details = testRecord.description,
                reportId = reportId,
                log = "No dataset identifiers found in the maDMP.",
                wasGeneratedBy = "${this::class.qualifiedName}::evaluateFAIRnessDatasetByChampion",
                outputFromTest = testRecord.id,
                completion = 100
            ))
        }

        return datasets.flatMap { guid ->
            val response = runBlocking { fairChampionService.assessBenchmark(guid) }
            val success = response["success"]?.jsonPrimitive?.boolean

            if (success == true) {
                val data = response["data"]?.jsonObjectOrNull()
                    ?: return@flatMap listOf(errorEvaluation(guid, "Empty response body", reportId, testRecord))
                parseFairChampionResponse(data, reportId, testRecord)
            } else {
                val errorMsg = response["error"]?.jsonPrimitive?.contentOrNull
                    ?: "HTTP ${response["status"]?.jsonPrimitive?.intOrNull ?: "unknown"}"
                listOf(errorEvaluation(guid, errorMsg, reportId, testRecord))
            }
        }
    }

    private fun errorEvaluation(guid: String, reason: String, reportId: String, testRecord: TestRecord) =
        Evaluation(
            evaluationId = UUID.randomUUID().toString(),
            result = ResultTestEnum.ERROR,
            title = testRecord.title,
            details = testRecord.description,
            reportId = reportId,
            log = "FAIR Champion assessment failed for '$guid': $reason",
            wasGeneratedBy = "${this::class.qualifiedName}::evaluateFAIRnessDatasetByChampion",
            outputFromTest = testRecord.id,
            completion = 100
        )

    private fun extractDatasetIds(maDMP: JsonObject): List<String>? =
        maDMP["dmp"]?.jsonObject?.get("dataset")?.jsonArrayOrNull()
            ?.mapNotNull { element ->
                element.jsonObject["dataset_id"]?.jsonObjectOrNull()
                    ?.get("identifier")?.jsonPrimitive?.contentOrNull
            }

    private fun JsonElement.jsonArrayOrNull(): JsonArray? = this as? JsonArray
    private fun JsonElement.jsonObjectOrNull(): JsonObject? = this as? JsonObject
}
