package io.github.ostrails.dmpevaluatorservice.model

import io.github.ostrails.dmpevaluatorservice.database.model.BenchmarkRecord

data class PluginInfo(
    val pluginId: String,
    val description: String,
    val benchmarks: List<BenchmarkRecord>
)
