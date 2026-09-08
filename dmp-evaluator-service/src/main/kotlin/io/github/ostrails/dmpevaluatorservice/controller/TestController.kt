package io.github.ostrails.dmpevaluatorservice.controller

import io.github.ostrails.dmpevaluatorservice.auth.isAdmin
import io.github.ostrails.dmpevaluatorservice.model.requests.TestAddMetricRequest
import io.github.ostrails.dmpevaluatorservice.model.requests.TestCreateRequest
import io.github.ostrails.dmpevaluatorservice.model.requests.TestUpdateRequest
import io.github.ostrails.dmpevaluatorservice.model.test.TestJsonLD
import io.github.ostrails.dmpevaluatorservice.model.test.TestResponse
import io.github.ostrails.dmpevaluatorservice.model.test.toResponse
import io.github.ostrails.dmpevaluatorservice.service.TestService
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@Tag(name = "Test APIs", description = "Manage tests")
@RestController
@RequestMapping("/tests")
class TestController(
    val testService: TestService,
) {

    @Operation(summary = "Create a test record", security = [SecurityRequirement(name = "bearerAuth")])
    @PostMapping
    suspend fun createTest(
        @RequestBody test: TestCreateRequest,
        authentication: Authentication
    ): ResponseEntity<TestResponse> {
        val result = testService.createTest(test, authentication.name)
        return ResponseEntity.ok(result.toResponse())
    }

    @Operation(summary = "Update a test record", security = [SecurityRequirement(name = "bearerAuth")])
    @PutMapping("/{testId}")
    suspend fun updateTest(
        @PathVariable testId: String,
        @RequestBody test: TestUpdateRequest,
        authentication: Authentication
    ): ResponseEntity<TestResponse> {
        val result = testService.updateTest(testId, test, authentication.name, authentication.isAdmin())
        return ResponseEntity.ok(result.toResponse())
    }

    @Operation(summary = "Get the list of tests ids")
    @GetMapping
    suspend fun getTestsIds(): ResponseEntity<List<String?>> =
        ResponseEntity.ok(testService.listAllTestUIDs())

    @Operation(summary = "Get the list of tests")
    @GetMapping("/info", produces = ["application/json"])
    suspend fun getTests(): ResponseEntity<List<TestResponse>> =
        ResponseEntity.ok(testService.listAllTests().map { it.toResponse() })

    @Operation(summary = "Get a specific test")
    @GetMapping("/info/{testId}", produces = ["application/json"])
    suspend fun getTestById(@PathVariable testId: String): ResponseEntity<TestResponse> =
        ResponseEntity.ok(testService.getTest(testId).toResponse())

    @Operation(summary = "Get a test in json-ld using the path variable")
    @GetMapping("/{testId}", produces = ["application/ld+json"])
    suspend fun getTestJsonLD(@PathVariable testId: String): ResponseEntity<TestJsonLD> {
        val result = testService.testJsonLD(testId)
        val headers = HttpHeaders()
        headers.contentType = MediaType.valueOf("application/ld+json")
        return ResponseEntity(result, headers, HttpStatus.OK)
    }

    @Operation(summary = "Get a test in json-ld using the request param testId")
    @GetMapping("/", produces = ["application/ld+json"])
    suspend fun getTestJsonLDFrom(@RequestParam("testId") testId: String): ResponseEntity<TestJsonLD> {
        val result = testService.testJsonLD(testId)
        val headers = HttpHeaders()
        headers.contentType = MediaType.valueOf("application/ld+json")
        return ResponseEntity(result, headers, HttpStatus.OK)
    }

    @Operation(summary = "List the tests in json-ld")
    @GetMapping("/list", produces = ["application/ld+json"])
    suspend fun getTestsJsonLD(): ResponseEntity<List<TestJsonLD?>> {
        val result = testService.listAllTests()
        return ResponseEntity.ok(result.map { it.id?.let { id -> testService.testJsonLD(id) } })
    }

    @Operation(summary = "Delete a test record", security = [SecurityRequirement(name = "bearerAuth")])
    @DeleteMapping("/{testId}")
    suspend fun deleteTest(@PathVariable testId: String): ResponseEntity<String> =
        ResponseEntity.ok(testService.deleteTest(testId))

    @Operation(summary = "Filter tests by metric")
    @GetMapping("/metrics/{metricId}")
    suspend fun getTestsByMetricId(@PathVariable metricId: String): ResponseEntity<List<TestResponse>> =
        ResponseEntity.ok(testService.getTestsByMetrics(metricId).map { it.toResponse() })

    @Operation(summary = "Update the evaluator and function of a test", security = [SecurityRequirement(name = "bearerAuth")])
    @PostMapping("/{testId}/addEvaluator")
    suspend fun updateTestEvaluator(
        @PathVariable testId: String,
        @RequestBody test: TestAddMetricRequest,
        authentication: Authentication
    ): ResponseEntity<TestResponse> {
        val result = testService.addMetric(testId, test, authentication.name, authentication.isAdmin())
        return if (result != null) ResponseEntity.ok(result.toResponse()) else ResponseEntity.notFound().build()
    }
}
