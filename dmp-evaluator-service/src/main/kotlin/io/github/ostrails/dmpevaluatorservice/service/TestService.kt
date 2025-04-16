package io.github.ostrails.dmpevaluatorservice.service

import io.github.ostrails.dmpevaluatorservice.database.model.TestRecord
import io.github.ostrails.dmpevaluatorservice.database.repository.TestRepository
import io.github.ostrails.dmpevaluatorservice.exceptionHandler.ResourceNotFoundException
import io.github.ostrails.dmpevaluatorservice.model.requests.TestUpdateRequest
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Service

@Service
class TestService(
    val testRepository: TestRepository
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
       val test = testRepository.findById(testId).awaitSingle()
       if (test != null) {
           val updateTest = test.copy(
               title = testInfo.title ?: test.title,
               description = testInfo.description ?: test.description,
               license = testInfo.license ?: test.license,
               version = testInfo.version ?: test.version,
               metricImplemented = testInfo.metricImplemented ?: testInfo.metricImplemented,
               evaluator = testInfo.evaluator ?: testInfo.evaluator,
               functionEvaluator = testInfo.functionEvaluator ?: testInfo.functionEvaluator, )
           return testRepository.save(updateTest).awaitSingle()

       }else {
           return null
       }
   }
}