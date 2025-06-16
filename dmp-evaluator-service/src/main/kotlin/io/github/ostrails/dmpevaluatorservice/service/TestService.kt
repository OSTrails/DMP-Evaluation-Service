package io.github.ostrails.dmpevaluatorservice.service

import io.github.ostrails.dmpevaluatorservice.database.model.TestRecord
import io.github.ostrails.dmpevaluatorservice.database.repository.TestRepository
import io.github.ostrails.dmpevaluatorservice.exceptionHandler.DatabaseException
import io.github.ostrails.dmpevaluatorservice.exceptionHandler.ResourceNotFoundException
import io.github.ostrails.dmpevaluatorservice.model.requests.TestAddMetricRequest
import io.github.ostrails.dmpevaluatorservice.model.requests.TestUpdateRequest
import io.github.ostrails.dmpevaluatorservice.model.test.FullTestLDEntry
import io.github.ostrails.dmpevaluatorservice.model.test.IdWrapper
import io.github.ostrails.dmpevaluatorservice.model.test.LangLiteral
import io.github.ostrails.dmpevaluatorservice.model.test.TestJsonLD
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

   suspend fun addMetric(testId: String, testInfo: TestAddMetricRequest): TestRecord? {
       val test = testRepository.findById(testId).awaitFirstOrNull() ?: throw ResourceNotFoundException("Test with id $testId not found")

       if (testInfo.evaluator != null) {
           val updateTest = test.copy(
               metricImplemented = testInfo.metricImplemented,
               evaluator = testInfo.evaluator,
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

    suspend fun updateTest (testId: String, newTestData: TestUpdateRequest): TestRecord {
        val test = testRepository.findById(testId).awaitFirstOrNull() ?: throw ResourceNotFoundException("Test with id $testId not found")
        val updateTest = test.copy(
            title = newTestData.title ?: test.title,
            description = newTestData.description ?: test.description,
            license = newTestData.license ?: test.license,
            version =  newTestData.version ?: test.version,
            endpointURL = newTestData.endpointURL ?: test.endpointURL,
            endpointDescription = newTestData.description ?: test.description,
            keyword = newTestData.keyword ?: test.keyword,
            abbreviation = newTestData.abbreviation ?: test.abbreviation,
            repository = newTestData.repository ?: test.repository,
            type = newTestData.type ?: test.type,
            theme = newTestData.theme ?: test.theme,
            versionNotes = newTestData.versionNotes ?: test.versionNotes,
            status = newTestData.status ?: test.status,
            isApplicableFor = newTestData.isApplicableFor ?: test.isApplicableFor,
            supportedBy = newTestData.supportedBy ?: test.supportedBy,
        )
        return testRepository.save(updateTest).awaitSingle()
  }

    suspend fun testJsonLD(testId: String): TestJsonLD {
        val test = testRepository.findById(testId).awaitFirstOrNull() ?: throw ResourceNotFoundException("Test with id $testId not found")
        val keywords = test.keyword?.split(",")?.map { LangLiteral(value = it.trim()) }

        val mainTest = FullTestLDEntry(
            id = "urn:dmpEvaluationService:${test.id}",
            identifier = IdWrapper(test.id ?: "urn:uuid:test-id"),
            title = LangLiteral(value = test.title),
            description = LangLiteral(value = test.description),
            license = IdWrapper(test.license),
            endpointURL = IdWrapper(test.endpointURL ?: ""),
            endpointDescription = test.endpointDescription?.let { IdWrapper(it) },
            version = LangLiteral(value = test.version),
            keyword = keywords,
            typeUri = test.type?.let { IdWrapper(it) },
            theme = test.theme?.let { IdWrapper(it) },
            inDimension = null, // optionally filled
            supportedBy = test.supportedBy?.let { IdWrapper(it) },
            isApplicableFor = test.isApplicableFor?.let { IdWrapper(it) },
            creator = test.evaluator?.let { IdWrapper(it) },
            contactPoint = listOfNotNull(
                test.evaluator?.let { IdWrapper(it) },
                test.supportedBy?.let { IdWrapper(it) }
            ).takeIf { it.isNotEmpty() },
            linkedMetric = test.metricImplemented?.let { IdWrapper(it) }
        )
        return TestJsonLD(graph = listOf(mainTest))

    }


}