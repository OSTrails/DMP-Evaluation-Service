package io.github.ostrails.dmpevaluatorservice.model.testResult

import java.time.Instant

data class TestResult(
    val name: String,
    val title: String,
    val description: String,
    val license: String,
    val value: String,
    val log: String,
    val generated: Instant = Instant.now(),
    )
