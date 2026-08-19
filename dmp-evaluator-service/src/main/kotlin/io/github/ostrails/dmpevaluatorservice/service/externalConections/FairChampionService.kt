package io.github.ostrails.dmpevaluatorservice.service.externalConections

import io.github.ostrails.dmpevaluatorservice.utils.ConfigurationGlobalVariables
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.serialization.json.*
import kotlinx.serialization.json.buildJsonObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient

@Service
class FairChampionService(
    private val webClient: WebClient,
    val globalVariables: ConfigurationGlobalVariables
) {

    private val log: Logger = LoggerFactory.getLogger(FairChampionService::class.java)

    suspend fun assessBenchmark(guid: String): JsonObject {
        log.debug("Requesting FAIR Champion benchmark assessment for guid '$guid'")
        val requestBody = buildJsonObject { put("guid", guid) }
        val rawResponse = webClient.post()
            .uri(globalVariables.fairChampionBenchmarkEndpoint)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(requestBody.toString())
            .exchangeToMono { response: ClientResponse ->
                val status = response.statusCode().value()
                if (response.statusCode().is2xxSuccessful) {
                    response.bodyToMono(String::class.java).map { body ->
                        buildJsonObject {
                            put("success", true)
                            put("status", status)
                            put("data", Json.parseToJsonElement(body))
                        }.toString()
                    }
                } else {
                    log.warn("FAIR Champion benchmark assessment for guid '$guid' failed with status $status")
                    response.bodyToMono(String::class.java).map { errorBody ->
                        buildJsonObject {
                            put("success", false)
                            put("status", status)
                            put("error", errorBody)
                        }.toString()
                    }
                }
            }
            .awaitSingle()
        return Json.parseToJsonElement(rawResponse).jsonObject
    }

    suspend fun assessTest(testName: String, resourceUrl: String): JsonObject {
        val endpoint = globalVariables.fairChampionEndPoint + testName
        log.debug("Requesting FAIR Champion test '$testName' for resource '$resourceUrl'")

        val requestBody = buildJsonObject {
            put("resource_identifier", resourceUrl)
        }
        val rawResponse = webClient.post()
            .uri(endpoint)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(requestBody.toString())
            .exchangeToMono { response: ClientResponse ->
                val status = response.statusCode().value()
                if (response.statusCode().is2xxSuccessful) {
                    response.bodyToMono(String::class.java).map { body ->
                        buildJsonObject {
                            put("success", true)
                            put("status", status)
                            put("data", Json.parseToJsonElement(body))
                        }.toString()
                    }
                } else {
                    log.warn("FAIR Champion test '$testName' for resource '$resourceUrl' failed with status $status")
                    response.bodyToMono(String::class.java).map { errorBody ->
                        buildJsonObject {
                            put("success", false)
                            put("status", status)
                            put("error", errorBody)
                        }.toString()
                    }
                }
            }
            .awaitSingle()

        return Json.parseToJsonElement(rawResponse).jsonObject
    }
}