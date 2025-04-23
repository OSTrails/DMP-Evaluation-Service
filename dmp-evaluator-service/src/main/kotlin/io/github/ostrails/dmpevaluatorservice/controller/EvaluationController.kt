package io.github.ostrails.dmpevaluatorservice.controller

import io.github.ostrails.dmpevaluatorservice.database.model.Evaluation
import io.github.ostrails.dmpevaluatorservice.model.EvaluationReportResponse
import io.github.ostrails.dmpevaluatorservice.model.EvaluationRequest
import io.github.ostrails.dmpevaluatorservice.model.EvaluationResult
import io.github.ostrails.dmpevaluatorservice.service.EvaluationService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.http.codec.multipart.FilePart

@RestController
@RequestMapping("/api/evaluations")
class EvaluationController(
    private val evaluationService: EvaluationService,
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
        //        System.out.println(evaluations)
        return ResponseEntity.ok(evaluationreport)
    }



    /*
    Example in how to call the benchmark with a file
    curl -X POST http://localhost:8080/api/evaluation/benchmark \
  -F 'file=@/path/to/my-dmp.json' \
  -F 'dimension=completeness' \
  -F 'tests=metadataCoverage' \
  -F 'tests=structure'

    * */

    @PostMapping("/benchmark", consumes = ["multipart/form-data"])
    suspend fun runBenchmark(
        @RequestPart("maDMP") maDMP: FilePart
    ): ResponseEntity<kotlinx.serialization.json.JsonObject> {
        val jsonResult = evaluationService.gatewayEvaluationService(maDMP)
        return ResponseEntity.ok(jsonResult)
    }

}