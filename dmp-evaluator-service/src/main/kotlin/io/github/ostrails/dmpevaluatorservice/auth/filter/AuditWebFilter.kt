package io.github.ostrails.dmpevaluatorservice.auth.filter

import org.slf4j.LoggerFactory
import org.springframework.http.HttpMethod
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import java.time.Instant

class AuditWebFilter : WebFilter {
    private val logger = LoggerFactory.getLogger(AuditWebFilter::class.java)
    private val auditedMethods = setOf(HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE)

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val method = exchange.request.method
        if (method !in auditedMethods) return chain.filter(exchange)
        val path = exchange.request.path

        return ReactiveSecurityContextHolder.getContext()
            .map { it.authentication?.name ?: "anonymous" }
            .defaultIfEmpty("anonymous")
            .flatMap { caller ->
                logger.info("[AUDIT] {} {} by \"{}\" at {}", method, path, caller, Instant.now())
                chain.filter(exchange)
            }
    }
}
