package io.github.ostrails.dmpevaluatorservice.service

import io.github.ostrails.dmpevaluatorservice.database.model.TestRecord
import io.github.ostrails.dmpevaluatorservice.database.repository.TestRepository
import io.github.ostrails.dmpevaluatorservice.exceptionHandler.DatabaseException
import io.github.ostrails.dmpevaluatorservice.exceptionHandler.ForbiddenException
import io.github.ostrails.dmpevaluatorservice.exceptionHandler.ResourceNotFoundException
import io.github.ostrails.dmpevaluatorservice.model.requests.TestAddMetricRequest
import io.github.ostrails.dmpevaluatorservice.model.requests.TestUpdateRequest
import io.github.ostrails.dmpevaluatorservice.model.test.IdWrapper
import io.github.ostrails.dmpevaluatorservice.model.test.LangLiteral
import io.github.ostrails.dmpevaluatorservice.model.test.TestJsonLD
import io.github.ostrails.dmpevaluatorservice.utils.ConfigurationGlobalVariables
import io.github.ostrails.dmpevaluatorservice.utils.ConfigurationTestVariables
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.awaitSingle
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class TestService(
    val testRepository: TestRepository,
    val configurationTestVariables: ConfigurationTestVariables,
    val configurationGlobalVariables: ConfigurationGlobalVariables,
    private val metricService: MetricService
) {

    private val log: Logger = LoggerFactory.getLogger(TestService::class.java)

    suspend fun createTest(test: TestRecord, callerClientId: String): TestRecord {
        val enrichedTest = test.copy(
            repository = configurationGlobalVariables.repository,
            endpointURL = configurationTestVariables.endpointURL + "/" + test.id,
            createdBy = callerClientId
        )
        val saved = testRepository.save(enrichedTest).awaitSingle()
        log.info("Created test '${saved.id}'")
        return saved
    }

    suspend fun listAllTests(): List<TestRecord> {
        val tests = testRepository.findAll().collectList().awaitSingle()
        return tests.map {
            it.copy(
                repository = configurationGlobalVariables.repository,
                endpointURL = configurationTestVariables.endpointURL + "/" + it.id
            )
        }
    }

    suspend fun listAllTestUIDs(): List<String?> {
        val tests = testRepository.findAll().collectList().awaitSingle()
        return tests.map { configurationTestVariables.endpointURL + "/" + it.id }
    }

    suspend fun getTest(testId: String): TestRecord {
        val test = testRepository.findById(testId).awaitFirstOrNull() ?: run {
            log.warn("Test '$testId' not found")
            throw ResourceNotFoundException("Test with id $testId not found")
        }
        return test.copy(endpointURL = configurationTestVariables.endpointURL + "/" + test.id)
    }

    suspend fun addMetric(testId: String, testInfo: TestAddMetricRequest, callerClientId: String, isAdmin: Boolean): TestRecord? {
        val test = testRepository.findById(testId).awaitFirstOrNull() ?: run {
            log.warn("Test '$testId' not found")
            throw ResourceNotFoundException("Test with id $testId not found")
        }
        checkOwnership(test.createdBy, callerClientId, isAdmin)
        if (testInfo.evaluator != null) {
            val updateTest = test.copy(
                metricImplemented = testInfo.metricImplemented,
                evaluator = testInfo.evaluator,
                functionEvaluator = testInfo.functionEvaluator ?: testInfo.functionEvaluator,
            )
            metricService.addTests(testInfo.metricImplemented, listOf(testId))
            val testSaved = testRepository.save(updateTest).awaitSingle()
            log.debug("Linked test '$testId' to metric '${testInfo.metricImplemented}'")
            return testSaved
        } else {
            log.warn("Test '$testId': no evaluator supplied for metric '${testInfo.metricImplemented}'")
            throw ResourceNotFoundException("The metric id ${testInfo.metricImplemented} not found")
        }
    }

    suspend fun deleteTest(testId: String): String? {
        val test = testRepository.findById(testId).awaitFirstOrNull() ?: run {
            log.warn("Test '$testId' not found")
            throw ResourceNotFoundException("Test with id $testId not found")
        }
        try {
            testRepository.delete(test).awaitFirstOrNull()
            log.info("Deleted test '$testId'")
        } catch (e: Exception) {
            log.error("Failed to delete test '$testId'", e)
            throw DatabaseException("There is a error with the database trying to delete the test ${testId}   ${e.message}")
        }
        return testId
    }

    suspend fun findMultipleTests(testsIds: List<String>): List<TestRecord> {
        val tests = testRepository.findByIdIn(testsIds).collectList().awaitSingle() ?: run {
            log.warn("No tests found for ids $testsIds")
            throw ResourceNotFoundException("Tests with the ids ${testsIds} not found")
        }
        return tests.map {
            it.copy(
                repository = configurationGlobalVariables.repository,
                endpointURL = configurationTestVariables.endpointURL + "/" + it.id
            )
        }
    }

    suspend fun getTestsByMetrics(metricId: String): List<TestRecord> {
        val tests = testRepository.findBymetricImplemented(metricId).collectList().awaitSingle() ?: run {
            log.warn("No tests found for metric '$metricId'")
            throw ResourceNotFoundException("Tests associated with the metric id $metricId not found")
        }
        return tests.map {
            it.copy(
                repository = configurationGlobalVariables.repository,
                endpointURL = configurationTestVariables.endpointURL + "/" + it.id
            )
        }
    }

    suspend fun updateTest(testId: String, newTestData: TestUpdateRequest, callerClientId: String, isAdmin: Boolean): TestRecord {
        val test = testRepository.findById(testId).awaitFirstOrNull() ?: run {
            log.warn("Test '$testId' not found")
            throw ResourceNotFoundException("Test with id $testId not found")
        }
        checkOwnership(test.createdBy, callerClientId, isAdmin)
        log.debug("Updating test '$testId'")
        val updateTest = test.copy(
            title = newTestData.title ?: test.title,
            description = newTestData.description ?: test.description,
            license = newTestData.license ?: test.license,
            version = newTestData.version ?: test.version,
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
        val updatedTestSaved = testRepository.save(updateTest).awaitSingle()
        return updatedTestSaved.copy(endpointURL = configurationTestVariables.endpointURL + "/" + updatedTestSaved.id)
    }

    suspend fun testJsonLD(testId: String): TestJsonLD {
        val test = testRepository.findById(testId).awaitFirstOrNull() ?: run {
            log.warn("Test '$testId' not found")
            throw ResourceNotFoundException("Test with id $testId not found")
        }
        val keywords = test.keyword?.split(",")?.map { LangLiteral(value = it.trim()) }
        return TestJsonLD(
            id = "urn:dmpEvaluationService:${test.id}",
            identifier = IdWrapper(test.id ?: "urn:uuid:test-id"),
            title = LangLiteral(value = test.title),
            description = LangLiteral(value = test.description),
            license = IdWrapper(test.license),
            endpointURL = IdWrapper("${configurationTestVariables.endpointURL}/${test.id}"),
            endpointDescription = test.endpointDescription?.let { IdWrapper(it) },
            version = LangLiteral(value = test.version),
            keyword = keywords,
            typeUri = test.type?.let { IdWrapper(it) },
            theme = test.theme?.let { IdWrapper(it) },
            inDimension = null,
            supportedBy = test.supportedBy?.let { IdWrapper(it) },
            isApplicableFor = test.isApplicableFor?.let { IdWrapper(it) },
            creator = test.evaluator?.let { IdWrapper(it) },
            contactPoint = listOfNotNull(
                test.evaluator?.let { IdWrapper(it) },
                test.supportedBy?.let { IdWrapper(it) }
            ).takeIf { it.isNotEmpty() },
            linkedMetric = test.metricImplemented?.let { IdWrapper(it) }
        )
    }

    private fun checkOwnership(createdBy: String?, callerClientId: String, isAdmin: Boolean) {
        if (isAdmin) return
        if (createdBy == null)
            throw ForbiddenException("Only ADMIN can modify records without an owner")
        if (createdBy != callerClientId)
            throw ForbiddenException("You can only modify records you created")
    }
}
