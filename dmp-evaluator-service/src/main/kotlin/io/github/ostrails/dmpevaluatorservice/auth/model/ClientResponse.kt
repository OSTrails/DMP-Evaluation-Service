package io.github.ostrails.dmpevaluatorservice.auth.model

data class ClientResponse(
    val clientId: String,
    val displayName: String,
    val roles: Set<Role>,
    val enabled: Boolean
)

fun Client.toResponse() = ClientResponse(
    clientId = clientId,
    displayName = displayName,
    roles = roles,
    enabled = enabled
)
