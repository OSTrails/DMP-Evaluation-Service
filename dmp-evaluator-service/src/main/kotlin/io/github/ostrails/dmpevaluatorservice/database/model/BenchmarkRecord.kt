package io.github.ostrails.dmpevaluatorservice.database.model

import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document


@Document(collection = "benchmarks")
data class BenchmarkRecord(
    @Id
    @JsonProperty("identifier")
    val benchmarkId: String? = null,
    val title: String = "",
    val description: String= "",
    val version: String = "",
    val hasAssociatedMetric: List<String>? = emptyList(),
    @JsonProperty("scoringFunction")
    val algorithms: List<String>? = emptyList(),
    val keyword: String? = null ,
    val abbreviation: String? = null,
    val landingPage: String? = null,
    val theme: String? = null,
    val status: String? = null,
    val creator: List<String> = emptyList(),
    val license: String? = null,
)