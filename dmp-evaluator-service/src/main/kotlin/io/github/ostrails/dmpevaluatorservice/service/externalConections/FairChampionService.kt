package io.github.ostrails.dmpevaluatorservice.service.externalConections

import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.serialization.json.*
import kotlinx.serialization.json.buildJsonObject
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient

@Service
class FairChampionService(
    private val webClient: WebClient
) {

    suspend fun assessTest(testName: String, resourceUrl: String): JsonObject {
        val endpoint = "https://tests.ostrails.eu/assess/test/$testName"

        val requestBody = buildJsonObject {
            put("resource_identifier", resourceUrl)
        }
        val rawResponse = webClient.post()
            .uri(endpoint)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(requestBody.toString())
            .exchangeToMono { response: ClientResponse ->
                val status = response.rawStatusCode()
                if (response.statusCode().is2xxSuccessful) {
                    response.bodyToMono(String::class.java).map { body ->
                        buildJsonObject {
                            put("success", true)
                            put("status", status)
                            put("data", Json.parseToJsonElement(body))
                        }.toString()
                    }
                } else {
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