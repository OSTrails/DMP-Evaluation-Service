package io.github.ostrails.dmpevaluatorservice.controller

import io.github.ostrails.dmpevaluatorservice.model.EvaluationResult
import io.github.ostrails.dmpevaluatorservice.service.EvaluationService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/evaluations")
class EvaluationController(private val evaluationService: EvaluationService) {

    @PostMapping
    fun evaluate(@RequestBody requestBody: Map<String, Any>): ResponseEntity<EvaluationResult> {
        val result = evaluationService.evaluate(requestBody)
        return ResponseEntity.ok(result)
    }

    @GetMapping("/{ReportId}")
    fun getReport(@PathVariable ReportId: String): ResponseEntity<List<EvaluationResult>> {
        val evaluations = evaluationService.getEvaluations(ReportId)
        return ResponseEntity.ok(evaluations)
    }
}