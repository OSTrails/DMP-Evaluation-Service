package io.github.ostrails.dmpevaluatorservice.auth.model

data class TokenRequest(
    val clientId: String,
    val clientSecret: String
)
