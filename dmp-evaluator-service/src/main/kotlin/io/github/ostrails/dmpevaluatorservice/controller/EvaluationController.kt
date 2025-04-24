package io.github.ostrails.dmpevaluatorservice.controller

import io.github.ostrails.dmpevaluatorservice.utils.madmp2rdf.ToRDF
import io.github.ostrails.dmpevaluatorservice.database.model.Evaluation
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
        curl -X POST http://localhost:8080/api/evaluations/benchmark \
    -F 'file=@/path/to/my-dmp.json' \
    -F 'dimension=completeness' \
    -F 'tests=metadataCoverage' \
    */

    /*
        The input is a JSON string that is passed to the HTTP POST request body
    */
    @PostMapping("/benchmark/test")
    suspend fun runBenchmark(@RequestBody jsonBody: kotlinx.serialization.json.JsonObject): ResponseEntity<kotlinx.serialization.json.JsonObject> {
        val toRDF = ToRDF()
        toRDF.jsonToRDF(jsonBody.toString())
        return ResponseEntity.ok(kotlinx.serialization.json.JsonObject(mapOf()))
    }
}