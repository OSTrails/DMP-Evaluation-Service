package io.github.ostrails.dmpevaluatorservice.model.metric


data class MetricUpdateRequest(
    val title: String? = null,
    val description: String? = null,
    val version: String? = null,
    val keyword: String? = null,
    val abbreviation: String? = null,
    val landingPage: String? = null,
    val theme: String? = null,
    val status: String? = null,
    val isApplicableFor: String? = null, //Indicates the concept or information is applicable for specified context
    val supportedBy: String? = null,

)


