package io.github.ostrails.dmpevaluatorservice.auth.filter

import io.github.ostrails.dmpevaluatorservice.auth.service.JwtService
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

class JwtAuthenticationWebFilter(private val jwtService: JwtService) : WebFilter {

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val token = extractToken(exchange) ?: return chain.filter(exchange)
        return try {
            val claims = jwtService.parseToken(token)
            val clientId = claims.subject
            @Suppress("UNCHECKED_CAST")
            val roles = (claims["roles"] as? List<String>) ?: emptyList()
            val authorities = roles.map { SimpleGrantedAuthority("ROLE_$it") }
            val authentication = UsernamePasswordAuthenticationToken(clientId, null, authorities)
            chain.filter(exchange)
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication))
        } catch (e: Exception) {
            chain.filter(exchange)
        }
    }

    private fun extractToken(exchange: ServerWebExchange): String? {
        val header = exchange.request.headers.getFirst(HttpHeaders.AUTHORIZATION) ?: return null
        if (!header.startsWith("Bearer ")) return null
        return header.substring(7)
    }
}
