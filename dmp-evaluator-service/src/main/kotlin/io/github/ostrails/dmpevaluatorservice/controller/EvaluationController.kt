package io.github.ostrails.dmpevaluatorservice.controller

import io.github.ostrails.dmpevaluatorservice.database.model.Evaluation
import io.github.ostrails.dmpevaluatorservice.database.model.TestRecord
import io.github.ostrails.dmpevaluatorservice.model.EvaluationReportResponse
import io.github.ostrails.dmpevaluatorservice.model.EvaluationRequest
import io.github.ostrails.dmpevaluatorservice.model.EvaluationResult
import io.github.ostrails.dmpevaluatorservice.service.EvaluationManagerService
import io.github.ostrails.dmpevaluatorservice.service.EvaluationService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.http.codec.multipart.FilePart

@RestController
@RequestMapping("/api/evaluations")
class EvaluationController(
    private val evaluationManagerService: EvaluationManagerService,
) {

    @PostMapping
    suspend fun evaluate(@RequestBody requestBody: EvaluationRequest): ResponseEntity<EvaluationResult> {
        val result = evaluationManagerService.generateEvaluations(requestBody)
        return ResponseEntity.ok(result)
    }

    @GetMapping()
    suspend fun getAllEvaluations(): ResponseEntity<List<Evaluation>> {
        val evaluations = evaluationManagerService.getEvaluations()
        return ResponseEntity.ok(evaluations)
    }

    @GetMapping("/report/{reportid}/full")
    suspend fun getReport(@PathVariable reportid: String): ResponseEntity<EvaluationReportResponse> {
        val evaluationreport = evaluationManagerService.getFullreport(reportid)
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
        @RequestPart("maDMP") maDMP: FilePart,
        @RequestPart("benchmark") benchmark: String
    ): ResponseEntity<List<TestRecord>>{
            //kotlinx.serialization.json.JsonObject>
        println("Received benchmark. $benchmark")
        val jsonResult = evaluationManagerService.gatewayEvaluationService(maDMP, benchmark)
        return ResponseEntity.ok(jsonResult)
    }
}