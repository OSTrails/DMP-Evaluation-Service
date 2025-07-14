package io.github.ostrails.dmpevaluatorservice.service.externalConections

import io.github.ostrails.dmpevaluatorservice.utils.ConfigurationGlobalVariables
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.serialization.json.*
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.ClientResponse

@Service
class UnpaywallService(private val webClient: WebClient,
    val globalVariables: ConfigurationGlobalVariables) {
    suspend fun checkOpenAccess(doi: String, email: String="dmpEvalutionService@test.com" ): JsonObject {
        val response = webClient.get()
            .uri(globalVariables.unpayWallEndPoint + "$doi?email=" + globalVariables.unpayWallEmail)
            .exchangeToMono { response: ClientResponse ->
            val statusCode = response.rawStatusCode()
            if (response.statusCode().is2xxSuccessful) {
                response.bodyToMono(String::class.java).map { body ->
                    buildJsonObject {
                        put("success", true)
                        put("status", statusCode)
                        put("data", Json.parseToJsonElement(body))
                    }.toString()
                }
            } else {
                response.bodyToMono(String::class.java).map { errorBody ->
                    buildJsonObject {
                        put("success", false)
                        put("status", statusCode)
                        put("error", errorBody)
                    }.toString()
                }
            }
        }
            .awaitSingle()
        val json = Json.parseToJsonElement(response).jsonObject
        return json
    }
}