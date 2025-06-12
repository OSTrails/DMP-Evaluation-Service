package io.github.ostrails.dmpevaluatorservice.controller

import io.github.ostrails.dmpevaluatorservice.database.model.BenchmarkRecord
import io.github.ostrails.dmpevaluatorservice.model.benchmark.BenchmarkJsonLD
import io.github.ostrails.dmpevaluatorservice.model.benchmark.BenchmarkUpdateRequest
import io.github.ostrails.dmpevaluatorservice.model.metric.metricsListsIDs
import io.github.ostrails.dmpevaluatorservice.service.BenchmarService
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@Tag(name = "Benchmark APIs", description = "Manage benchmarks and associated metrics")
@RestController
@RequestMapping("/api/benchmarks")
class BenchmarkController(
    val benchMarkService: BenchmarService
) {

    @Operation(
        summary = "Create a benchmark",
        description = "Receives the json with the data that describe a benchmark and create a new one and return the benchmark record "
    )
    @PostMapping
    suspend fun create(@RequestBody benchmarkBody: BenchmarkRecord): ResponseEntity<BenchmarkRecord> {
        val result = benchMarkService.createBenchmark(benchmarkBody)
        return ResponseEntity.ok(result)
    }


    @Operation(
        summary = "Add a metric to a benchmark",
        description = "Receives the json a object called metrics that is a list of strings and return the benchmark record"
    )
    @PostMapping("/metrics/{benchmarkId}")
    suspend fun addMetric(@PathVariable benchmarkId: String, @RequestBody newMetrics: metricsListsIDs): ResponseEntity<BenchmarkRecord> {
        val benchmark = benchMarkService.addMetric(benchmarkId, newMetrics.metrics)
        return ResponseEntity.ok(benchmark)
    }

    @Operation(
        summary = "Edit a benchmark",
        description = "Receives the json with the data that describe the new info for the benchmark and return the benchmark record"
    )
    @PostMapping("/edit/{benchmarkId}")
    suspend fun updateBenchmark(@PathVariable benchmarkId: String, @RequestBody benchmark: BenchmarkUpdateRequest): ResponseEntity<BenchmarkRecord> {
        val benchmark = benchMarkService.updateBenchmark(benchmarkId, benchmark)
        return ResponseEntity.ok(benchmark)
    }

    @Operation(
        summary = "Get all the benchmarks",
        description = "Return all the benchmarks supported by the system"
    )
    @GetMapping
    suspend fun getBenchmarks(): ResponseEntity<List<BenchmarkRecord>> {
        val benchmarks = benchMarkService.getBenchmarks()
        return ResponseEntity.ok(benchmarks)
    }


    @Operation(
        summary = "Get all the benchmarks in json-ld format",
        description = "Return all the benchmarks supported by the system in json-ld format"
    )
    @GetMapping("/list/json-ld")
    suspend fun getBenchmarksJsonLD (): ResponseEntity<List<BenchmarkJsonLD>> {
        val benchmarks = benchMarkService.getBenchmarks()
        val result = benchmarks.map { benchmark -> benchMarkService.toJsonLD(benchmark) }
        return ResponseEntity.ok(result)
    }

    @Operation(
        summary = "Delete a specific benchmark",
        description = "Delete a benchmark, and return the id of the benchmark deleted"
    )
    @DeleteMapping("/{benchmarkId}")
    suspend fun deleteBenchmark(@PathVariable benchmarkId: String): ResponseEntity<String> {
        val benchmarkIdRecord = benchMarkService.deleteBenchmark(benchmarkId)
        return ResponseEntity.ok(benchmarkId)
    }


    @Operation(
        summary = "Get a specific benchmark",
        description = "Send a benchmark if and Return a specific benchmark record"
    )
    @GetMapping("/{benchmarkId}")
    suspend fun getBenchmark(@PathVariable benchmarkId: String): ResponseEntity<BenchmarkRecord> {
        val benchmark = benchMarkService.getBenchmarkDetail(benchmarkId)
        return ResponseEntity.ok(benchmark)
    }

    @Operation(
        summary = "Get a list of specific benchmarks",
        description = "Send a list of benchmarks id and Return a specifics benchmarks record"
    )
    @PostMapping("/list/filter")
    suspend fun getBenchmarkById(@RequestBody benchmarkIds: List<String>): ResponseEntity<List<BenchmarkRecord>> {
        val benchmark = benchMarkService.getBenchmarskDetail(benchmarkIds)
        return ResponseEntity.ok(benchmark)
    }

    @Operation(
        summary = "Get a specific benchmark in json-ld",
        description = "Send an id of a benchmark and Return a specific benchmark record"
    )
    @GetMapping("/{benchmarkId}/json-ld")
    suspend fun getBenchmarkJsonLD(@PathVariable benchmarkId: String): ResponseEntity<BenchmarkJsonLD> {
        val benchmarkJsonLD = benchMarkService.getBenchmarkDetailJsonLD(benchmarkId)
        return ResponseEntity.ok(benchmarkJsonLD)
    }

    @Operation(
        summary = "Delete a specific metric from a benchmark",
        description = "Send the id of the metric that you want to delete inside the benchmark record"
    )
    @PostMapping("/{benchmarkId}/delete/metric")
    suspend fun deleteMetric(@PathVariable benchmarkId: String, @RequestBody metrics: List<String> ): ResponseEntity<BenchmarkRecord> {
        val result = benchMarkService.deleteMetric(benchmarkId, metrics)
        return ResponseEntity.ok(result)
    }







}