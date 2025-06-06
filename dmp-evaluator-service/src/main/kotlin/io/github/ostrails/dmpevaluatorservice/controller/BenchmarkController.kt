package io.github.ostrails.dmpevaluatorservice.controller

import io.github.ostrails.dmpevaluatorservice.database.model.BenchmarkRecord
import io.github.ostrails.dmpevaluatorservice.model.benchmark.BenchmarkJsonLD
import io.github.ostrails.dmpevaluatorservice.model.benchmark.BenchmarkUpdateRequest
import io.github.ostrails.dmpevaluatorservice.model.metric.metricsListsIDs
import io.github.ostrails.dmpevaluatorservice.service.BenchmarService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@Tag(name = "Benchmark APIs", description = "Manage benchmarks and associated metrics")
@RestController
@RequestMapping("/api/benchmarks")
class BenchmarkController(
    val benchMarkService: BenchmarService
) {

    @PostMapping
    suspend fun create(@RequestBody benchmarkBody: BenchmarkRecord): ResponseEntity<BenchmarkRecord> {
        val result = benchMarkService.createBenchmark(benchmarkBody)
        return ResponseEntity.ok(result)
    }

    @PostMapping("/edit/metrics/{benchmarkId}")
    suspend fun addMetric(@PathVariable benchmarkId: String, @RequestBody newMetrics: metricsListsIDs): ResponseEntity<BenchmarkRecord> {
        val benchmark = benchMarkService.addMetric(benchmarkId, newMetrics.metrics)
        return ResponseEntity.ok(benchmark)
    }

    @PostMapping("/edit/{benchmarkId}")
    suspend fun updateBenchmark(@PathVariable benchmarkId: String, @RequestBody benchmark: BenchmarkUpdateRequest): ResponseEntity<BenchmarkRecord> {
        val benchmark = benchMarkService.updateBenchmark(benchmarkId, benchmark)
        return ResponseEntity.ok(benchmark)
    }

    @GetMapping
    suspend fun getBenchmarks(): ResponseEntity<List<BenchmarkRecord>> {
        val benchmarks = benchMarkService.getBenchmarks()
        return ResponseEntity.ok(benchmarks)
    }

//    @GetMapping("/json-ld")
//    suspend fun getBenchmarksJsonLD (): ResponseEntity<List<BenchmarkJsonLD>> {
//        val benchmarks = benchMarkService.getBenchmarks()
//        val result = benchmarks.map { benchmark -> benchMarkService.toJsonLD(benchmark) }
//        return ResponseEntity.ok(result)
//    }

    @DeleteMapping("/{benchmarkId}")
    suspend fun deleteBenchmark(@PathVariable benchmarkId: String): ResponseEntity<String> {
        val benchmarkIdRecord = benchMarkService.deleteBenchmark(benchmarkId)
        return ResponseEntity.ok(benchmarkId)
    }

    @GetMapping("/{benchmarkId}")
    suspend fun getBenchmark(@PathVariable benchmarkId: String): ResponseEntity<BenchmarkRecord> {
        val benchmark = benchMarkService.getBenchmarkDetail(benchmarkId)
        return ResponseEntity.ok(benchmark)
    }

    @PostMapping("/filter")
    suspend fun getBenchmarkById(@RequestBody benchmarkIds: List<String>): ResponseEntity<List<BenchmarkRecord>> {
        val benchmark = benchMarkService.getBenchmarskDetail(benchmarkIds)
        return ResponseEntity.ok(benchmark)
    }

    @GetMapping("/{benchmarkId}/json-ld")
    suspend fun getBenchmarkJsonLD(@PathVariable benchmarkId: String): ResponseEntity<BenchmarkJsonLD> {
        val benchmarkJsonLD = benchMarkService.getBenchmarkDetailJsonLD(benchmarkId)
        return ResponseEntity.ok(benchmarkJsonLD)
    }

    @PostMapping("/{benchmarkId}/delete/metric")
    suspend fun deleteMetric(@PathVariable benchmarkId: String, @RequestBody metrics: List<String> ): ResponseEntity<BenchmarkRecord> {
        val result = benchMarkService.deleteMetric(benchmarkId, metrics)
        return ResponseEntity.ok(result)
    }







}