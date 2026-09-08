package io.github.ostrails.dmpevaluatorservice.model.metric

import io.github.ostrails.dmpevaluatorservice.database.model.MetricRecord

data class MetricResponse(
    val identifier: String?,
    val title: String,
    val description: String,
    val version: String,
    val testAssociated: List<String>?,
    val keyword: String?,
    val abbreviation: String?,
    val landingPage: String?,
    val theme: String?,
    val status: String?,
    val isApplicableFor: String?,
    val supportedBy: String?,
    val hasBenchmark: List<String>?,
    val license: String?,
    val inDimension: String?,
    val createdBy: String?,
)

fun MetricRecord.toResponse() = MetricResponse(
    identifier = id,
    title = title,
    description = description,
    version = version,
    testAssociated = testAssociated,
    keyword = keyword,
    abbreviation = abbreviation,
    landingPage = landingPage,
    theme = theme,
    status = status,
    isApplicableFor = isApplicableFor,
    supportedBy = supportedBy,
    hasBenchmark = hasBenchmark,
    license = license,
    inDimension = inDimension,
    createdBy = createdBy,
)
