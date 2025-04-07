package io.github.ostrails.dmpevaluatorservice.model

import io.github.ostrails.dmpevaluatorservice.database.model.Evaluation
import io.github.ostrails.dmpevaluatorservice.database.model.EvaluationReport

data class EvaluationReportResponse(
    val report: EvaluationReport,
    val evaluations: List<Evaluation>,
)
