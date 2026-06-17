package io.github.ostrails.dmpevaluatorservice.model

import io.github.ostrails.dmpevaluatorservice.database.model.Evaluation
import java.time.Instant

data class EvaluationResult(
    val reportId: String,
    val evaluations: List<Evaluation>,
    val timestamp: Instant = Instant.now(),
)
