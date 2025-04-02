package io.github.ostrails.dmpevaluatorservice.controller

import io.github.ostrails.dmpevaluatorservice.database.model.Evaluation
import io.github.ostrails.dmpevaluatorservice.model.EvaluationResult
import io.github.ostrails.dmpevaluatorservice.service.EvaluationService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/evaluations")
class EvaluationController(private val evaluationService: EvaluationService) {

    @PostMapping
    fun evaluate(@RequestBody requestBody: Map<String, Any>): ResponseEntity<Evaluation> {
        val result = evaluationService.evaluate(requestBody)
        return ResponseEntity.ok(result)
    }

//    @GetMapping()
//    fun getAllEvaluations(): ResponseEntity<List<Evaluation>> {
//        val evaluations = evaluationService.getEvaluations()
//        return ResponseEntity.ok(evaluations)
//    }

    @GetMapping( "/{reportId}")
    fun getReport(@PathVariable reportId: String): ResponseEntity<List<EvaluationResult>> {
        val evaluations = evaluationService.getEvaluation(reportId)
        System.out.println(evaluations)
        return ResponseEntity.ok(evaluations)
    }
}