package io.github.ostrails.dmpevaluatorservice.service.externalConections

import io.github.ostrails.dmpevaluatorservice.utils.ConfigurationGlobalVariables
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.serialization.json.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.ClientResponse

@Service
class UnpaywallService(private val webClient: WebClient,
    val globalVariables: ConfigurationGlobalVariables) {

    private val log: Logger = LoggerFactory.getLogger(UnpaywallService::class.java)

    suspend fun checkOpenAccess(doi: String, email: String="dmpEvalutionService@test.com" ): JsonObject {
        log.debug("Checking open access status for DOI '$doi'")
        val response = webClient.get()
            .uri(globalVariables.unpayWallEndPoint + "$doi?email=" + globalVariables.unpayWallEmail)
            .exchangeToMono { response: ClientResponse ->
            val statusCode = response.statusCode().value()
            if (response.statusCode().is2xxSuccessful) {
                response.bodyToMono(String::class.java).map { body ->
                    buildJsonObject {
                        put("success", true)
                        put("status", statusCode)
                        put("data", Json.parseToJsonElement(body))
                    }.toString()
                }
            } else {
                log.warn("Unpaywall lookup for DOI '$doi' failed with status $statusCode")
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