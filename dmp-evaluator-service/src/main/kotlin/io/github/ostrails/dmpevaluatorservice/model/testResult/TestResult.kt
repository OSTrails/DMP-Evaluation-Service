package io.github.ostrails.dmpevaluatorservice.model.testResult

import java.time.Instant

data class TestResult(
    val id: String? = null,
    val title: String,
    val description: String,
    val license: String,
    val status: String,
    val log: String,
    val generated: Instant = Instant.now(),
    )
