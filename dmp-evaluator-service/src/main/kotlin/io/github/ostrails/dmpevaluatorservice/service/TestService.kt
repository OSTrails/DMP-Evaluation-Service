package io.github.ostrails.dmpevaluatorservice.service

import io.github.ostrails.dmpevaluatorservice.database.model.TestRecord
import io.github.ostrails.dmpevaluatorservice.database.repository.TestRepository
import io.github.ostrails.dmpevaluatorservice.exceptionHandler.DatabaseException
import io.github.ostrails.dmpevaluatorservice.exceptionHandler.ResourceNotFoundException
import io.github.ostrails.dmpevaluatorservice.model.requests.TestUpdateRequest
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Service

@Service
class TestService(
    val testRepository: TestRepository,
    private val metricService: MetricService
) {

    suspend fun createTest(test: TestRecord): TestRecord {
        return testRepository.save(test).awaitSingle()
    }

    suspend fun listAllTests(): List<TestRecord> {
        return testRepository.findAll().collectList().awaitSingle()
    }

    suspend fun getTest(testId: String): TestRecord {
        val test = testRepository.findById(testId).awaitFirstOrNull() ?: throw ResourceNotFoundException("Test with id $testId not found")
        return test
    }

   suspend fun addMetric(testId: String, testInfo: TestUpdateRequest): TestRecord? {
       val test = testRepository.findById(testId).awaitFirstOrNull() ?: throw ResourceNotFoundException("Test with id $testId not found")

       if (testInfo.metricImplemented != null) {
           val updateTest = test.copy(
               title = testInfo.title ?: test.title,
               description = testInfo.description ?: test.description,
               license = testInfo.license ?: test.license,
               version = testInfo.version ?: test.version,
               metricImplemented = testInfo.metricImplemented,
               evaluator = testInfo.evaluator ?: testInfo.evaluator,
               functionEvaluator = testInfo.functionEvaluator ?: testInfo.functionEvaluator, )
           metricService.addTests(testInfo.metricImplemented, listOf(testId))
           val testSaved = testRepository.save(updateTest).awaitSingle()
           return testSaved
       }else {
           throw ResourceNotFoundException("The metric id ${testInfo.metricImplemented} not found")
       }
   }

    suspend fun deleteTest(testId: String): String? {
        val test = testRepository.findById(testId).awaitFirstOrNull() ?: throw ResourceNotFoundException("Test with id $testId not found")
        try {
            testRepository.delete(test).awaitFirstOrNull()
        }catch (e:Exception){
            throw DatabaseException("There is a error with the database trying to delete the test ${testId}   ${e.message}")
        }
        return testId
    }

    suspend fun findMultipleTests(testsIds: List<String>): List<TestRecord> {
            val tests = testRepository.findByIdIn(testsIds).collectList().awaitSingle() ?: throw ResourceNotFoundException("Tests with the ids ${testsIds} not found")
            return tests
    }

    suspend fun getTestsByMetrics(metricId: String):List<TestRecord>{
        val tests = testRepository.findBymetricImplemented(metricId).collectList().awaitSingle() ?: throw ResourceNotFoundException("Tests associated with the metric id $metricId not found")
        return tests
    }


}