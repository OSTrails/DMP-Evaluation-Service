package io.github.ostrails.dmpevaluatorservice.auth.model

data class ChangeSecretRequest(
    val currentSecret: String,
    val newSecret: String
)
