package io.github.ostrails.dmpevaluatorservice.evaluators

import io.github.ostrails.dmpevaluatorservice.database.model.Evaluation
import io.github.ostrails.dmpevaluatorservice.database.model.EvaluationReport
import io.github.ostrails.dmpevaluatorservice.database.model.TestRecord
import io.github.ostrails.dmpevaluatorservice.model.PluginInfo
import io.github.ostrails.dmpevaluatorservice.model.ResultTestEnum
import io.github.ostrails.dmpevaluatorservice.plugin.EvaluatorPlugin
import io.github.ostrails.dmpevaluatorservice.service.externalConections.UnpaywallService
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.springframework.stereotype.Component
import java.util.*

@Component
class QualityOfActionsEvaluator(
    private val unpaywallService: UnpaywallService,
): EvaluatorPlugin {


    override fun supports(t: String): Boolean = t == getPluginIdentifier()

    override fun getPluginIdentifier(): String {
        return "QualityOfActionsEvaluator"
    }

    override val functionMap = mapOf(
        "evaluateOpenAccess" to ::evaluateOpenAccess,
    )

    override fun getPluginInformation(): PluginInfo {
        return PluginInfo(
            pluginId = getPluginIdentifier(),
            description = "Evaluator to perform completeness tests",
            tests = listOf()
        )
    }

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

    fun evaluateOpenAccess(
        maDMP: JsonObject,
        reportId: String,
        testRecord: TestRecord
    ): Evaluation {
        var resulTest: ResultTestEnum
        var logForTest: String = ""
        val datasets = extractDatasetIds(maDMP)
        if (datasets == null) {
            resulTest = ResultTestEnum.FAIL
            logForTest += "There is no dataset in the DMP"
        }else {
            val isOpenAccess = datasets.let { openAccess(datasets = it) }
            resulTest = ResultTestEnum.PASS
            for (dataset in datasets) {
                val entry = isOpenAccess?.get(dataset)
                val isOa = entry?.get("is_oa") as? Boolean
                val log = entry?.get("log") as? String
                if (isOa != true) {
                    resulTest = ResultTestEnum.FAIL
                }
                logForTest += log + "the resource is open access based in the unpaywall service - "
            }

        }
        return Evaluation(
            evaluationId = UUID.randomUUID().toString(),
            result = resulTest,
            details = testRecord.description,
            title = testRecord.title,
            reportId = reportId,
            generated = "${this::class.qualifiedName}:: evaluateOpenAccess",
            outputFromTest = testRecord.id,
            log = logForTest,
            affectedElements = "datasets ${datasets}",
        )
    }

    fun extractDatasetIds(maDMP: JsonObject): List<String>? {
        val datasetArray = maDMP["dmp"]
            ?.jsonObject?.get("dataset")
            ?.jsonArrayOrNull()
        println(datasetArray)
        if (datasetArray != null) {
            return  datasetArray.mapNotNull { datasetElement ->
                val dataset = datasetElement.jsonObject
                val datasetIdObj = dataset["dataset_id"]?.jsonObjectOrNull()
                val identifier = datasetIdObj?.get("identifier")
                println("dataset: $dataset")
                return@mapNotNull identifier?.jsonPrimitive?.contentOrNull
            }
        }else return null
    }


    fun openAccess(datasets: List<String>): Map<String, Map<String, Any?>> {
        return datasets.associateWith { dataset ->
            val resultJson = runBlocking { unpaywallService.checkOpenAccess(dataset)}
            val isSuccess = resultJson["success"]?.jsonPrimitive?.boolean
            if(isSuccess == true){
                val data = resultJson["data"]?.jsonObjectOrNull()
                val isOa = data?.get("is_oa")?.jsonPrimitive?.boolean
                mapOf(
                    "is_oa" to isOa,
                    "log" to "Successfully fetched unpaywall data doi $dataset - "
                )
            } else {
                val errorMessage = resultJson["error"]?.jsonPrimitive?.contentOrNull ?: "Unknown error"
                val status = resultJson["status"]?.jsonPrimitive?.intOrNull
                mapOf(
                    "is_oa" to null,
                    "log" to "Failed to fetch data with doi $dataset (status=$status): $errorMessage"
                )
            }
        }
    }

    fun JsonElement.jsonArrayOrNull(): JsonArray? =
        this as? JsonArray

    fun JsonElement.jsonObjectOrNull(): JsonObject? =
        this as? JsonObject


}