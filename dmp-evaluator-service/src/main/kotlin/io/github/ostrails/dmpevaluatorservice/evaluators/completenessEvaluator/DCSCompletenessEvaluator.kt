package io.github.ostrails.dmpevaluatorservice.evaluators.completenessEvaluator

import io.github.ostrails.dmpevaluatorservice.database.model.Evaluation
import io.github.ostrails.dmpevaluatorservice.database.model.EvaluationReport
import io.github.ostrails.dmpevaluatorservice.database.model.TestRecord
import io.github.ostrails.dmpevaluatorservice.model.PluginInfo
import io.github.ostrails.dmpevaluatorservice.model.ResultTestEnum
import io.github.ostrails.dmpevaluatorservice.plugin.EvaluatorPlugin
import io.github.ostrails.dmpevaluatorservice.utils.extractValuesByPath
import kotlinx.serialization.json.*
import org.everit.json.schema.Schema
import org.everit.json.schema.ValidationException
import org.everit.json.schema.loader.SchemaLoader
import org.json.JSONObject
import org.json.JSONTokener
import org.springframework.stereotype.Component
import java.io.InputStream
import java.util.*

@Component
class DCSCompletenessEvaluator: EvaluatorPlugin {

    override fun supports(t: String): Boolean = t == getPluginIdentifier()

    override fun getPluginIdentifier(): String {
        return "DCSCompletenessEvaluator"
    }

