package io.github.ostrails.dmpevaluatorservice.model.metric


data class MetricUpdateRequest(
    val title: String? = "",
    val description: String? = "",
    val version: String? = "",
    val keyword: String? = "",
    val abbreviation: String? = null,
    val landingPage: String? = null,
    val theme: String? = null,
    val status: String? = null,
    val isApplicableFor: String? = null, //Indicates the concept or information is applicable for specified context
    val supportedBy: String? = null,

)


