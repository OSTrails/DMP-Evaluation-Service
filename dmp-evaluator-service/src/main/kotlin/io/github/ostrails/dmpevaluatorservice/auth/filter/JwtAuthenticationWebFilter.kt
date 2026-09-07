package io.github.ostrails.dmpevaluatorservice.auth.filter

import com.github.benmanes.caffeine.cache.AsyncLoadingCache
import com.github.benmanes.caffeine.cache.Caffeine
import io.github.ostrails.dmpevaluatorservice.auth.repository.ClientRepository
import io.github.ostrails.dmpevaluatorservice.auth.service.JwtService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import java.time.Duration

class JwtAuthenticationWebFilter(
    private val jwtService: JwtService,
    private val clientRepository: ClientRepository
) : WebFilter {

    private val logger = LoggerFactory.getLogger(JwtAuthenticationWebFilter::class.java)

    private val activeClientCache: AsyncLoadingCache<String, Boolean> = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofSeconds(30))
        .maximumSize(10_000)
        .buildAsync { clientId, _ ->
            clientRepository.findByClientId(clientId)
                .map { it.enabled }
                .defaultIfEmpty(false)
                .toFuture()
        }

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val token = extractToken(exchange) ?: return chain.filter(exchange)
        return try {
            val claims = jwtService.parseToken(token)
            val clientId = claims.subject
            val roles = (claims["roles"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()

            Mono.fromFuture { activeClientCache.get(clientId) }
                .flatMap { isActive ->
                    if (isActive) {
                        val authorities = roles.map { SimpleGrantedAuthority("ROLE_$it") }
                        val authentication = UsernamePasswordAuthenticationToken(clientId, null, authorities)
                        chain.filter(exchange)
                            .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication))
                    } else {
                        chain.filter(exchange)
                    }
                }
        } catch (e: Exception) {
            logger.warn("Rejected JWT on {}: {}", exchange.request.path, e.message)
            chain.filter(exchange)
        }
    }

    private fun extractToken(exchange: ServerWebExchange): String? {
        val header = exchange.request.headers.getFirst(HttpHeaders.AUTHORIZATION) ?: return null
        if (!header.startsWith("Bearer ")) return null
        return header.substring(7)
    }
}
