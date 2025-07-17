package io.github.ostrails.dmpevaluatorservice.database.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "metrics")
data class MetricRecord(
    @Id val id: String? = null,
    val title: String,
    val description: String,
    val version: String,
    val testAssociated: List<String>?,
    val keyword: String? = null,
    val abbreviation: String? = null,
    val landingPage: String? = null,
    val theme: String? = null,
    val status: String? = null,
    val isApplicableFor: String? = null, //Indicates the concept or information is applicable for specified context
    val supportedBy: String? = null,
    val hasBenchmark: List<String>?
    )
