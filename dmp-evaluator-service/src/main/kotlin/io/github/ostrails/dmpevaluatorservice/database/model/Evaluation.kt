package io.github.ostrails.dmpevaluatorservice.database.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.github.ostrails.dmpevaluatorservice.model.ResultTestEnum
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

data class GuidanceEntry  (
    val dataset: String,
    val reason: String
)

data class Guidance(
    val summary: String,
    val issues: List<GuidanceEntry> = emptyList()
)

@Document(collection = "evaluations")
data class Evaluation(
    @Id
    @JsonProperty("identifier")
    val evaluationId: String? = null,

    val title: String = "",

    @JsonProperty("description")
    val details: String,

    @JsonProperty("value")
    val result: ResultTestEnum,

    @JsonProperty("generatedAtTime")
    val timestamp: Instant = Instant.now(),

    val reportId: String?,

    val log: String = "",

    val affectedElements: List<String>? = null,

    val completion: Int? = null,

    val assessmentTarget: String? = null,

    val wasGeneratedBy: String? = null,

    val outputFromTest: String? = null,

    val guidance: Guidance? = null,
)
