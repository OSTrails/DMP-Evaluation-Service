package io.github.ostrails.dmpevaluatorservice.model

data class EvaluationRequest(
    val reportId: String? = null,
    // Is needed to map the maDMP into a class and the evaluationParams into another
    val maDMP: Map<String, Any>,
    val evaluationParams: List<String>,
)
