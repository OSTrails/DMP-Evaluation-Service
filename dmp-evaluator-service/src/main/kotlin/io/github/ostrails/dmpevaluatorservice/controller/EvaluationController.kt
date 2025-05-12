package io.github.ostrails.dmpevaluatorservice.controller

import io.github.ostrails.dmpevaluatorservice.database.model.Evaluation
import io.github.ostrails.dmpevaluatorservice.model.EvaluationReportResponse
import io.github.ostrails.dmpevaluatorservice.model.EvaluationRequest
import io.github.ostrails.dmpevaluatorservice.model.EvaluationResult
import io.github.ostrails.dmpevaluatorservice.service.EvaluationManagerService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.http.codec.multipart.FilePart

@Tag(name = "Evaluation APIs", description = "Manage evaluations for the maDMPs")
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
        val evaluationreport = evaluationManagerService.getFullReport(reportid)
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
        @RequestPart("benchmark") benchmark: String,
        @RequestPart(required = false) reportId: String?
    ): ResponseEntity<List<Evaluation>>{
        val filename = maDMP.filename().lowercase()
        println("filename ---------------- $filename")
        val jsonResult = evaluationManagerService.gatewayBenchmarkEvaluationService(maDMP, benchmark, reportId)
        return ResponseEntity.ok(jsonResult)
    }

    @PostMapping("/test", consumes = ["multipart/form-data"])
    suspend fun runTest(
        @RequestPart("maDMP") maDMP: FilePart,
        @RequestPart("test") test: String,
        @RequestPart(required = false) reportId: String?
    ): ResponseEntity<Evaluation>{
        val jsonResult = evaluationManagerService.gatewayTestsEvaluationService(maDMP, test, reportId)
        return ResponseEntity.ok(jsonResult)
    }

    @PostMapping("/mappingRDF", consumes = ["multipart/form-data"])
    suspend fun runMapping(
        @RequestPart("maDMP") maDMP: FilePart,
    ): ResponseEntity<Any>{
        val result = evaluationManagerService.mapToRDF(maDMP)
        return ResponseEntity.ok().body(result)
    }
}