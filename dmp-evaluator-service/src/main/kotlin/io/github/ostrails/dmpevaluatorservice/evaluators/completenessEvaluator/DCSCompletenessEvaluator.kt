package io.github.ostrails.dmpevaluatorservice.evaluators.completenessEvaluator

import io.github.ostrails.dmpevaluatorservice.database.model.Evaluation
import io.github.ostrails.dmpevaluatorservice.database.model.EvaluationReport
import io.github.ostrails.dmpevaluatorservice.database.model.Guidance
import io.github.ostrails.dmpevaluatorservice.database.model.GuidanceEntry
import io.github.ostrails.dmpevaluatorservice.database.model.TestRecord
import io.github.ostrails.dmpevaluatorservice.model.PluginInfo
import io.github.ostrails.dmpevaluatorservice.model.ResultTestEnum
import io.github.ostrails.dmpevaluatorservice.plugin.EvaluatorPlugin
import io.github.ostrails.dmpevaluatorservice.utils.buildDatasetLabel
import io.github.ostrails.dmpevaluatorservice.utils.extractAssessmentTarget
import io.github.ostrails.dmpevaluatorservice.utils.extractValuesByPath
import kotlinx.serialization.json.*
import org.everit.json.schema.Schema
import org.everit.json.schema.ValidationException
import org.everit.json.schema.loader.SchemaLoader
import org.json.JSONObject
import org.json.JSONTokener
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.InputStream
import java.util.*

@Component
class DCSCompletenessEvaluator: EvaluatorPlugin {

    private val log: Logger = LoggerFactory.getLogger(DCSCompletenessEvaluator::class.java)

    override fun supports(t: String): Boolean = t == getPluginIdentifier()

    override fun getPluginIdentifier(): String {
        return "DCSCompletenessEvaluator"
    }

    override val functionMap = mapOf(
        "evaluateStructure" to ::evaluateStructure,
        "evaluateFormats" to ::evaluateFormats,
        "costEntityPresent" to ::costEntityPresent,
        "costEntityValuesPresent" to ::costEntityValuesPresent,
        "contributorValuesPresent" to ::contributorValuesPresent,
        "datasetEntityValuesPresent" to ::datasetEntityValuesPresent,
        "datasetPersistentIdentifierPresent" to ::datasetPersistentIdentifierPresent,
    )

