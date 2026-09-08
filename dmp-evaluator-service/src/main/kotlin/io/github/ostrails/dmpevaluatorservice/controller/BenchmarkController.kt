package io.github.ostrails.dmpevaluatorservice.controller

import io.github.ostrails.dmpevaluatorservice.auth.isAdmin
import io.github.ostrails.dmpevaluatorservice.model.benchmark.BenchmarkCreateRequest
import io.github.ostrails.dmpevaluatorservice.model.benchmark.BenchmarkJsonLD
import io.github.ostrails.dmpevaluatorservice.model.benchmark.BenchmarkResponse
import io.github.ostrails.dmpevaluatorservice.model.benchmark.BenchmarkUpdateRequest
import io.github.ostrails.dmpevaluatorservice.model.benchmark.toResponse
import io.github.ostrails.dmpevaluatorservice.model.metric.metricsListsIDs
import io.github.ostrails.dmpevaluatorservice.service.BenchmarService
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@Tag(name = "Benchmark APIs", description = "Manage benchmarks and associated metrics")
@RestController
@RequestMapping("/benchmarks")
class BenchmarkController(
    val benchMarkService: BenchmarService
) {

    @Operation(summary = "Create a benchmark", security = [SecurityRequirement(name = "bearerAuth")])
    @PostMapping
    suspend fun create(
        @RequestBody benchmarkBody: BenchmarkCreateRequest,
        authentication: Authentication
    ): ResponseEntity<BenchmarkResponse> {
        val result = benchMarkService.createBenchmark(benchmarkBody, authentication.name)
        return ResponseEntity.ok(result.toResponse())
    }

    @Operation(summary = "Add a metric to a benchmark", security = [SecurityRequirement(name = "bearerAuth")])
    @PostMapping("/metrics/{benchmarkId}")
    suspend fun addMetric(
        @PathVariable benchmarkId: String,
        @RequestBody newMetrics: metricsListsIDs,
        authentication: Authentication
    ): ResponseEntity<BenchmarkResponse> {
        val benchmark = benchMarkService.addMetric(benchmarkId, newMetrics.metrics, authentication.name, authentication.isAdmin())
        return ResponseEntity.ok(benchmark.toResponse())
    }

    @Operation(summary = "Edit a benchmark", security = [SecurityRequirement(name = "bearerAuth")])
    @PutMapping("/{benchmarkId}")
    suspend fun updateBenchmark(
        @PathVariable benchmarkId: String,
        @RequestBody benchmark: BenchmarkUpdateRequest,
        authentication: Authentication
    ): ResponseEntity<BenchmarkResponse> {
        val benchmarkResult = benchMarkService.updateBenchmark(benchmarkId, benchmark, authentication.name, authentication.isAdmin())
        return ResponseEntity.ok(benchmarkResult.toResponse())
    }

    @Operation(summary = "Get all the benchmarks")
    @GetMapping("/list", produces = ["application/json"])
    suspend fun getBenchmarks(): ResponseEntity<List<BenchmarkResponse>> {
        return ResponseEntity.ok(benchMarkService.getBenchmarks().map { it.toResponse() })
    }

    @Operation(summary = "Get all the benchmarks ids")
    @GetMapping
    suspend fun getBenchmarksIds(): ResponseEntity<List<String>> {
        return ResponseEntity.ok(benchMarkService.getBenchmarksIds())
    }

    @Operation(summary = "Get all the benchmarks in json-ld format")
    @GetMapping("/list/jsonLD")
    suspend fun getBenchmarksJsonLD(): ResponseEntity<List<BenchmarkJsonLD>> {
        val benchmarks = benchMarkService.getBenchmarks()
        return ResponseEntity.ok(benchmarks.map { benchMarkService.toJsonLD(it) })
    }

    @Operation(summary = "Delete a specific benchmark", security = [SecurityRequirement(name = "bearerAuth")])
    @DeleteMapping("/{benchmarkId}")
    suspend fun deleteBenchmark(@PathVariable benchmarkId: String): ResponseEntity<String> {
        benchMarkService.deleteBenchmark(benchmarkId)
        return ResponseEntity.ok(benchmarkId)
    }

    @Operation(summary = "Get a specific benchmark")
    @GetMapping("/info/{benchmarkId}")
    suspend fun getBenchmark(@PathVariable benchmarkId: String): ResponseEntity<BenchmarkResponse> {
        return ResponseEntity.ok(benchMarkService.getBenchmarkDetail(benchmarkId).toResponse())
    }

    @Operation(summary = "Get a list of specific benchmarks")
    @PostMapping("/list/filter")
    suspend fun getBenchmarkById(
        @RequestBody benchmarkIds: List<String>
    ): ResponseEntity<List<BenchmarkResponse>> {
        return ResponseEntity.ok(benchMarkService.getBenchmarskDetail(benchmarkIds).map { it.toResponse() })
    }

    @Operation(summary = "Get a specific benchmark in json-ld")
    @GetMapping("/{benchmarkId}")
    suspend fun getBenchmarkJsonLD(@PathVariable benchmarkId: String): ResponseEntity<BenchmarkJsonLD> {
        return ResponseEntity.ok(benchMarkService.getBenchmarkDetailJsonLD(benchmarkId))
    }

    @Operation(summary = "Get a specific benchmark in json-ld by request param")
    @GetMapping("/")
    suspend fun getBenchmarkJsonLDRequestParam(@RequestParam("benchmarkId") benchmarkId: String): ResponseEntity<BenchmarkJsonLD> {
        return ResponseEntity.ok(benchMarkService.getBenchmarkDetailJsonLD(benchmarkId))
    }

    @Operation(summary = "Delete a specific metric from a benchmark", security = [SecurityRequirement(name = "bearerAuth")])
    @PostMapping("/{benchmarkId}/delete/metric")
    suspend fun deleteMetric(
        @PathVariable benchmarkId: String,
        @RequestBody metrics: List<String>,
        authentication: Authentication
    ): ResponseEntity<BenchmarkResponse> {
        val result = benchMarkService.deleteMetric(benchmarkId, metrics, authentication.name, authentication.isAdmin())
        return ResponseEntity.ok(result.toResponse())
    }
}
