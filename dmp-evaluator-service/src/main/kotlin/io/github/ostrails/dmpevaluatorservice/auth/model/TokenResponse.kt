package io.github.ostrails.dmpevaluatorservice.auth.model

data class TokenResponse(
    val accessToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long
)
