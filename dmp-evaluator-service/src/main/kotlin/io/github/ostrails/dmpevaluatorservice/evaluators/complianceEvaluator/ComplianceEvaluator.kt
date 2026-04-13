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
import java.net.URI
import java.net.URL
import java.net.URLEncoder
import java.util.*

@Component
class ComplianceEvaluator: EvaluatorPlugin {

    companion object {
        private const val OKF_OPEN_LICENSES_URL = "https://licenses.opendefinition.org/licenses/groups/all.json"
        private const val RE3DATA_LIST_URL = "https://www.re3data.org/api/v1/repositories"
        private const val RE3DATA_SUGGEST_URL = "https://www.re3data.org/api/opensearch/suggest"
        private const val RE3DATA_DETAIL_URL = "https://www.re3data.org/api/v1/repository"

        // Full RE3data repository list cache: name (lowercase) → id
        // Populated once on first RE3data lookup and reused for the application lifetime
        val re3dataRepositoryListCache: MutableMap<String, String> = mutableMapOf()

        // Per-domain result caches to avoid redundant API calls within the same run
        val re3dataFoundCache: MutableSet<String> = mutableSetOf()
        val re3dataNotFoundCache: MutableSet<String> = mutableSetOf()

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
        "datasetRepositoryIsInRe3data" to ::datasetRepositoryIsInRe3data,
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

    fun datasetRepositoryIsInRe3data(
        maDMP: JsonObject,
        reportId: String,
        testRecord: TestRecord
    ): Evaluation {
        val logMessages = mutableListOf<String>()
        val affectedElements = mutableListOf<String>()
        var resultValue = ResultTestEnum.INDERTERMINATED

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
                            val host = distObj?.get("host") as? JsonObject
                            val hostUrl = host?.get("url")?.jsonPrimitiveOrNull?.contentOrNull
                            when {
                                host == null -> {
                                    logMessages.add("Dataset[$datasetIndex] distribution[$distIndex]: no host declared.")
                                    affectedElements.add("dataset[$datasetIndex].distribution[$distIndex]")
                                    resultValue = ResultTestEnum.FAIL
                                }
                                hostUrl.isNullOrBlank() -> {
                                    logMessages.add("Dataset[$datasetIndex] distribution[$distIndex]: host.url is missing.")
                                    affectedElements.add("dataset[$datasetIndex].distribution[$distIndex].host")
                                    resultValue = ResultTestEnum.FAIL
                                }
                                !isRepositoryInRe3data(hostUrl, logMessages) -> {
                                    logMessages.add("Dataset[$datasetIndex] distribution[$distIndex]: '$hostUrl' was not found in RE3data.")
                                    affectedElements.add("dataset[$datasetIndex].distribution[$distIndex].host:$hostUrl")
                                    resultValue = ResultTestEnum.FAIL
                                }
                                else -> logMessages.add("Dataset[$datasetIndex] distribution[$distIndex]: '$hostUrl' is registered in RE3data.")
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
            wasGeneratedBy = "${this::class.qualifiedName}::datasetRepositoryIsInRe3data",
            outputFromTest = testRecord.id,
            completion = 100
        )
    }

    private fun isRepositoryInRe3data(hostUrl: String, logMessages: MutableList<String>): Boolean {
        val domain = normalizedDomain(hostUrl)

        // Short-circuit: this domain was already verified in a previous call
        if (re3dataFoundCache.contains(domain)) {
            logMessages.add("RE3data cache hit: '$domain' is registered.")
            return true
        }
        if (re3dataNotFoundCache.contains(domain)) {
            logMessages.add("RE3data cache hit: '$domain' is not registered.")
            return false
        }

        // Step 1: load the full RE3data repository list (name → id) if not already cached.
        // This is fetched once per application lifetime and reused for all subsequent lookups.
        if (re3dataRepositoryListCache.isEmpty()) {
            fetchRe3dataFullList(logMessages)
        }
        if (re3dataRepositoryListCache.isEmpty()) {
            // List fetch failed — cannot perform a reliable lookup
            logMessages.add("RE3data full list could not be loaded — cannot verify '$domain'.")
            return false
        }

        // Step 2: use the OpenSearch suggest endpoint to get candidate repository names for
        // this domain. The results are then matched against the full list to resolve IDs.
        val suggestedNames = fetchSuggestedNames(domain, logMessages)
        if (suggestedNames.isEmpty()) {
            logMessages.add("RE3data suggest returned no candidates for '$domain'.")
            re3dataNotFoundCache.add(domain)
            return false
        }

        // Step 3: resolve suggested names → repository IDs via the cached full list
        val candidateIds = suggestedNames.mapNotNull { name ->
            re3dataRepositoryListCache[name.lowercase()]
        }
        if (candidateIds.isEmpty()) {
            logMessages.add("None of the suggested names matched a known RE3data ID for '$domain'.")
            re3dataNotFoundCache.add(domain)
            return false
        }

        // Step 4: verify each candidate by checking its detail endpoint for a URL match
        val found = candidateIds.take(5).any { repoId ->
            checkRe3dataDetail(repoId, domain, logMessages)
        }

        if (found) re3dataFoundCache.add(domain) else re3dataNotFoundCache.add(domain)
        return found
    }

    // Fetches the full RE3data repository list and populates re3dataRepositoryListCache (name → id)
    private fun fetchRe3dataFullList(logMessages: MutableList<String>) {
        try {
            val connection = URL(RE3DATA_LIST_URL).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 8000
            connection.readTimeout = 15000
            connection.setRequestProperty("Accept", "application/xml")
            if (connection.responseCode == 200) {
                val xml = connection.inputStream.bufferedReader().readText()
                connection.disconnect()
                val idPattern = Regex("<id>\\s*(.*?)\\s*</id>")
                val namePattern = Regex("<name>\\s*(.*?)\\s*</name>")
                val repoBlocks = Regex("<repository>(.*?)</repository>", RegexOption.DOT_MATCHES_ALL)
                repoBlocks.findAll(xml).forEach { block ->
                    val id = idPattern.find(block.value)?.groupValues?.get(1) ?: return@forEach
                    val name = namePattern.find(block.value)?.groupValues?.get(1) ?: return@forEach
                    re3dataRepositoryListCache[name.lowercase()] = id
                }
                logMessages.add("RE3data full list loaded: ${re3dataRepositoryListCache.size} repositories cached.")
            } else {
                connection.disconnect()
                logMessages.add("RE3data list endpoint returned HTTP ${connection.responseCode} — cache not populated.")
            }
        } catch (e: Exception) {
            logMessages.add("RE3data list endpoint unreachable (${e.message}) — cache not populated.")
        }
    }

    // Calls the OpenSearch suggest endpoint and returns the list of suggested repository names
    private fun fetchSuggestedNames(domain: String, logMessages: MutableList<String>): List<String> {
        return try {
            val encodedDomain = URLEncoder.encode(domain, "UTF-8")
            val connection = URL("$RE3DATA_SUGGEST_URL?search=$encodedDomain").openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 8000
            connection.setRequestProperty("Accept", "application/json")
            if (connection.responseCode == 200) {
                val body = connection.inputStream.bufferedReader().readText()
                connection.disconnect()
                // OpenSearch Suggest format: ["query", ["name1","name2",...], [...], [...]]
                val parsed = Json.parseToJsonElement(body).jsonArray
                parsed.getOrNull(1)?.jsonArray?.mapNotNull { it.jsonPrimitiveOrNull?.contentOrNull } ?: emptyList()
            } else {
                connection.disconnect()
                logMessages.add("RE3data suggest returned HTTP ${connection.responseCode} for '$domain'.")
                emptyList()
            }
        } catch (e: Exception) {
            logMessages.add("RE3data suggest endpoint unreachable (${e.message}) for '$domain'.")
            emptyList()
        }
    }

    // Fetches the repository detail and checks whether its repositoryURL domain matches
    private fun checkRe3dataDetail(repoId: String, domain: String, logMessages: MutableList<String>): Boolean {
        return try {
            val connection = URL("$RE3DATA_DETAIL_URL/$repoId").openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 8000
            val responseCode = connection.responseCode
            if (responseCode == 200) {
                val xml = connection.inputStream.bufferedReader().readText()
                connection.disconnect()
                val urlPattern = Regex("<r3d:repositoryURL>\\s*(.*?)\\s*</r3d:repositoryURL>")
                val repoUrl = urlPattern.find(xml)?.groupValues?.get(1) ?: return false
                val repoDomain = normalizedDomain(repoUrl)
                val match = repoDomain == domain || repoDomain.contains(domain) || domain.contains(repoDomain)
                if (match) logMessages.add("RE3data repo '$repoId': URL '$repoUrl' matches domain '$domain'.")
                match
            } else {
                connection.disconnect()
                false
            }
        } catch (e: Exception) {
            logMessages.add("RE3data detail fetch failed for '$repoId': ${e.message}")
            false
        }
    }

    private fun normalizedDomain(url: String): String = try {
        URI(url.trim()).host?.lowercase()?.removePrefix("www.") ?: url.lowercase().trim()
    } catch (e: Exception) {
        url.lowercase().trim()
    }
}
