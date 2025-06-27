package io.github.ostrails.dmpevaluatorservice.controller

import io.github.ostrails.dmpevaluatorservice.database.model.Evaluation
import io.github.ostrails.dmpevaluatorservice.model.EvaluationReportResponse
import io.github.ostrails.dmpevaluatorservice.model.EvaluationRequest
import io.github.ostrails.dmpevaluatorservice.model.EvaluationResult
import io.github.ostrails.dmpevaluatorservice.model.testResult.TestResultJsonLD
import io.github.ostrails.dmpevaluatorservice.service.EvaluationManagerService
import io.github.ostrails.dmpevaluatorservice.service.EvaluationService
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.http.codec.multipart.FilePart

@Tag(name = "Evaluation APIs", description = "Manage evaluations for the maDMPs")
@RestController
@RequestMapping("/api/evaluations")
class EvaluationController(
    private val evaluationManagerService: EvaluationManagerService,
    private val evaluationService: EvaluationService,
) {

    @Operation(
        summary = "Create a generic evaluation",
        description = "Create a evaluation and return a evaluation record "
    )
    @PostMapping
    suspend fun evaluate(@RequestBody requestBody: EvaluationRequest): ResponseEntity<EvaluationResult> {
        val result = evaluationManagerService.generateEvaluations(requestBody)
        return ResponseEntity.ok(result)
    }

    @Operation(
        summary = "List all evaluations in the system",
        description = "List all the evaluations in the system"
    )
    @GetMapping()
    suspend fun getAllEvaluations(): ResponseEntity<List<Evaluation>> {
        val evaluations = evaluationManagerService.getEvaluations()
        return ResponseEntity.ok(evaluations)
    }

    @Operation(
        summary = "List all the evaluations of a specific report",
        description = "List of evaluations results"
    )
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


    @Operation(
        summary = "List all the evaluations of a specific report",
        description = "List of evaluations results"
    )
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


    @Operation(
        summary = "Run a benchmark evaluaion and return the list of results in json-ld",
        description = "Run all the tests that are associated wiht a benchmark"
    )
    @PostMapping("/benchmark/json-ld", consumes = ["multipart/form-data"])
    suspend fun runBenchmarkJsonLD(
        @RequestPart("maDMP") maDMP: FilePart,
        @RequestPart("benchmark") benchmark: String,
        @RequestPart(required = false) reportId: String?
    ): ResponseEntity<List<TestResultJsonLD>>{
        val filename = maDMP.filename().lowercase()
        val jsonResult = evaluationManagerService.gatewayBenchmarkEvaluationService(maDMP, benchmark, reportId)
        val jsonLDResult: List<TestResultJsonLD> = jsonResult.map { evaluationService.buildEvalutionResultJsonLD(it) }
        return ResponseEntity.ok(jsonLDResult)
    }

    @Operation(
        summary = "Run a specific test",
        description = "Return the result of a specific test evaluation"
    )
    @PostMapping("/test", consumes = ["multipart/form-data"])
    suspend fun runTest(
        @RequestPart("maDMP") maDMP: FilePart,
        @RequestPart("test") test: String,
        @RequestPart(required = false) reportId: String?
    ): ResponseEntity<Evaluation>{
        val jsonResult = evaluationManagerService.gatewayTestsEvaluationService(maDMP, test, reportId)
        return ResponseEntity.ok(jsonResult)
    }

    @Operation(
        summary = "Run the evaluation for a specific test",
        description = "Return the evaluation for a specific test in json-ld format"
    )
    @PostMapping("/test/JsonLD", consumes = ["multipart/form-data"])
    suspend fun runTestJsonLD(
        @RequestPart("maDMP") maDMP: FilePart,
        @RequestPart("test") test: String,
        @RequestPart(required = false) reportId: String?
    ): ResponseEntity<TestResultJsonLD>{
        val jsonResult = evaluationManagerService.gatewayTestsEvaluationService(maDMP, test, reportId)
        val jsonLDResult = jsonResult?.let { evaluationService.buildEvalutionResultJsonLD(it) }

        return ResponseEntity.ok(jsonLDResult)
    }

    @Operation(
        summary = "Mapping a maDMP into a RDF format"
    )
    @PostMapping("/mappingRDF", consumes = ["multipart/form-data"])
    suspend fun runMapping(
        @RequestPart("maDMP") maDMP: FilePart,
    ): ResponseEntity<Any>{
        val result = evaluationManagerService.mapToRDF(maDMP)
        return ResponseEntity.ok().body(result)
    }
}