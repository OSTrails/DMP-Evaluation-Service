package io.github.ostrails.dmpevaluatorservice.model.benchmark


data class BenchmarkUpdateRequest(
    val title: String? = null,
    val description: String? = null,
    val version: String? = null,
    val keyword: String? = null,
    val abbreviation: String? = null,
    val landingPage: String? = null,
    val theme: String? = null,
    val status: String? = null,
    val creator: List<String>? = null,
    )
