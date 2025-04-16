package io.github.ostrails.dmpevaluatorservice.model.requests

data class TestUpdateRequest(
    val title: String?,
    val description: String?,
    val license: String?,
    val version: String?,
    val metricImplemented: String?,
    val evaluator: String?,
    val functionEvaluator: String?,
)
