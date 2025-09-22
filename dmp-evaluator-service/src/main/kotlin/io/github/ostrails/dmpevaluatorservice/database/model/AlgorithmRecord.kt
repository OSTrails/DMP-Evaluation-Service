package io.github.ostrails.dmpevaluatorservice.database.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "algorithms")
data class AlgorithmRecord (
    @Id val algorithmId: String? = null,
    val title: String = "",
    val description: String= "",
    val version: String = "",
    val hasAssociatedMetric: List<String>? = emptyList(),
    val endpointURL: String?,
    val keyword: String? = null,
    val abbreviation: String? = null,
    val type: String? = null,
    val theme: String? = null,
    val versionNotes: String? = null,
    val status: String? = null,
    val creator: List<String> = emptyList(),
)