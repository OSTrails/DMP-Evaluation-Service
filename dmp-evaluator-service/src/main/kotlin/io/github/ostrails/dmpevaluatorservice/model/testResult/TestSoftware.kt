package io.github.ostrails.dmpevaluatorservice.model.testResult

import javax.swing.border.TitledBorder

data class TestSoftware(
    val id: String,
    val titled: String,
    val description: String,
    val version: String,
    val license: String,
)
