package io.github.ostrails.dmpevaluatorservice.model.requests

data class TestUpdateRequest(
    val title: String?,
    val description: String?,
    val license: String?,
    val version: String?,
    val endpointURL: String? = "",
    val endpointDescription: String? = null,
    val keyword: String? = null,
    val abbreviation: String? = null,
    val repository: String? = null,
    val type: String? = null,
    val theme: String? = null,
    val versionNotes: String? = null,
    val status: String? = null,
    val isApplicableFor: String? = null,
    val supportedBy: String? = null,
)

data class TestAddMetricRequest(
    val metricImplemented: String,
    val evaluator: String?,
    val functionEvaluator: String?,
)
