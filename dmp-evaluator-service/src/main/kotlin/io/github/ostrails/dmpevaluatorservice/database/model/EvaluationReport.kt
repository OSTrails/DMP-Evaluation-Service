package io.github.ostrails.dmpevaluatorservice.database.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "evaluation_reports")
data class EvaluationReport(
    @Id var reportId: String? = null,
    val dmpId: String,
    val generatedAt: Instant = Instant.now(),
    val evaluations: List<String> = listOf()
)
