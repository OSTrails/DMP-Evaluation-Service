package io.github.ostrails.dmpevaluatorservice.model.requests

data class TestCreateRequest(
    val title: String,
    val description: String,
    val license: String,
    val version: String,
    val endpointDescription: String? = null,
    val keyword: String? = null,
    val abbreviation: String? = null,
    val type: String? = null,
    val theme: String? = null,
    val versionNotes: String? = null,
    val status: String? = null,
    val isApplicableFor: String? = null,
    val supportedBy: String? = null,
    val metricImplemented: String? = null,
    val evaluator: String? = null,
    val functionEvaluator: String? = null,
)
