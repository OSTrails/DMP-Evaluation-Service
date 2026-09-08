 package io.github.ostrails.dmpevaluatorservice.model.metric

data class MetricCreateRequest(
    val title: String,
    val description: String,
    val version: String,
    val keyword: String? = null,
    val abbreviation: String? = null,
    val landingPage: String? = null,
    val theme: String? = null,
    val status: String? = null,
    val isApplicableFor: String? = null,
    val supportedBy: String? = null,
    val license: String? = null,
    val inDimension: String? = null,
)
