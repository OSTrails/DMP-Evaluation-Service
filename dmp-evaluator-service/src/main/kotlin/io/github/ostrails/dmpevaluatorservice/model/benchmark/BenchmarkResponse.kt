package io.github.ostrails.dmpevaluatorservice.model.benchmark

import io.github.ostrails.dmpevaluatorservice.database.model.BenchmarkRecord

data class BenchmarkResponse(
    val identifier: String?,
    val title: String,
    val description: String,
    val version: String,
    val hasAssociatedMetric: List<String>?,
    val scoringFunction: List<String>?,
    val keyword: String?,
    val abbreviation: String?,
    val landingPage: String?,
    val theme: String?,
    val status: String?,
    val creator: List<String>,
    val license: String?,
    val createdBy: String?,
)

fun BenchmarkRecord.toResponse() = BenchmarkResponse(
    identifier = benchmarkId,
    title = title,
    description = description,
    version = version,
    hasAssociatedMetric = hasAssociatedMetric,
    scoringFunction = algorithms,
    keyword = keyword,
    abbreviation = abbreviation,
    landingPage = landingPage,
    theme = theme,
    status = status,
    creator = creator,
    license = license,
    createdBy = createdBy,
)
