package io.github.ostrails.dmpevaluatorservice.controller

import io.github.ostrails.dmpevaluatorservice.database.model.MetricRecord
import io.github.ostrails.dmpevaluatorservice.model.metric.MetricJsonLD
import io.github.ostrails.dmpevaluatorservice.model.metric.MetricUpdateRequest
import io.github.ostrails.dmpevaluatorservice.service.MetricService
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@Tag(name = "Metric APIs", description = "Manage metrics and associated tests")
@RestController
@RequestMapping("/api/metrics")
class MetricController(
    val metricService: MetricService
) {

    @Operation(
        summary = "Create a metric",
        description = "Receives the json with the data that describe a metric and create a new one and return the metric record "
    )
    @PostMapping
    suspend fun create(@RequestBody metricBody: MetricRecord): ResponseEntity<MetricRecord> {
        val result = metricService.createMetric(metricBody)
        return ResponseEntity.ok(result)
    }

    @Operation(
        summary = "List the ids of metrics",
        description = "List all the ids of the metrics"
    )
    @GetMapping(produces =   ["application/json"])
    suspend fun getMetricsIds(): ResponseEntity<List<String?>> {
        val result = metricService.listMetricsIds()
        return ResponseEntity.ok(result)
    }

    @Operation(
        summary = "List the metrics",
        description = "List all the metrics"
    )
    @GetMapping("/list", produces =   ["application/json"])
    suspend fun getMetrics(): ResponseEntity<List<MetricRecord>> {
        val result = metricService.listMetrics()
        return ResponseEntity.ok(result)
    }


    @Operation(
        summary = "List the metrics in Json-ld ",
        description = "List all the metrics"
    )
    @GetMapping("/list/jsonLD", produces =   ["application/ld+json"])
    suspend fun listJsonLD(): ResponseEntity<List<MetricJsonLD?>> {
        val result = metricService.getMetricsJsonLD()
        return ResponseEntity.ok(result)
    }

    @Operation(
        summary = "Detail of a  metric",
        description = "Detail of a specific metric from the system"
    )
    @GetMapping("/info/{metricId}", produces =   ["application/json"])
    suspend fun detailMetric(@PathVariable metricId: String): ResponseEntity<MetricRecord> {
        val result =  metricService.metricDetail(metricId)
        return ResponseEntity.ok(result)
    }

    @Operation(
        summary = "Detail metric in json-ld",
        description = "Detail a metric from the system"
    )
    @GetMapping("/{metricId}")
    suspend fun detailMetricJsonLD(@PathVariable metricId: String): ResponseEntity<MetricJsonLD> {
        val result =  metricService.getMetricDetailJsonLD(metricId)
        return ResponseEntity.ok(result)
    }

    @Operation(
        summary = "Detail metric in json-ld using the request param",
        description = "Detail a metric from the system"
    )
    @GetMapping("/")
    suspend fun detailMetricJsonLDFromRequest(@RequestParam("metricId") metricId: String): ResponseEntity<MetricJsonLD> {
        val result =  metricService.getMetricDetailJsonLD(metricId)
        return ResponseEntity.ok(result)
    }

    @Operation(
        summary = "Update a metric",
        description = "update a metric, with a new data that it receives"
    )
    @PutMapping("/update/{metricId}")
    suspend fun updateMetric(@PathVariable metricId: String, @RequestBody metric: MetricUpdateRequest): ResponseEntity<MetricRecord> {
        val result = metricService.updateMetric(metricId, metric)
        return  ResponseEntity.ok(result)
    }

    @Operation(
        summary = "Add tests to a specific metric",
        description = "Add a list of tests to an specific metric"
    )
    @PostMapping("/addTests/{metricId}")
    suspend fun addTests(@PathVariable metricId: String, @RequestBody tests: List<String>): ResponseEntity<MetricRecord> {
        val result = metricService.addTests(metricId, tests)
        return  ResponseEntity.ok(result)
    }


    @Operation(
        summary = "Add a benchmark to a metric",
        description = "Associate a benchmark to a metric"
    )
    @PostMapping("/addBenchmarks/{metricId}")
    suspend fun addBenchmark(@PathVariable metricId: String, @RequestBody benchMarks: List<String>): ResponseEntity<MetricRecord> {
        val result = metricService.addBenchMark(metricId, benchMarks)
        return ResponseEntity.ok(result)
    }


    @Operation(
        summary = "Delete metric",
        description = "Delete a metric from the system"
    )
    @DeleteMapping("/{metricId}")
    suspend fun delete(@PathVariable metricId: String): ResponseEntity<String?> {
        val result = metricService.deleteMetric(metricId)
        if (result != null) {
            return ResponseEntity.ok(result)
        }else{
            return ResponseEntity.notFound().build()
        }
    }

    @Operation(
        summary = "Delete a specific test",
        description = "Delete a test from the metric record the system"
    )
    @PostMapping("/delete/test/{metricId}")
    suspend fun deleteTest(@PathVariable metricId: String, @RequestBody tests: List<String>): ResponseEntity<MetricRecord> {
        val result = metricService.deleteTest(metricId, tests)
        return ResponseEntity.ok(result)
    }

}