    override val functionMap = mapOf(
        "evaluateStructure" to ::evaluateStructure,
        "evaluateFormats" to :: evaluateFormats,
        "costEntityPresent" to :: costEntityPresent,
        "costEntityValuesPresent" to :: costEntityValuesPresent,
        "contributorValuesPresent" to :: contributorValuesPresent,
        "datasetEntityValuesPresent" to :: datasetEntityValuesPresent,
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

    fun evaluateStructure(
        maDMP: JsonObject,
        reportId: String,
        testRecord: TestRecord
    ): Evaluation {
        val validationDMP = Validator.validateRequiredValues(maDMP.toString())
        return Evaluation(
            evaluationId = UUID.randomUUID().toString(),
            result = if (validationDMP.isEmpty()) ResultTestEnum.PASS else ResultTestEnum.FAIL,
            details = testRecord.description,
            title = testRecord.title,
            reportId = reportId,
            log = formattedLog(validationDMP, "All required fields are present.", "Missing required fields detected"),
            generated = "${this::class.qualifiedName}:: evaluateStructure",
            outputFromTest = testRecord.id,
            completion = 100
        )
    }

    fun evaluateFormats(
        maDMP: JsonObject,
        reportId: String,
        testRecord: TestRecord
    ): Evaluation {
        val validationDMP = Validator.validateFormatValues(maDMP.toString())
        return Evaluation(
            evaluationId = UUID.randomUUID().toString(),
            result = if (validationDMP.isEmpty()) ResultTestEnum.PASS else ResultTestEnum.FAIL,
            details = testRecord.description,
            title = testRecord.title,
            affectedElements = "dpm",
            reportId = reportId,
            log = formattedLog(validationDMP, "All required fields are in format.", "Fields formats required detected"),
            generated = "${this::class.qualifiedName}:: evaluateFormats",
            outputFromTest = testRecord.id,
            completion = 100
        )
    }

    fun costEntityPresent(
        maDMP: JsonObject,
        reportId: String,
        testRecord: TestRecord): Evaluation{
        val costs = extractValuesByPath<String>(maDMP, "dmp.cost[*]")
        println(costs.toString())
        return Evaluation(
            evaluationId = UUID.randomUUID().toString(),
            result =if (costs.isEmpty()) ResultTestEnum.FAIL else ResultTestEnum.PASS,
            details = testRecord.description,
            title = testRecord.title,
            affectedElements = "dpm.contributor",
            reportId = reportId,
            log = if (costs.isEmpty()) "Cost field are not present in the maDMP"  else "Cost fields are present in the maDMP",
            generated = "${this::class.qualifiedName}:: costEntityPresent",
            outputFromTest = testRecord.id,
            completion = 100
        )
    }

    fun costEntityValuesPresent(
        maDMP: JsonObject,
        reportId: String,
        testRecord: TestRecord): Evaluation{
        var resultValue: ResultTestEnum = ResultTestEnum.INDERTERMINATED
        val costs = extractValuesByPath<Any>(maDMP, "dmp.cost[*]")
        val logMessages = mutableListOf<String>()
        if (costs.isEmpty()) {
            resultValue = ResultTestEnum.FAIL
            logMessages.add("Cost field is not present in the maDMP")
        } else{resultValue = ResultTestEnum.INDERTERMINATED}

        val validCosts = costs.mapNotNull { element ->
            if (element is JsonObject) {
                val title = element["title"]?.jsonPrimitiveOrNull?.contentOrNull
                val value = element["value"]?.jsonPrimitiveOrNull?.contentOrNull
                val description = element["description"]?.jsonPrimitiveOrNull?.contentOrNull
                val currency = element["currency_code"]?.jsonPrimitiveOrNull?.contentOrNull

                logMessages.add("Cost entry - title: $title, description; $description, value: $value, currency: $currency")

                if (!title.isNullOrBlank() && !description.isNullOrBlank() && !value.isNullOrBlank() && !currency.isNullOrBlank()) {
                    title // Use title as a "valid presence" indicator
                    resultValue = ResultTestEnum.PASS
                    return@mapNotNull true
                } else {
                    resultValue = ResultTestEnum.FAIL
                    logMessages.add("The full data for the cost is required")
                    return@mapNotNull false
                }
            } else {
                logMessages.add("Invalid cost entry: not a JsonObject.")
                resultValue = ResultTestEnum.FAIL
                return@mapNotNull false
            }
        }

        return Evaluation(
            evaluationId = UUID.randomUUID().toString(),
            result = resultValue,
            affectedElements = "dpm.cost",
            details = testRecord.description,
            title = testRecord.title,
            reportId = reportId,
            log = logMessages.joinToString("\n"),
            generated = "${this::class.qualifiedName}:: costEntityPresent",
            outputFromTest = testRecord.id,
            completion = 100
        )
    }

    fun contributorValuesPresent(
        maDMP: JsonObject,
        reportId: String,
        testRecord: TestRecord): Evaluation{
        val logMessages = mutableListOf<String>()
        val contributors = extractValuesByPath<String>(maDMP, "dmp.contributor[*]")
        var resultValue: ResultTestEnum = ResultTestEnum.INDERTERMINATED
        logMessages.add("contributors: $contributors")
        if (contributors.isEmpty()) {
            resultValue = ResultTestEnum.FAIL
            logMessages.add("Contributor field are not present in the maDMP")
        }  else {
            resultValue = ResultTestEnum.PASS
            logMessages.add("Contributor fields are present in the maDMP")
        }

        return Evaluation(
            evaluationId = UUID.randomUUID().toString(),
            result =resultValue,
            details = testRecord.description,
            affectedElements = "dpm.contributor",
            title = testRecord.title,
            reportId = reportId,
            log = logMessages.joinToString("\n"),
            generated = "${this::class.qualifiedName}:: contributorValuesPresent",
            outputFromTest = testRecord.id,
            completion = 100
            )
    }

    fun datasetEntityValuesPresent(
        maDMP: JsonObject,
        reportId: String,
        testRecord: TestRecord): Evaluation{
        var resultValue: ResultTestEnum = ResultTestEnum.INDERTERMINATED
        val datasets = extractValuesByPath<Any>(maDMP, "dmp.dataset[*]")
        val logMessages = mutableListOf<String>()
        if (datasets.isEmpty()) {
            resultValue = ResultTestEnum.FAIL
            logMessages.add("Cost field is not present in the maDMP")
        } else{resultValue = ResultTestEnum.INDERTERMINATED}

        datasets.forEachIndexed { index, element ->
            if (element is JsonObject) {
                val id = element["dataset_id"]?.jsonObjectOrNull
                    ?.get("identifier")?.jsonPrimitiveOrNull?.contentOrNull
                val idType = element["dataset_id"]?.jsonObjectOrNull
                    ?.get("type")?.jsonPrimitiveOrNull?.contentOrNull
                val personalData = element["personal_data"]?.jsonPrimitiveOrNull?.contentOrNull
                val sensitiveData = element["sensitive_data"]?.jsonPrimitiveOrNull?.contentOrNull
                val title = element["title"]?.jsonPrimitiveOrNull?.contentOrNull
                val type = element["type"]?.jsonPrimitiveOrNull?.contentOrNull

                var hasValidDistribution = false
                val distributions = element["distribution"]?.jsonArrayOrNull()

                if (!distributions.isNullOrEmpty()) {
                    for (dist in distributions) {
                        val distObj = dist.jsonObjectOrNull
                        val distTitle = distObj?.get("title")?.jsonPrimitiveOrNull?.contentOrNull
                        val distLicense = distObj?.get("license")?.jsonArrayOrNull()


                        if (!distTitle.isNullOrBlank() && distLicense != null) {
                            logMessages.add("Dataset[$index] distribution title: $distTitle, license: $distLicense")
                            hasValidDistribution = true
                        } else {
                            logMessages.add("Dataset[$index] has an invalid distribution: missing title or license.")
                            resultValue = ResultTestEnum.FAIL
                        }
                    }
                } else {
                    logMessages.add("Dataset[$index] is missing distribution array.")
                    resultValue = ResultTestEnum.FAIL
                }

                logMessages.add(
                    "Dataset[$index] - id: $id, type: $idType, title: $title, personal_data: $personalData, sensitive_data: $sensitiveData"
                )

                if (id.isNullOrBlank() || idType.isNullOrBlank() || personalData.isNullOrBlank() ||
                    sensitiveData.isNullOrBlank() || title.isNullOrBlank() || type.isNullOrBlank() || !hasValidDistribution
                ) {
                    resultValue = ResultTestEnum.FAIL
                    logMessages.add("Dataset[$index] is missing one or more required fields.")
                } else if (resultValue != ResultTestEnum.FAIL) {
                    resultValue = ResultTestEnum.PASS
                }
            } else {
                resultValue = ResultTestEnum.FAIL
                logMessages.add("Invalid dataset entry at index $index: not a JsonObject.")
            }
        }
        return Evaluation(
            evaluationId = UUID.randomUUID().toString(),
            result = resultValue,
            affectedElements = "dpm.dataset",
            details = testRecord.description,
            title = testRecord.title,
            reportId = reportId,
            log = logMessages.joinToString("\n"),
            generated = "${this::class.qualifiedName}:: datasetEntityValuesPresent",
            outputFromTest = testRecord.id,
            completion = 100
        )
    }




    object Validator {
        private val schema: Schema by lazy {
            val inputStream: InputStream = javaClass.classLoader
                .getResourceAsStream("maDMPSchemas/maDMP-schema-1.2.json")
                ?: throw IllegalStateException("Schema file not found")
            val rawSchema = JSONObject(JSONTokener(inputStream))
            SchemaLoader.load(rawSchema)
        }

        fun validateRequiredValues(inputJson: String): List<String> {
            return try {
                val jsonObject = JSONObject(inputJson)
                schema.validate(jsonObject)
                emptyList()
            } catch (e: ValidationException) {
                e.allMessages.filter { msg ->
                    msg.contains("required key", ignoreCase = true)
                }
            }
        }

        fun validateFormatValues(inputJson: String): List<String> {
            return try {
                val jsonObject = JSONObject(inputJson)
                schema.validate(jsonObject)
                emptyList()
            } catch (e: ValidationException) {
                e.allMessages.filter { msg ->
                    !msg.contains("required key", ignoreCase = true) &&
                            (msg.contains("not a valid") ||
                                    msg.contains("expected type", ignoreCase = true))
                }
            }
        }
    }

    fun   formattedLog (validationDMP: List<String>, feedbackMessage: String, descriptionError: String): String {
        if (validationDMP.isNotEmpty()) {
            return buildString {
                appendLine(descriptionError + ": " )
                validationDMP.forEachIndexed { index, msg ->
                    appendLine("${index + 1}. $msg")
                }
            }
        } else {
            return feedbackMessage
        }
    }

    val JsonElement.jsonPrimitiveOrNull: JsonPrimitive?
        get() = this as? JsonPrimitive

    fun JsonElement.jsonArrayOrNull(): JsonArray? =
        this as? JsonArray

    val JsonElement.jsonObjectOrNull: JsonObject?
        get() = this as? JsonObject









}