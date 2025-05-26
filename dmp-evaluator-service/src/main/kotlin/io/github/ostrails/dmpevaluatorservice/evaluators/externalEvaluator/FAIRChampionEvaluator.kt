package io.github.ostrails.dmpevaluatorservice.evaluators.externalEvaluator

import io.github.ostrails.dmpevaluatorservice.database.model.Evaluation
import io.github.ostrails.dmpevaluatorservice.database.model.EvaluationReport
import io.github.ostrails.dmpevaluatorservice.database.model.TestRecord
import io.github.ostrails.dmpevaluatorservice.model.PluginInfo
import io.github.ostrails.dmpevaluatorservice.model.ResultTestEnum
import io.github.ostrails.dmpevaluatorservice.plugin.EvaluatorPlugin
import io.github.ostrails.dmpevaluatorservice.service.externalConections.FairChampionService
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.springframework.stereotype.Component
import java.util.*

@Component
class FAIRChampionEvaluato(
    private val fairChampionService: FairChampionService,
): EvaluatorPlugin {

    override fun supports(t: String): Boolean = t == getPluginIdentifier()

    override val functionMap = mapOf(
        "evaluateStructure" to ::evaluateStructure,
        "evaluateMetadata" to ::evaluateMetadata,
        "evaluateLicense" to ::evaluateLicense,
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

    override fun getPluginInformation(): PluginInfo {
        return PluginInfo(
            pluginId = getPluginIdentifier(),
            description = "Evaluator to perform completeness tests",
            tests = listOf()
        )
    }



    fun evaluateStructure(
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
            generated = "${this::class.qualifiedName}:: evaluateStructure",
            outputFromTest = testRecord.id

        )
    }

    fun evaluateMetadata(
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
            generated = "${this::class.qualifiedName}:: evaluateMetadata",
            outputFromTest = testRecord.id
        )
    }

    fun evaluateLicense(
        maDMP: JsonObject,
        reportId: String,
        testRecord: TestRecord
    ): Evaluation {
        var resulTest: ResultTestEnum
        var logForTest: String = ""
        val datasets = extractDatasetIds(maDMP)
        if (datasets == null) {
            resulTest = ResultTestEnum.FAIL
        }else {
            val fairresult = checkFair(datasets)

        }
        return Evaluation(
            evaluationId = UUID.randomUUID().toString(),
            result = ResultTestEnum.PASS,
            details = testRecord.description,
            title = testRecord.title,
            reportId = reportId,
            generated = "${this::class.qualifiedName}:: evaluateLicense",
            outputFromTest = testRecord.id
        )
    }

    fun checkFair(datasets: List<String>): Map<String, Map<String, Any?>> {
        return datasets.associateWith { dataset ->
            val jsonresult = runBlocking { fairChampionService.assessTest("fc_metadata_includes_license",dataset) }
            val isSuccess = jsonresult["success"]?.jsonPrimitive?.boolean
            if(isSuccess == true){
                val data = jsonresult["data"]?.jsonObjectOrNull()
                mapOf(
                    "data" to data,
                    "log" to "Successfully fetched unpaywall data doi $dataset - "
                )
            } else {
                val errorMessage = jsonresult["error"]?.jsonPrimitive?.contentOrNull ?: "Unknown error"
                val status = jsonresult["status"]?.jsonPrimitive?.intOrNull
                mapOf(
                    "log" to "Failed to fetch data with doi $dataset (status=$status): $errorMessage"
                )
            }
        }
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

    fun JsonElement.jsonArrayOrNull(): JsonArray? =
        this as? JsonArray

    fun JsonElement.jsonObjectOrNull(): JsonObject? =
        this as? JsonObject

}