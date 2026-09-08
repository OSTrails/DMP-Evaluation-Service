package io.github.ostrails.dmpevaluatorservice.model

import io.github.ostrails.dmpevaluatorservice.database.model.EvaluationReport
import java.time.Instant

data class EvaluationReportInfo(
    val identifier: String?,
    val title: String,
    val assessmentTarget: String?,
    val generatedAtTime: Instant,
    val hadMember: List<String>,
)

fun EvaluationReport.toInfo() = EvaluationReportInfo(
    identifier = reportId,
    title = title,
    assessmentTarget = assessmentTarget,
    generatedAtTime = generatedAt,
    hadMember = evaluations,
)
