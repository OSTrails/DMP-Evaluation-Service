package io.github.ostrails.dmpevaluatorservice.model

import java.time.Instant

data class EvaluationResult(
    val reportId: String,
    val evaluations: List<EvaluationResponse>,
    val timestamp: Instant = Instant.now(),
)
