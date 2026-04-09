package io.github.ostrails.dmpevaluatorservice.evaluators.complianceEvaluator

import io.github.ostrails.dmpevaluatorservice.database.model.Evaluation
import io.github.ostrails.dmpevaluatorservice.database.model.EvaluationReport
import io.github.ostrails.dmpevaluatorservice.database.model.TestRecord
import io.github.ostrails.dmpevaluatorservice.model.PluginInfo
import io.github.ostrails.dmpevaluatorservice.model.ResultTestEnum
import io.github.ostrails.dmpevaluatorservice.plugin.EvaluatorPlugin
import io.github.ostrails.dmpevaluatorservice.utils.extractValuesByPath
import io.github.ostrails.dmpevaluatorservice.utils.jsonArrayOrNull
import io.github.ostrails.dmpevaluatorservice.utils.jsonPrimitiveOrNull
import kotlinx.serialization.json.*
import org.springframework.stereotype.Component
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

@Component
class ComplianceEvaluator: EvaluatorPlugin {

    companion object {
        private const val OKF_OPEN_LICENSES_URL = "https://licenses.opendefinition.org/licenses/groups/all.json"

        // Baseline fallback list — updated at runtime whenever the OKF API responds successfully
        val openLicenseCache: MutableSet<String> = mutableSetOf(
            "https://creativecommons.org/publicdomain/zero/1.0/",
            "https://creativecommons.org/licenses/by/4.0/",
            "https://creativecommons.org/licenses/by/3.0/",
            "https://creativecommons.org/licenses/by/2.5/",
            "https://creativecommons.org/licenses/by/2.0/",
            "https://creativecommons.org/licenses/by-sa/4.0/",
            "https://creativecommons.org/licenses/by-sa/3.0/",
            "https://opendatacommons.org/licenses/odbl/1-0/",
            "https://opendatacommons.org/licenses/pddl/1-0/",
            "https://opendatacommons.org/licenses/by/1-0/",
            "https://opensource.org/licenses/MIT",
            "https://www.apache.org/licenses/LICENSE-2.0"
        )
    }

    override fun supports(t: String): Boolean = t == getPluginIdentifier()

    override fun getPluginIdentifier(): String = "ComplianceEvaluator"

    override fun getPluginInformation(): PluginInfo = PluginInfo(
        pluginId = getPluginIdentifier(),
        description = "Evaluator to perform Compliance tests",
        functions = listOf()
    )

    override val functionMap = mapOf(
        "evaluateCoherentLicense" to ::evaluateLicenseCompliance,
        "checkFormatFile" to ::checkFormatFile,
        "datasetLicenseIsOpen" to ::datasetLicenseIsOpen,
    )

