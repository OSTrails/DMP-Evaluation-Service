package io.github.ostrails.dmpevaluatorservice.database.model

import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "evaluation_reports")
data class EvaluationReport(
    @Id
    @JsonProperty("identifier")
    var reportId: String? = null,
    val title: String = "",
    val assessmentTarget: String? = null,
    @JsonProperty("generatedAtTime")
    val generatedAt: Instant = Instant.now(),
    @JsonProperty("hadMember")
    val evaluations: List<String> = listOf()
)
