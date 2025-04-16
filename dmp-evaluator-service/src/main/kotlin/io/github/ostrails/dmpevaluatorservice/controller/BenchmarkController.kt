package io.github.ostrails.dmpevaluatorservice.controller

import io.github.ostrails.dmpevaluatorservice.database.model.BenchmarkRecord
import io.github.ostrails.dmpevaluatorservice.model.metric.metricsListsIDs
import io.github.ostrails.dmpevaluatorservice.service.BenchmarService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

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

    @PostMapping("/edit/{benchmarkId}")
    suspend fun addMetric(@PathVariable benchmarkId: String, @RequestBody newMetrics: metricsListsIDs): ResponseEntity<BenchmarkRecord> {
        val benchmark = benchMarkService.addMetric(benchmarkId, newMetrics.metrics)
        return ResponseEntity.ok(benchmark)
    }

    @GetMapping
    suspend fun getBenchmarks(): ResponseEntity<List<BenchmarkRecord>> {
        val benchmarks = benchMarkService.getBenchmarks()
        return ResponseEntity.ok(benchmarks)
    }




}