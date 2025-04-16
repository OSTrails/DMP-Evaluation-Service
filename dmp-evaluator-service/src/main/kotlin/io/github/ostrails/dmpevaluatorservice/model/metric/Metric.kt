package io.github.ostrails.dmpevaluatorservice.model.metric

data class Metric(
    val id: String,
    val title: String,
    val description: String,
    val version: String,
    val reference: String,
    val testAssociated: List<String>
)
