package io.github.ostrails.dmpevaluatorservice.database.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "tests")
data class TestRecord(
    @Id var id: String? = null,
    val title: String,
    val description: String,
    val license: String,
    val version: String,
    val metricImplemented: String?,
    val evaluator: String?,
    val functionEvaluator: String?,
)