    override fun evaluate(maDMP: Map<String, Any>, config: Map<String, Any>, tests: List<String>, report: EvaluationReport): List<Evaluation> {
        return tests.map { test ->
            Evaluation(
                evaluationId = UUID.randomUUID().toString(),
                result = ResultTestEnum.PASS,
                title = "Testing ",
                details = "Auto-generated evaluation of the test" + test,
                reportId = report.reportId
            )
        }
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
                outputFromTest = testRecord.id,
                log = "The provided maDMP could not be parsed as a JsonObject."
            )
        val extension = json["fileExtension"]?.jsonPrimitive?.contentOrNull?.lowercase()
        val result = if (extension == "json") ResultTestEnum.PASS else ResultTestEnum.FAIL
        val logMessage = if (extension != "json")
            "File extension is '$extension'. Expected: 'json'."
        else
            "File extension is a valid json."
        return Evaluation(
            evaluationId = UUID.randomUUID().toString(),
            result = result,
            title = testRecord.title,
            details = testRecord.description,
            reportId = reportId,
            outputFromTest = testRecord.id,
            log = logMessage
        )
    }

    fun datasetLicenseIsOpen(
        maDMP: JsonObject,
        reportId: String,
        testRecord: TestRecord
    ): Evaluation {
        val logMessages = mutableListOf<String>()
        val affectedElements = mutableListOf<String>()
        var resultValue = ResultTestEnum.INDERTERMINATED

        val openLicenses = fetchAndUpdateOpenLicenses(logMessages)

        val datasets = extractValuesByPath<Any>(maDMP, "dmp.dataset[*]")

        if (datasets.isEmpty()) {
            logMessages.add("No datasets found in the maDMP.")
            resultValue = ResultTestEnum.FAIL
        } else {
            resultValue = ResultTestEnum.PASS
            datasets.forEachIndexed { datasetIndex, datasetElement ->
                if (datasetElement is JsonObject) {
                    val distributions = datasetElement["distribution"]?.jsonArrayOrNull()
                    if (distributions == null || distributions.isEmpty()) {
                        logMessages.add("Dataset[$datasetIndex]: no distributions found.")
                        affectedElements.add("dataset[$datasetIndex]")
                        resultValue = ResultTestEnum.FAIL
                    } else {
                        distributions.forEachIndexed { distIndex, distElement ->
                            val distObj = distElement as? JsonObject
                            val licenses = distObj?.get("license")?.jsonArrayOrNull()
                            if (licenses == null || licenses.isEmpty()) {
                                logMessages.add("Dataset[$datasetIndex] distribution[$distIndex]: no license found.")
                                affectedElements.add("dataset[$datasetIndex].distribution[$distIndex]")
                                resultValue = ResultTestEnum.FAIL
                            } else {
                                licenses.forEachIndexed { licIndex, licElement ->
                                    val licRef = (licElement as? JsonObject)
                                        ?.get("license_ref")?.jsonPrimitiveOrNull?.contentOrNull
                                    when {
                                        licRef.isNullOrBlank() -> {
                                            logMessages.add("Dataset[$datasetIndex] distribution[$distIndex] license[$licIndex]: license_ref is missing.")
                                            affectedElements.add("dataset[$datasetIndex].distribution[$distIndex].license[$licIndex]")
                                            resultValue = ResultTestEnum.FAIL
                                        }
                                        !isOpenLicense(licRef, openLicenses) -> {
                                            logMessages.add("Dataset[$datasetIndex] distribution[$distIndex] license[$licIndex]: '$licRef' is not a recognized open license.")
                                            affectedElements.add("dataset[$datasetIndex].distribution[$distIndex].license[$licIndex]:$licRef")
                                            resultValue = ResultTestEnum.FAIL
                                        }
                                        else -> logMessages.add("Dataset[$datasetIndex] distribution[$distIndex] license[$licIndex]: '$licRef' is an open license.")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return Evaluation(
            evaluationId = UUID.randomUUID().toString(),
            result = resultValue,
            details = testRecord.description,
            title = testRecord.title,
            reportId = reportId,
            log = logMessages.joinToString("\n"),
            affectedElements = affectedElements.ifEmpty { null },
            assessmentTarget = "https://www.rd-alliance.org/group/dmp-common-standards-wg/outcomes/rda",
            wasGeneratedBy = "${this::class.qualifiedName}::datasetLicenseIsOpen",
            outputFromTest = testRecord.id,
            completion = 100
        )
    }

    // Calls the OKF API. On success, adds the returned URLs to the cache and returns it.
    // On failure, logs the reason and returns whatever is currently in the cache.
    private fun fetchAndUpdateOpenLicenses(logMessages: MutableList<String>): Set<String> {
        return try {
            val connection = URL(OKF_OPEN_LICENSES_URL).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.setRequestProperty("Accept", "application/json")
            val responseCode = connection.responseCode
            if (responseCode == 200) {
                val content = connection.inputStream.bufferedReader().readText()
                connection.disconnect()
                val jsonObject = Json.parseToJsonElement(content).jsonObject
                val fetchedUrls = jsonObject.values.mapNotNull { entry ->
                    (entry as? JsonObject)?.get("url")?.jsonPrimitiveOrNull?.contentOrNull
                        ?.takeIf { it.isNotBlank() }
                }
                openLicenseCache.addAll(fetchedUrls)
                logMessages.add("Open license list updated from OKF API (${fetchedUrls.size} licenses loaded).")
            } else {
                connection.disconnect()
                logMessages.add("OKF API returned HTTP $responseCode — using cached license list (${openLicenseCache.size} licenses).")
            }
            openLicenseCache
        } catch (e: Exception) {
            logMessages.add("OKF API unreachable (${e.message}) — using cached license list (${openLicenseCache.size} licenses).")
            openLicenseCache
        }
    }

    // Normalizes both sides (lowercase, strip trailing slash) before comparing
    private fun isOpenLicense(licenseRef: String, openLicenses: Set<String>): Boolean {
        val normalized = licenseRef.trimEnd('/').lowercase()
        return openLicenses.any { it.trimEnd('/').lowercase() == normalized }
    }
}
