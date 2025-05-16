package io.github.ostrails.dmpevaluatorservice.database.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "tests")
data class TestRecord(
    @Id var id: String? = null,
    val title: String,
    val description: String,
    val license: String,
    val version: String,
    val endpointURL: String? = "http://localhost:8080/api/evaluations/benchmark",
    val endpointDescription: String? = null,
    val keyword: String? = null,
    val abbreviation: String? = null,
    val repository: String? = null,
    val type: String? = null,
    val theme: String? = null,
    val versionNotes: String? = null,
    val isApplicableFor: String? = null,
    val supportedBy: String? = null,
    val metricImplemented: String?,
    val evaluator: String?,
    val functionEvaluator: String?,

    )

