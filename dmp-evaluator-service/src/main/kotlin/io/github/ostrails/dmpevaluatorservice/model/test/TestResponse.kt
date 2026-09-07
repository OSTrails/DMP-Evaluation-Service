package io.github.ostrails.dmpevaluatorservice.model.test

import io.github.ostrails.dmpevaluatorservice.database.model.TestRecord

data class TestResponse(
    val identifier: String?,
    val title: String,
    val description: String,
    val license: String,
    val version: String,
    val endpointURL: String?,
    val endpointDescription: String?,
    val keyword: String?,
    val abbreviation: String?,
    val repository: String?,
    val type: String?,
    val theme: String?,
    val versionNotes: String?,
    val status: String?,
    val isApplicableFor: String?,
    val supportedBy: String?,
    val metricImplemented: String?,
    val evaluator: String?,
    val functionEvaluator: String?,
    val createdBy: String?,
)

fun TestRecord.toResponse() = TestResponse(
    identifier = id,
    title = title,
    description = description,
    license = license,
    version = version,
    endpointURL = endpointURL,
    endpointDescription = endpointDescription,
    keyword = keyword,
    abbreviation = abbreviation,
    repository = repository,
    type = type,
    theme = theme,
    versionNotes = versionNotes,
    status = status,
    isApplicableFor = isApplicableFor,
    supportedBy = supportedBy,
    metricImplemented = metricImplemented,
    evaluator = evaluator,
    functionEvaluator = functionEvaluator,
    createdBy = createdBy,
)
