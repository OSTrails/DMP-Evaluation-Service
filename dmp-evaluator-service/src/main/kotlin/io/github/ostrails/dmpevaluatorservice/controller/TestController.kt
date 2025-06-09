package io.github.ostrails.dmpevaluatorservice.controller

import io.github.ostrails.dmpevaluatorservice.database.model.TestRecord
import io.github.ostrails.dmpevaluatorservice.model.requests.TestAddMetricRequest
import io.github.ostrails.dmpevaluatorservice.model.requests.TestUpdateRequest
import io.github.ostrails.dmpevaluatorservice.model.test.TestJsonLD
import io.github.ostrails.dmpevaluatorservice.service.TestService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Test APIs", description = "Manage tests")
@RestController
@RequestMapping("/api/tests")
class TestController(
    val testService: TestService,
) {

    @PostMapping
    suspend fun createTest(@RequestBody test: TestRecord): ResponseEntity<TestRecord>{
        val result = testService.createTest(test)
        return ResponseEntity.ok(result)
    }

    @PostMapping("/{testId}")
    suspend fun updateTest(@PathVariable testId: String,@RequestBody test: TestUpdateRequest): ResponseEntity<TestRecord>{
        val result = testService.updateTest(testId, test)
        return ResponseEntity.ok(result)
    }



    @GetMapping
    suspend fun getTests(): ResponseEntity<List<TestRecord>>{
        val result = testService.listAllTests()
        return ResponseEntity.ok(result)
    }

    @GetMapping("/{testId}")
    suspend fun getTest(@PathVariable testId: String): ResponseEntity<TestRecord>{
        val result = testService.getTest(testId)
        return ResponseEntity.ok(result)
    }

    @GetMapping("/{testId}/json-ld")
    suspend fun getTestJsonLD(@PathVariable testId: String): ResponseEntity<TestJsonLD> {
        val result = testService.testJsonLD(testId)
        return ResponseEntity.ok(result)
    }

    @GetMapping("/list/json-ld")
    suspend fun getTestsJsonLD(): ResponseEntity<List<TestJsonLD?>> {
        val result = testService.listAllTests()
        val resultJsonLD = result.map { it.id?.let { it1 -> testService.testJsonLD(it1) } }
        return ResponseEntity.ok(resultJsonLD)
    }

    @DeleteMapping("/{testId}")
    suspend fun deleteTest(@PathVariable testId: String): ResponseEntity<String>{
        val result = testService.deleteTest(testId)
        return ResponseEntity.ok(result)
    }

    @GetMapping("/metrics/{metricId}")
    suspend fun getTestsByMetricId(@PathVariable metricId: String): ResponseEntity<List<TestRecord>>{
        val result = testService.getTestsByMetrics(metricId)
        return ResponseEntity.ok(result)
    }

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