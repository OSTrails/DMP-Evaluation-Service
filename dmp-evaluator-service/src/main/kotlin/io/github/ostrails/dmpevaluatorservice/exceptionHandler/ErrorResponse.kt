package io.github.ostrails.dmpevaluatorservice.exceptionHandler

import java.time.Instant

data class ErrorResponse(
    val code: String,
    val message: String,
    val timestamp: Instant = Instant.now(),
    val path: String? = null
)
