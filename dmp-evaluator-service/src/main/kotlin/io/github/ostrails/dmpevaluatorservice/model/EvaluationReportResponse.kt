package io.github.ostrails.dmpevaluatorservice.model

data class EvaluationReportResponse(
    val report: EvaluationReportInfo,
    val evaluations: List<EvaluationResponse>,
)
