package io.github.ostrails.dmpevaluatorservice

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import io.swagger.v3.oas.models.info.Info
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsWebFilter
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource


@Configuration
class OpenApiConfig(
    @Value("\${openapi.title}") private val title: String,
    @Value("\${openapi.version}") private val version: String,
    @Value("\${openapi.description}") private val description: String,
    @Value("\${openapi.license.name}") private val licenseName: String,
    @Value("\${openapi.license.url}") private val licenseUrl: String,
) {

    @Bean
    fun customOpenApi(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title(title).version(version).description(description)
                    .license(License().name(licenseName).url(licenseUrl))
            )
            .components(
                Components().addSecuritySchemes(
                    "bearerAuth",
                    SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Paste your JWT token obtained from POST /auth/token")
                )
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
            //allowCredentials = true
        }

        corsConfig.addAllowedOriginPattern("*")
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", corsConfig)
        return CorsWebFilter(source)
    }
}

