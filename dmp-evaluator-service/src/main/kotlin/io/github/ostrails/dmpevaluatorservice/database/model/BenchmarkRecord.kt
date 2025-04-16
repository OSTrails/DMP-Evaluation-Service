package io.github.ostrails.dmpevaluatorservice.database.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document


@Document(collection = "benchmarks")
data class BenchmarkRecord(
    @Id val benchmarkId: String? = null,
    val title: String,
    val description: String,
    val version: String,
    val metrics: List<String>? = emptyList()
)