package io.github.ostrails.dmpevaluatorservice.model

import java.time.LocalDateTime

data class EvaluationResult(
    val dmpId: String,
    val isValid: Boolean,
    val messages: List<String>,
    val timeStamp: LocalDateTime,
)
