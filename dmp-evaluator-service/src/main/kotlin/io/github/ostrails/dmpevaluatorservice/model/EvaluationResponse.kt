package io.github.ostrails.dmpevaluatorservice.model

import io.github.ostrails.dmpevaluatorservice.database.model.Evaluation
import io.github.ostrails.dmpevaluatorservice.database.model.Guidance
import java.time.Instant

data class EvaluationResponse(
    val identifier: String?,
    val title: String,
    val description: String,
    val value: ResultTestEnum,
    val generatedAtTime: Instant,
    val reportId: String?,
    val log: String,
    val affectedElements: List<String>?,
    val completion: Int?,
    val assessmentTarget: String?,
    val wasGeneratedBy: String?,
    val outputFromTest: String?,
    val guidance: Guidance?,
)

fun Evaluation.toResponse() = EvaluationResponse(
    identifier = evaluationId,
    title = title,
    description = details,
    value = result,
    generatedAtTime = timestamp,
    reportId = reportId,
    log = log,
    affectedElements = affectedElements,
    completion = completion,
    assessmentTarget = assessmentTarget,
    wasGeneratedBy = wasGeneratedBy,
    outputFromTest = outputFromTest,
    guidance = guidance,
)
