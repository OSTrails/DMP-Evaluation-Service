package io.github.ostrails.dmpevaluatorservice.auth.model

data class RegisterClientRequest(
    val clientId: String,
    val clientSecret: String,
    val displayName: String,
    val roles: Set<Role>
)
