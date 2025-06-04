package io.github.ostrails.dmpevaluatorservice.model.benchmark


data class BenchmarkUpdateRequest(
    val title: String = "",
    val description: String= "",
    val version: String = "",
    val keyword: String? = null,
    val abbreviation: String? = null,
    val landingPage: String? = null,
    val theme: String? = null,
    val status: String? = null,
    val creator: List<String> = emptyList(),
    )
