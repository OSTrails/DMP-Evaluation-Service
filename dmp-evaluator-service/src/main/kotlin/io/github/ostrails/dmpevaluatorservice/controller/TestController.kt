package io.github.ostrails.dmpevaluatorservice.controller

import io.github.ostrails.dmpevaluatorservice.database.model.TestRecord
import io.github.ostrails.dmpevaluatorservice.model.requests.TestAddMetricRequest
import io.github.ostrails.dmpevaluatorservice.model.requests.TestUpdateRequest
import io.github.ostrails.dmpevaluatorservice.model.test.TestJsonLD
import io.github.ostrails.dmpevaluatorservice.service.TestService
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@Tag(name = "Test APIs", description = "Manage tests")
@RestController
@RequestMapping("/api/tests")
class TestController(
    val testService: TestService,
) {

    @Operation(
        summary = "Create a test record",
        description = "Receives the json with the data that describe a test and create a new one and return the test record "
    )
    @PostMapping
    suspend fun createTest(@RequestBody test: TestRecord): ResponseEntity<TestRecord>{
        val result = testService.createTest(test)
        return ResponseEntity.ok(result)
    }

    @Operation(
        summary = "Update a test record",
        description = "Receives the json with the data that describe a test and updated return the test record "
    )
    @PostMapping("/{testId}")
    suspend fun updateTest(@PathVariable testId: String,@RequestBody test: TestUpdateRequest): ResponseEntity<TestRecord>{
        val result = testService.updateTest(testId, test)
        return ResponseEntity.ok(result)
    }

    @Operation(
        summary = "get the list of tests ids",
        description = "Return the list of tests ids that are in the system"
    )
    @GetMapping
    suspend fun getTestsIds(): ResponseEntity<List<String?>>{
        val result = testService.listAllTestUIDs()
        return ResponseEntity.ok(result)
    }

    @Operation(
        summary = "get the list of tests",
        description = "Return the list of tests that are in the system"
    )
    @GetMapping("/info")
    suspend fun getTests(): ResponseEntity<List<TestRecord>>{
        val result = testService.listAllTests()
        return ResponseEntity.ok(result)
    }

    @Operation(
        summary = "Get a specific test",
        description = "Receives the id of a test and return the test record"
    )
    @GetMapping("/info/", produces =   ["application/json"])
    suspend fun getTestById(@RequestParam("testId") testId: String): ResponseEntity<TestRecord> {
        val test = testService.getTest(testId)
        return ResponseEntity.ok(test)
    }

    @Operation(
        summary = "Get a test in json - ld using the pathvariable ",
        description = "Return a test in json ld format "
    )
    @GetMapping("/{testId}", produces =   ["application/ld+json"])
    suspend fun getTestJsonLD(@PathVariable testId: String): ResponseEntity<TestJsonLD> {
        val result = testService.testJsonLD(testId)
        val headers = HttpHeaders()
        headers.contentType = MediaType.valueOf("application/ld+json")
        return ResponseEntity(result, headers, HttpStatus.OK )
    }

    @Operation(
        summary = "Get a test in json - ld using the Requestparam testId",
        description = "Return a test in json ld format "
    )
    @GetMapping("/", produces =   ["application/ld+json"])
    suspend fun getTestJsonLDFrom(@RequestParam("testId") testId: String): ResponseEntity<TestJsonLD> {
        val result = testService.testJsonLD(testId)
        val headers = HttpHeaders()
        headers.contentType = MediaType.valueOf("application/ld+json")
        return ResponseEntity(result, headers, HttpStatus.OK )
    }

    @Operation(
        summary = "List the test in json ld ",
        description = "return a list of tests in json-ld "
    )
    @GetMapping("/list", produces =   ["application/ld+json"])
    suspend fun getTestsJsonLD(): ResponseEntity<List<TestJsonLD?>> {
        val result = testService.listAllTests()
        val resultJsonLD = result.map { it.id?.let { it1 -> testService.testJsonLD(it1) } }
        return ResponseEntity.ok(resultJsonLD)
    }

    @Operation(
        summary = "Delete a test record",
        description = "Receive the testId and delete that test record"
    )
    @DeleteMapping("/{testId}")
    suspend fun deleteTest(@PathVariable testId: String): ResponseEntity<String>{
        val result = testService.deleteTest(testId)
        return ResponseEntity.ok(result)
    }

    @Operation(
        summary = "Filter the test by metric",
        description = "Receive a metric id and return a list of tests that are implementations of that metric"
    )
    @GetMapping("/metrics/{metricId}")
    suspend fun getTestsByMetricId(@PathVariable metricId: String): ResponseEntity<List<TestRecord>>{
        val result = testService.getTestsByMetrics(metricId)
        return ResponseEntity.ok(result)
    }


    @Operation(
        summary = "Update the evaluator and the function of a test",
        description = "receive a testId and the metric, evaluator and function that implement that test"
    )
    @PostMapping("/{testId}/addEvaluator")
    suspend fun updateTestEvaluator(@PathVariable testId: String,@RequestBody test: TestAddMetricRequest): ResponseEntity<TestRecord>{
        val result = testService.addMetric(testId, test)
        if (result != null){
            return ResponseEntity.ok(result)
        }else{
            return ResponseEntity.notFound().build()
        }
    }


}