    override fun getPluginInformation(): PluginInfo {
        return PluginInfo(
            pluginId = getPluginIdentifier(),
            description = "Evaluator to perform DCScompleteness tests",
            functions = listOf()
        )
    }

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
            affectedElements = listOf("dpm"),
            reportId = reportId,
            log = formattedLog(validationDMP, "All required fields are in format.", "Fields formats required detected"),
            outputFromTest = testRecord.id,
            completion = 100
        )
    }

    fun costEntityPresent(
        maDMP: JsonObject,
        reportId: String,
        testRecord: TestRecord): Evaluation{
        val costs = extractValuesByPath<String>(maDMP, "dmp.cost[*]")
        log.debug("costs: $costs")
        return Evaluation(
            evaluationId = UUID.randomUUID().toString(),
            result =if (costs.isEmpty()) ResultTestEnum.FAIL else ResultTestEnum.PASS,
            details = testRecord.description,
            title = testRecord.title,
            affectedElements = listOf("dpm.contributor"),
            reportId = reportId,
            log = if (costs.isEmpty()) "Cost field are not present in the maDMP"  else "Cost fields are present in the maDMP",
            outputFromTest = testRecord.id,
            completion = 100
        )
    }

    fun costEntityValuesPresent(
        maDMP: JsonObject,
        reportId: String,
        testRecord: TestRecord): Evaluation{
        val costs = extractValuesByPath<Any>(maDMP, "dmp.cost[*]")
        val logMessages = mutableListOf<String>()
        if (costs.isEmpty()) {
            logMessages.add("Cost field is not present in the maDMP")
        }

        // Evaluate every entry (map, not all) so each one is logged; any incomplete entry fails the test
        val costEntriesComplete = costs.map { element ->
            if (element is JsonObject) {
                val title = element["title"]?.jsonPrimitiveOrNull?.contentOrNull
                val value = element["value"]?.jsonPrimitiveOrNull?.contentOrNull
                val description = element["description"]?.jsonPrimitiveOrNull?.contentOrNull
                val currency = element["currency_code"]?.jsonPrimitiveOrNull?.contentOrNull

                logMessages.add("Cost entry - title: $title, description; $description, value: $value, currency: $currency")

                if (!title.isNullOrBlank() && !description.isNullOrBlank() && !value.isNullOrBlank() && !currency.isNullOrBlank()) {
                    true
                } else {
                    logMessages.add("The full data for the cost is required")
                    false
                }
            } else {
                logMessages.add("Invalid cost entry: not a JsonObject.")
                false
            }
        }
        val resultValue = if (costs.isNotEmpty() && costEntriesComplete.all { it }) ResultTestEnum.PASS else ResultTestEnum.FAIL

        return Evaluation(
            evaluationId = UUID.randomUUID().toString(),
            result = resultValue,
            affectedElements = listOf("dpm.cost"),
            details = testRecord.description,
            title = testRecord.title,
            reportId = reportId,
            log = logMessages.joinToString("\n"),
            assessmentTarget = extractAssessmentTarget(maDMP),
            wasGeneratedBy = "${this::class.qualifiedName}::costEntityValuesPresent",
            outputFromTest = testRecord.id,
            completion = 100
        )
    }

    fun contributorValuesPresent(
        maDMP: JsonObject,
        reportId: String,
        testRecord: TestRecord): Evaluation{
        val logMessages = mutableListOf<String>()
        val affectedContributors = mutableListOf<String>()
        val guidanceIssues = mutableListOf<GuidanceEntry>()
        val contributors = extractValuesByPath<Any>(maDMP, "dmp.contributor[*]")

        if (contributors.isEmpty()) {
            logMessages.add("Contributor field is not present in the maDMP.")
        }

        contributors.forEachIndexed { index, element ->
            val contributor = element as? JsonObject
            val name = contributor?.get("name")?.jsonPrimitiveOrNull?.contentOrNull
            val label = if (!name.isNullOrBlank()) name else "Unnamed contributor (position $index)"
            val roleElement = contributor?.get("role")
            val roles = roleElement?.jsonArrayOrNull()

            val problem = when {
                contributor == null -> "Contributor entry is not a JSON object."
                roleElement == null -> "No role declared. Add at least one role (e.g. 'Data Steward')."
                roles == null -> "'role' must be a list of role names."
                roles.isEmpty() -> "The role list is empty. Add at least one role (e.g. 'Data Steward')."
                roles.any { it.jsonPrimitiveOrNull?.contentOrNull.isNullOrBlank() } -> "The role list contains a blank or non-text entry."
                else -> null
            }

            if (problem == null) {
                logMessages.add("Contributor[$index] '$label': roles ${roles?.map { it.jsonPrimitive.content }}.")
            } else {
                logMessages.add("Contributor[$index] '$label': $problem")
                affectedContributors.add("contributor[$index]")
                guidanceIssues.add(GuidanceEntry(dataset = label, reason = problem))
            }
        }

        val resultValue = if (contributors.isNotEmpty() && guidanceIssues.isEmpty()) ResultTestEnum.PASS else ResultTestEnum.FAIL

        val guidance = when {
            contributors.isEmpty() -> Guidance(
                summary = "No contributors found in the maDMP. Add the people responsible for data management, each with at least one role."
            )
            resultValue == ResultTestEnum.PASS -> Guidance(
                summary = "All contributors have at least one declared role."
            )
            else -> Guidance(
                summary = "${guidanceIssues.size} out of ${contributors.size} contributor(s) have no valid role declared.",
                issues = guidanceIssues
            )
        }

        return Evaluation(
            evaluationId = UUID.randomUUID().toString(),
            result = resultValue,
            details = testRecord.description,
            affectedElements = affectedContributors.ifEmpty { null },
            title = testRecord.title,
            reportId = reportId,
            log = logMessages.joinToString("\n"),
            assessmentTarget = extractAssessmentTarget(maDMP),
            wasGeneratedBy = "${this::class.qualifiedName}::contributorValuesPresent",
            outputFromTest = testRecord.id,
            completion = 100,
            guidance = guidance
            )
    }

    fun datasetEntityValuesPresent(
        maDMP: JsonObject,
        reportId: String,
        testRecord: TestRecord): Evaluation{
        var resultValue: ResultTestEnum = ResultTestEnum.INDETERMINATE
        val datasets = extractValuesByPath<Any>(maDMP, "dmp.dataset[*]")
        val logMessages = mutableListOf<String>()
        if (datasets.isEmpty()) {
            resultValue = ResultTestEnum.FAIL
            logMessages.add("dataset field is not present in the maDMP")
        } else{resultValue = ResultTestEnum.INDETERMINATE}

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
            affectedElements = listOf("dpm.dataset"),
            details = testRecord.description,
            title = testRecord.title,
            reportId = reportId,
            log = logMessages.joinToString("\n"),
            assessmentTarget = extractAssessmentTarget(maDMP),
            wasGeneratedBy = "${this::class.qualifiedName}::datasetEntityValuesPresent",
            outputFromTest = testRecord.id,
            completion = 100
        )
    }




    fun datasetPersistentIdentifierPresent(
        maDMP: JsonObject,
        reportId: String,
        testRecord: TestRecord
    ): Evaluation {
        val logMessages = mutableListOf<String>()
        val affectedDatasets = mutableListOf<String>()
        val guidanceIssues = mutableListOf<GuidanceEntry>()
        var resultValue = ResultTestEnum.INDETERMINATE
        val validTypes = setOf("doi", "handle", "ark", "url", "other")

        val datasets = extractValuesByPath<Any>(maDMP, "dmp.dataset[*]")

        if (datasets.isEmpty()) {
            logMessages.add("No datasets found in the maDMP.")
            resultValue = ResultTestEnum.FAIL
        } else {
            resultValue = ResultTestEnum.PASS
            datasets.forEachIndexed { index, element ->
                if (element is JsonObject) {
                    val datasetId = element["dataset_id"]?.jsonObjectOrNull
                    val type = datasetId?.get("type")?.jsonPrimitiveOrNull?.contentOrNull
                    val identifier = datasetId?.get("identifier")?.jsonPrimitiveOrNull?.contentOrNull
                    val title = element["title"]?.jsonPrimitiveOrNull?.contentOrNull
                    val label = buildDatasetLabel(title, type, identifier, index)

                    when {
                        type == null || type !in validTypes -> {
                            logMessages.add("Dataset[$index]: missing or invalid type '$type'. Expected one of $validTypes.")
                            affectedDatasets.add("dataset[$index]")
                            guidanceIssues.add(GuidanceEntry(
                                dataset = label,
                                reason = "Missing or invalid identifier type '${type ?: "none"}'. Expected one of: doi, handle, ark, url, other."
                            ))
                            resultValue = ResultTestEnum.FAIL
                        }
                        identifier.isNullOrBlank() -> {
                            logMessages.add("Dataset[$index]: identifier is missing or blank.")
                            affectedDatasets.add("dataset[$index]")
                            guidanceIssues.add(GuidanceEntry(
                                dataset = label,
                                reason = "The identifier value is missing or blank."
                            ))
                            resultValue = ResultTestEnum.FAIL
                        }
                        else -> {
                            val resolvedUrl = resolveIdentifierUrl(type, identifier)
                            val responseCode = getResponseCode(resolvedUrl)
                            when {
                                responseCode == null -> {
                                    logMessages.add("Dataset[$index]: identifier '$identifier' (type=$type) — could not connect to $resolvedUrl.")
                                    affectedDatasets.add("dataset[$index]:$identifier")
                                    guidanceIssues.add(GuidanceEntry(
                                        dataset = label,
                                        reason = "Could not connect to '$resolvedUrl'. Verify the URL is reachable."
                                    ))
                                    resultValue = ResultTestEnum.FAIL
                                }
                                responseCode in 404..404 || responseCode == 410 -> {
                                    logMessages.add("Dataset[$index]: identifier '$identifier' (type=$type) — resource not found at $resolvedUrl (HTTP $responseCode).")
                                    affectedDatasets.add("dataset[$index]:$identifier")
                                    guidanceIssues.add(GuidanceEntry(
                                        dataset = label,
                                        reason = "Identifier URL '$resolvedUrl' returned HTTP $responseCode (not found). The resource may have been removed or the identifier is incorrect."
                                    ))
                                    resultValue = ResultTestEnum.FAIL
                                }
                                responseCode >= 500 -> {
                                    logMessages.add("Dataset[$index]: identifier '$identifier' (type=$type) — server error at $resolvedUrl (HTTP $responseCode). Could not confirm resolvability.")
                                    affectedDatasets.add("dataset[$index]:$identifier")
                                    guidanceIssues.add(GuidanceEntry(
                                        dataset = label,
                                        reason = "Identifier URL '$resolvedUrl' returned a server error (HTTP $responseCode). The hosting service may be temporarily unavailable."
                                    ))
                                    resultValue = ResultTestEnum.FAIL
                                }
                                else -> logMessages.add("Dataset[$index]: identifier '$identifier' (type=$type) resolves at $resolvedUrl (HTTP $responseCode).")
                            }
                        }
                    }
                } else {
                    logMessages.add("Dataset[$index]: not a valid JSON object.")
                    resultValue = ResultTestEnum.FAIL
                }
            }
        }

        val guidance = when {
            resultValue != ResultTestEnum.FAIL -> Guidance(
                summary = "All datasets have valid, resolvable persistent identifiers."
            )
            datasets.isEmpty() -> Guidance(
                summary = "No datasets found in the maDMP. Add at least one dataset with a persistent identifier."
            )
            else -> Guidance(
                summary = "${guidanceIssues.size} out of ${datasets.size} dataset(s) have persistent identifier issues.",
                issues = guidanceIssues
            )
        }

        return Evaluation(
            evaluationId = UUID.randomUUID().toString(),
            result = resultValue,
            details = testRecord.description,
            title = testRecord.title,
            reportId = reportId,
            log = logMessages.joinToString("\n"),
            affectedElements = affectedDatasets.ifEmpty { null },
            assessmentTarget = extractAssessmentTarget(maDMP),
            wasGeneratedBy = "${this::class.qualifiedName}::datasetPersistentIdentifierPresent",
            outputFromTest = testRecord.id,
            completion = 100,
            guidance = guidance
        )
    }

    private fun resolveIdentifierUrl(type: String, identifier: String): String = when (type) {
        "doi"    -> if (identifier.startsWith("http")) identifier else "https://doi.org/$identifier"
        "handle" -> if (identifier.startsWith("http")) identifier else "https://hdl.handle.net/$identifier"
        else     -> identifier // ark, url, other — identifier is expected to be a full URL
    }

    private fun getResponseCode(url: String): Int? = try {
        val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 5000
        connection.readTimeout = 5000
        connection.instanceFollowRedirects = true
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (compatible; DMPEvaluationService/1.0; +https://github.com/OSTrails/DMP-Evaluation-Service)")
        connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
        val code = connection.responseCode
        connection.disconnect()
        code
    } catch (e: Exception) {
        null
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
