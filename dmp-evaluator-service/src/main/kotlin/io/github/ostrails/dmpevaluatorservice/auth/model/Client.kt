package io.github.ostrails.dmpevaluatorservice.auth.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "clients")
data class Client(
    @Id val id: String? = null,
    @Indexed(unique = true) val clientId: String,
    val passwordHash: String,
    val displayName: String,
    val roles: Set<Role>,
    val enabled: Boolean = true
)
