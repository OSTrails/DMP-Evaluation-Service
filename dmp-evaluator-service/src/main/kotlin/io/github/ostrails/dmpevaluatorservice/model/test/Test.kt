package io.github.ostrails.dmpevaluatorservice.model.test

class Test (
    val id: String? = null,
    val title: String,
    val description: String,
    val license: String,
    val version: String,
    val metricImplemented: String?,
    val evaluator: String?,
    val functionEvaluator: String?,
)