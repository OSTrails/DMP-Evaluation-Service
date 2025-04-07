package io.github.ostrails.dmpevaluatorservice.controller

import io.github.ostrails.dmpevaluatorservice.database.model.Evaluation
import io.github.ostrails.dmpevaluatorservice.database.repository.EvaluationReportRepository
import io.github.ostrails.dmpevaluatorservice.model.EvaluationReportResponse
import io.github.ostrails.dmpevaluatorservice.model.EvaluationRequest
import io.github.ostrails.dmpevaluatorservice.model.EvaluationResult
import io.github.ostrails.dmpevaluatorservice.service.EvaluationService
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/evaluations")
class EvaluationController(
    private val evaluationService: EvaluationService,
    private val evaluationReportRepository: EvaluationReportRepository
) {

    @PostMapping
    suspend fun evaluate(@RequestBody requestBody: EvaluationRequest): ResponseEntity<EvaluationResult> {
        val result = evaluationService.generateEvaluations(requestBody)
        return ResponseEntity.ok(result)
    }

    @GetMapping()
    suspend fun getAllEvaluations(): ResponseEntity<List<Evaluation>> {
        val evaluations = evaluationService.getEvaluations()
        return ResponseEntity.ok(evaluations)
    }

    @GetMapping("/report/{reportid}/full")
    suspend fun getReport(@PathVariable reportid: String): ResponseEntity<EvaluationReportResponse> {
        val evaluationreport = evaluationService.getFullreport(reportid)
        return ResponseEntity.ok(evaluationreport)
    }


//    @GetMapping( "/{reportId}")
//    fun getReport(@PathVariable reportId: String): ResponseEntity<List<EvaluationResult>> {
//        val evaluations = evaluationService.getEvaluation(reportId)
//        System.out.println(evaluations)
//        return ResponseEntity.ok(evaluations)
//    }
}