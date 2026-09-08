package io.github.ostrails.dmpevaluatorservice.controller

import io.github.ostrails.dmpevaluatorservice.auth.isAdmin
import io.github.ostrails.dmpevaluatorservice.model.metric.MetricCreateRequest
import io.github.ostrails.dmpevaluatorservice.model.metric.MetricJsonLD
import io.github.ostrails.dmpevaluatorservice.model.metric.MetricResponse
import io.github.ostrails.dmpevaluatorservice.model.metric.MetricUpdateRequest
import io.github.ostrails.dmpevaluatorservice.model.metric.toResponse
import io.github.ostrails.dmpevaluatorservice.service.MetricService
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@Tag(name = "Metric APIs", description = "Manage metrics and associated tests")
@RestController
@RequestMapping("/metrics")
class MetricController(
    val metricService: MetricService
) {

    @Operation(summary = "Create a metric", security = [SecurityRequirement(name = "bearerAuth")])
    @PostMapping
    suspend fun create(
        @RequestBody metricBody: MetricCreateRequest,
        authentication: Authentication
    ): ResponseEntity<MetricResponse> {
        val result = metricService.createMetric(metricBody, authentication.name)
        return ResponseEntity.ok(result.toResponse())
    }

    @Operation(summary = "List the ids of metrics")
    @GetMapping(produces = ["application/json"])
    suspend fun getMetricsIds(): ResponseEntity<List<String?>> =
        ResponseEntity.ok(metricService.listMetricsIds())

    @Operation(summary = "List the metrics")
    @GetMapping("/list", produces = ["application/json"])
    suspend fun getMetrics(): ResponseEntity<List<MetricResponse>> =
        ResponseEntity.ok(metricService.listMetrics().map { it.toResponse() })

    @Operation(summary = "List the metrics in Json-ld")
    @GetMapping("/list/jsonLD", produces = ["application/ld+json"])
    suspend fun listJsonLD(): ResponseEntity<List<MetricJsonLD?>> =
        ResponseEntity.ok(metricService.getMetricsJsonLD())

    @Operation(summary = "Detail of a metric")
    @GetMapping("/info/{metricId}", produces = ["application/json"])
    suspend fun detailMetric(@PathVariable metricId: String): ResponseEntity<MetricResponse> =
        ResponseEntity.ok(metricService.metricDetail(metricId).toResponse())

    @Operation(summary = "Detail metric in json-ld")
    @GetMapping("/{metricId}")
    suspend fun detailMetricJsonLD(@PathVariable metricId: String): ResponseEntity<MetricJsonLD> =
        ResponseEntity.ok(metricService.getMetricDetailJsonLD(metricId))

    @Operation(summary = "Detail metric in json-ld using the request param")
    @GetMapping("/")
    suspend fun detailMetricJsonLDFromRequest(@RequestParam("metricId") metricId: String): ResponseEntity<MetricJsonLD> =
        ResponseEntity.ok(metricService.getMetricDetailJsonLD(metricId))

    @Operation(summary = "Update a metric", security = [SecurityRequirement(name = "bearerAuth")])
    @PutMapping("/{metricId}")
    suspend fun updateMetric(
        @PathVariable metricId: String,
        @RequestBody metric: MetricUpdateRequest,
        authentication: Authentication
    ): ResponseEntity<MetricResponse> {
        val result = metricService.updateMetric(metricId, metric, authentication.name, authentication.isAdmin())
        return ResponseEntity.ok(result.toResponse())
    }

    @Operation(summary = "Add tests to a specific metric", security = [SecurityRequirement(name = "bearerAuth")])
    @PostMapping("/addTests/{metricId}")
    suspend fun addTests(
        @PathVariable metricId: String,
        @RequestBody tests: List<String>,
        authentication: Authentication
    ): ResponseEntity<MetricResponse> {
        val result = metricService.addTests(metricId, tests, authentication.name, authentication.isAdmin())
        return ResponseEntity.ok(result.toResponse())
    }

    @Operation(summary = "Add a benchmark to a metric", security = [SecurityRequirement(name = "bearerAuth")])
    @PostMapping("/addBenchmarks/{metricId}")
    suspend fun addBenchmark(
        @PathVariable metricId: String,
        @RequestBody benchMarks: List<String>,
        authentication: Authentication
    ): ResponseEntity<MetricResponse> {
        val result = metricService.addBenchMark(metricId, benchMarks, authentication.name, authentication.isAdmin())
        return ResponseEntity.ok(result.toResponse())
    }

    @Operation(summary = "Delete metric", security = [SecurityRequirement(name = "bearerAuth")])
    @DeleteMapping("/{metricId}")
    suspend fun delete(@PathVariable metricId: String): ResponseEntity<String?> {
        val result = metricService.deleteMetric(metricId)
        return if (result != null) ResponseEntity.ok(result) else ResponseEntity.notFound().build()
    }

    @Operation(summary = "Delete a specific test from a metric", security = [SecurityRequirement(name = "bearerAuth")])
    @PostMapping("/delete/test/{metricId}")
    suspend fun deleteTest(
        @PathVariable metricId: String,
        @RequestBody tests: List<String>,
        authentication: Authentication
    ): ResponseEntity<MetricResponse> {
        val result = metricService.deleteTest(metricId, tests, authentication.name, authentication.isAdmin())
        return ResponseEntity.ok(result.toResponse())
    }
}
