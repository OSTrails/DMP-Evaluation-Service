package io.github.ostrails.dmpevaluatorservice.database.model

import io.github.ostrails.dmpevaluatorservice.model.ResultTestEnum
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "evaluations")
data class Evaluation(
    @Id val evaluationId:  String? = null,
    val result: ResultTestEnum,
    val details: String,
    val timestamp: Instant = Instant.now(),
    val reportId: String?  // Links back to EvaluationReport
)
