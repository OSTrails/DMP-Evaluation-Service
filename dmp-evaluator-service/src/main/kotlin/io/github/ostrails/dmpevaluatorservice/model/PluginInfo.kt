package io.github.ostrails.dmpevaluatorservice.model

data class PluginInfo(
    val pluginId: String,
    val description: String,
    val benchmarks: List<Benchmark>
)
