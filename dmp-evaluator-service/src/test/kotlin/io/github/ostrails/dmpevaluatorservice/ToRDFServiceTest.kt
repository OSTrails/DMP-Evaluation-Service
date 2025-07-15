package io.github.ostrails.dmpevaluatorservice

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.core.io.ClassPathResource

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ToRDFServiceTest {

    @Autowired
    lateinit var webTestClient: WebTestClient

    @Test
    fun ex9LongToRDFTest() {
        val resource = ClassPathResource("ex9-dmp-long.json")

        webTestClient.post()
            .uri("/assess/mappingRDF")
            .bodyValue(org.springframework.util.LinkedMultiValueMap<String, Any>().apply {
                add("maDMP", resource)
            })
            .exchange()
            .expectStatus().isOk
    }
}