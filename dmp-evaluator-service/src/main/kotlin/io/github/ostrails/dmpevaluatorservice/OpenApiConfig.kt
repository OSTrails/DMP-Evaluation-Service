package io.github.ostrails.dmpevaluatorservice

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.License
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import io.swagger.v3.oas.models.info.Info
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsWebFilter
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource


@Configuration
class OpenApiConfig {

    @Bean
    fun customOpenApi(): OpenAPI{
        return OpenAPI()
            .info(Info()
            .title("DMP Evaluator Service API").version("v1.0.0").description("API for the service that evaluate DMPs")
            .license(License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0"))
            )
    }
}

@Configuration
class WebClientConfig {

    @Bean
    fun webClient(): WebClient {
        return WebClient.builder().build()
    }
}

@Configuration
class CorsGlobalConfig {

    @Bean
    fun corsWebFilter(): CorsWebFilter {
        val corsConfig = CorsConfiguration().apply {
            addAllowedOrigin("*") // or addAllowedOriginPattern("*") for Spring 6+
            addAllowedHeader("*")
            addAllowedMethod("*")
            allowCredentials = true
        }

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", corsConfig)
        return CorsWebFilter(source)
    }
}

