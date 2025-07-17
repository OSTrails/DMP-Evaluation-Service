package io.github.ostrails.dmpevaluatorservice.model

import io.github.ostrails.dmpevaluatorservice.database.model.TestRecord

data class PluginInfo(
    val pluginId: String,
    val description: String,
    val tests: List<TestRecord>?
)
