package io.github.ostrails.dmpevaluatorservice.service

import kotlinx.serialization.json.*
import io.github.ostrails.dmpevaluatorservice.database.model.Evaluation
import io.github.ostrails.dmpevaluatorservice.database.model.EvaluationReport
import io.github.ostrails.dmpevaluatorservice.database.repository.EvaluationReportRepository
import io.github.ostrails.dmpevaluatorservice.database.repository.EvaluationResultRepository
import io.github.ostrails.dmpevaluatorservice.exceptionHandler.ResourceNotFoundException
import io.github.ostrails.dmpevaluatorservice.model.EvaluationReportResponse
import io.github.ostrails.dmpevaluatorservice.model.EvaluationRequest
import io.github.ostrails.dmpevaluatorservice.model.EvaluationResult
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service

@Service
class EvaluationService(
    private val resultEvaluationResultRepository: EvaluationResultRepository,
    private val evaluationReportRepository: EvaluationReportRepository
) {




}
