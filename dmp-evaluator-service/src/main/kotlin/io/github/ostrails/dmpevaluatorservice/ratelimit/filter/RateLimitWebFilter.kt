package io.github.ostrails.dmpevaluatorservice.ratelimit.filter

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.github.ostrails.dmpevaluatorservice.exceptionHandler.ErrorResponse
import io.github.ostrails.dmpevaluatorservice.ratelimit.config.RateLimitProperties
import com.github.benmanes.caffeine.cache.Caffeine
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import java.time.Duration
import java.util.concurrent.TimeUnit

class RateLimitWebFilter(
    private val properties: RateLimitProperties,
    private val objectMapper: ObjectMapper
) : WebFilter {

    private val logger = LoggerFactory.getLogger(RateLimitWebFilter::class.java)

    private val buckets = Caffeine.newBuilder()
        .expireAfterAccess(Duration.ofMinutes(10))
        .maximumSize(100_000)
        .build<String, Bucket> { newBucket() }

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        if (!properties.enabled || exchange.request.method == HttpMethod.OPTIONS) {
            return chain.filter(exchange)
        }

        val clientIp = resolveClientIp(exchange)
        val probe = buckets.get(clientIp)!!.tryConsumeAndReturnRemaining(1)

        if (probe.isConsumed) {
            exchange.response.headers.add("X-RateLimit-Remaining", probe.remainingTokens.toString())
            return chain.filter(exchange)
        }

        val retryAfterSeconds = TimeUnit.NANOSECONDS.toSeconds(probe.nanosToWaitForRefill) + 1
        logger.warn("[RATE-LIMIT] Rejected {} {} from {} — retry after {}s", exchange.request.method, exchange.request.path, clientIp, retryAfterSeconds)
        return writeTooManyRequests(exchange, retryAfterSeconds)
    }

    private fun newBucket(): Bucket {
        val limit = Bandwidth.builder()
            .capacity(properties.capacity)
            .refillGreedy(properties.refillTokens, Duration.ofSeconds(properties.refillDurationSeconds))
            .build()
        return Bucket.builder().addLimit(limit).build()
    }

    private fun resolveClientIp(exchange: ServerWebExchange): String {
        val forwardedFor = exchange.request.headers.getFirst("X-Forwarded-For")
        if (!forwardedFor.isNullOrBlank()) {
            return forwardedFor.split(",").first().trim()
        }
        return exchange.request.remoteAddress?.address?.hostAddress ?: "unknown"
    }

    private fun writeTooManyRequests(exchange: ServerWebExchange, retryAfterSeconds: Long): Mono<Void> {
        val response = exchange.response
        response.statusCode = HttpStatus.TOO_MANY_REQUESTS
        response.headers.contentType = MediaType.APPLICATION_JSON
        response.headers.add(HttpHeaders.RETRY_AFTER, retryAfterSeconds.toString())
        val body = objectMapper.writeValueAsBytes(
            ErrorResponse(
                code = "RATE_LIMIT_EXCEEDED",
                message = "Too many requests from your IP address — please slow down and retry after $retryAfterSeconds seconds",
                path = exchange.request.path.toString()
            )
        )
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body)))
    }
}
