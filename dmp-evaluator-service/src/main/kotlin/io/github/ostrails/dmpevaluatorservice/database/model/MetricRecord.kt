package io.github.ostrails.dmpevaluatorservice.database.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "metrics")
data class MetricRecord(
    @Id val id: String? = null,
    val title: String,
    val description: String,
    val version: String,
    val testAssociated: List<String>?
)
