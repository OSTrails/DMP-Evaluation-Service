package io.github.ostrails.dmpevaluatorservice.model.benchmark

import io.github.ostrails.dmpevaluatorservice.model.metric.Metric


data class Benchmark(
    val title: String,
    val description: String,
    val version: String,
    val metrics: List<Metric>
)

