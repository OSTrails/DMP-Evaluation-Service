package io.github.ostrails.dmpevaluatorservice.auth.config

import io.github.ostrails.dmpevaluatorservice.auth.filter.AuditWebFilter
import io.github.ostrails.dmpevaluatorservice.auth.filter.JwtAuthenticationWebFilter
import io.github.ostrails.dmpevaluatorservice.auth.service.JwtService
import io.github.ostrails.dmpevaluatorservice.exceptionHandler.ErrorResponse
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@Configuration
@EnableWebFluxSecurity
class SecurityConfig(
    private val jwtService: JwtService,
    private val objectMapper: ObjectMapper
) {

    @Bean
    fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http
            .csrf { it.disable() }
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            .authorizeExchange { auth ->
                auth
                    // Swagger UI — always public
                    .pathMatchers(
                        "/swagger-ui.html", "/swagger-ui/**",
                        "/v3/api-docs/**", "/webjars/**"
                    ).permitAll()
                    // CORS preflight
                    .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    // Auth endpoint — public
                    .pathMatchers(HttpMethod.POST, "/auth/token").permitAll()
                    // Assessment submissions — public (anyone can evaluate a DMP without a token)
                    .pathMatchers(HttpMethod.POST, "/assess/**").permitAll()
                    // Admin client management — ADMIN only (covers all HTTP methods on /admin/**)
                    .pathMatchers("/admin/**").hasRole("ADMIN")
                    // DELETE — ADMIN only
                    .pathMatchers(HttpMethod.DELETE, "/**").hasRole("ADMIN")
                    // All other writes — ADMIN or WRITER
                    .pathMatchers(HttpMethod.POST, "/**").hasAnyRole("ADMIN", "WRITER")
                    .pathMatchers(HttpMethod.PUT, "/**").hasAnyRole("ADMIN", "WRITER")
                    // All reads — public
                    .pathMatchers(HttpMethod.GET, "/**").permitAll()
                    .anyExchange().authenticated()
            }
            .addFilterAt(JwtAuthenticationWebFilter(jwtService), SecurityWebFiltersOrder.AUTHENTICATION)
            .addFilterAfter(AuditWebFilter(), SecurityWebFiltersOrder.AUTHENTICATION)
            .exceptionHandling { ex ->
                ex.authenticationEntryPoint { exchange, _ ->
                    writeError(exchange, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication required")
                }
                ex.accessDeniedHandler { exchange, _ ->
                    writeError(exchange, HttpStatus.FORBIDDEN, "FORBIDDEN", "You do not have permission to perform this action")
                }
            }
            .build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    private fun writeError(
        exchange: ServerWebExchange,
        status: HttpStatus,
        code: String,
        message: String
    ): Mono<Void> {
        val response = exchange.response
        response.statusCode = status
        response.headers.contentType = MediaType.APPLICATION_JSON
        val body = objectMapper.writeValueAsBytes(
            ErrorResponse(code = code, message = message, path = exchange.request.path.toString())
        )
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body)))
    }
}
