package io.github.ostrails.dmpevaluatorservice.auth.service

import io.github.ostrails.dmpevaluatorservice.auth.model.Client
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.Date
import javax.crypto.SecretKey

@Service
class JwtService(
    @Value("\${jwt.secret}") private val secret: String,
    @Value("\${jwt.expiration-seconds:3600}") val expirationSeconds: Long
) {
    private val key: SecretKey by lazy {
        Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret))
    }

    fun generateToken(client: Client): String {
        return Jwts.builder()
            .subject(client.clientId)
            .claim("displayName", client.displayName)
            .claim("roles", client.roles.map { it.name })
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + expirationSeconds * 1000))
            .signWith(key)
            .compact()
    }

    fun parseToken(token: String): Claims {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload
    }

    fun isValid(token: String): Boolean = try {
        parseToken(token)
        true
    } catch (e: Exception) {
        false
    }
}
