package io.github.ostrails.dmpevaluatorservice.controller

import io.github.ostrails.dmpevaluatorservice.database.model.MetricRecord
import io.github.ostrails.dmpevaluatorservice.service.MetricService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@Tag(name = "Metric APIs", description = "Manage metrics and associated tests")
@RestController
@RequestMapping("/api/metrics")
class MetricController(
    val metricService: MetricService
) {

    @PostMapping
    suspend fun create(@RequestBody metricBody: MetricRecord): ResponseEntity<MetricRecord> {
        val result = metricService.createMetric(metricBody)
        return ResponseEntity.ok(result)
    }

    @GetMapping
    suspend fun list(): ResponseEntity<List<MetricRecord>> {
        val result = metricService.listMetrics()
        return ResponseEntity.ok(result)
    }

    @GetMapping("/{metricId}")
    suspend fun detailMetric(@PathVariable metricId: String): ResponseEntity<MetricRecord> {
        val result =  metricService.metricDetail(metricId)
        return ResponseEntity.ok(result)
    }

    @PostMapping("/addTests/{metricId}")
    suspend fun addTests(@PathVariable metricId: String, @RequestBody tests: List<String>): ResponseEntity<MetricRecord> {
        val result = metricService.addTests(metricId, tests)
        return  ResponseEntity.ok(result)
    }

    @PostMapping("/addBenchmarks/{metricId}")
    suspend fun addBenchmark(@PathVariable metricId: String, @RequestBody benchMarks: List<String>): ResponseEntity<MetricRecord> {
        val result = metricService.addBenchMark(metricId, benchMarks)
        return ResponseEntity.ok(result)
    }

    @DeleteMapping("/{metricId}")
    suspend fun delete(@PathVariable metricId: String): ResponseEntity<String?> {
        val result = metricService.deleteMetric(metricId)
        if (result != null) {
            return ResponseEntity.ok(result)
        }else{
            return ResponseEntity.notFound().build()
        }
    }

    @PostMapping("/delete/test/{metricId}")
    suspend fun deleteTest(@PathVariable metricId: String, @RequestBody tests: List<String>): ResponseEntity<MetricRecord> {
        val result = metricService.deleteTest(metricId, tests)
        return ResponseEntity.ok(result)
    }

}