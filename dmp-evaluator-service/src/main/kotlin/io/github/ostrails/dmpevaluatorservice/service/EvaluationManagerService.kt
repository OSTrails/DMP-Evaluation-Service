package io.github.ostrails.dmpevaluatorservice.service

import io.github.ostrails.dmpevaluatorservice.database.model.Evaluation
import io.github.ostrails.dmpevaluatorservice.database.model.EvaluationReport
import io.github.ostrails.dmpevaluatorservice.database.repository.EvaluationReportRepository
import io.github.ostrails.dmpevaluatorservice.database.repository.EvaluationResultRepository
import io.github.ostrails.dmpevaluatorservice.exceptionHandler.ApiException
import io.github.ostrails.dmpevaluatorservice.exceptionHandler.InputvalidationException
import io.github.ostrails.dmpevaluatorservice.exceptionHandler.ResourceNotFoundException
import io.github.ostrails.dmpevaluatorservice.model.EvaluationReportResponse
import io.github.ostrails.dmpevaluatorservice.model.EvaluationRequest
import io.github.ostrails.dmpevaluatorservice.model.EvaluationResult
import io.github.ostrails.dmpevaluatorservice.model.ResultTestEnum
import io.github.ostrails.dmpevaluatorservice.model.toInfo
import io.github.ostrails.dmpevaluatorservice.model.toResponse
import io.github.ostrails.dmpevaluatorservice.model.testResult.TestResultSetJsonLD
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.serialization.json.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service

@Service
class EvaluationManagerService(
    private val resultEvaluationResultRepository: EvaluationResultRepository,
    private val evaluationReportRepository: EvaluationReportRepository,
    private val benchmarkService: BenchmarService,
    private val evaluationService: EvaluationService,
    private val testService: TestService
) {

    private val log: Logger = LoggerFactory.getLogger(EvaluationManagerService::class.java)

    suspend fun generateEvaluations(request: EvaluationRequest): EvaluationResult {
        // fetch the report id from the request or from the db.
        val reportEvaluation = getReportId(request.reportId)
        log.debug("generateEvaluations: reportId=${reportEvaluation.reportId}, params=${request.evaluationParams}")
        val evaluationsResults = evaluationResults(reportEvaluation, request)
        return EvaluationResult(
            reportId = reportEvaluation.reportId.toString(),
            evaluations = evaluationsResults.map { it.toResponse() }
        )
    }


    suspend fun getReportId(request: String?): EvaluationReport {
        val report = request.let {
            if (it != null) {
                evaluationReportRepository.findById(it).awaitFirstOrNull()
            }else evaluationReportRepository.save(EvaluationReport()).awaitSingle()
        }?:evaluationReportRepository.save(EvaluationReport()).awaitSingle()
        if (request == null || request != report.reportId) {
            log.debug("Created new EvaluationReport '${report.reportId}'")
        } else {
            log.debug("Reusing existing EvaluationReport '${report.reportId}'")
        }
        return report
    }

    suspend fun evaluationResults(report: EvaluationReport, evaluationRequest: EvaluationRequest): List<Evaluation> {
        val evaluators = evaluationRequest.evaluationParams as? List<String> ?: emptyList()

        val evaluations = evaluators.mapIndexed { index, evaluation ->
            Evaluation(
                result = ResultTestEnum.FAIL,
                details = "Auto-generated evaluation " + evaluation,
                title = "Testing",
                reportId = report.reportId
            )
        }
        val savedEvaluations = evaluations.map { resultEvaluationResultRepository.save(it).awaitSingle() }
        val updateReport = report.copy(
            evaluations = report.evaluations + savedEvaluations.mapNotNull { it.evaluationId }
        )
        evaluationReportRepository.save(updateReport).awaitSingle()
        return (savedEvaluations)
    }


    suspend fun getEvaluations(): List<Evaluation> {
        val evaluations = resultEvaluationResultRepository.findAll().asFlow().toList()
        return evaluations
    }


    suspend fun getFullReport(reportId: String): EvaluationReportResponse? {
        val report = evaluationReportRepository.findById(reportId).awaitFirstOrNull()?: run {
            log.warn("Report '$reportId' not found")
            throw ResourceNotFoundException("There is exist report with the id $reportId")
        }
        val evaluations = report.let{ resultEvaluationResultRepository.findByReportId(reportId).asFlow().toList() }
        return EvaluationReportResponse(
            report = report.toInfo(),
            evaluations = evaluations.map { it.toResponse() }
        )
    }


    suspend fun gatewayBenchmarkEvaluationService(file: FilePart, benchmarkId: String, reportId: String?): List<Evaluation> {
        try {
            val report = getReportId(reportId)
            if (report.reportId != null) {
                val reportIdentifier = report.reportId
                log.debug("Running benchmark '$benchmarkId' for report '$reportIdentifier'")
                val maDMP = fileToJsonObject(file) // Translate a json file to json object
                val benchmark = benchmarkService.getBenchmarkDetail(benchmarkId)
                    val evaluations = evaluationService.generateTestsResultsFromBenchmark(benchmark, maDMP, reportIdentifier.toString())
                    val savedEvaluations = evaluations.map { resultEvaluationResultRepository.save(it).awaitSingle() }
                    val updateReport = report.copy(
                        evaluations = report.evaluations + savedEvaluations.mapNotNull { it.evaluationId }
                    )
                    evaluationReportRepository.save(updateReport).awaitSingle()
                    log.info("Benchmark '$benchmarkId' produced ${savedEvaluations.size} evaluation(s) for report '$reportIdentifier'")
                    return savedEvaluations

            }else throw ResourceNotFoundException("Not found the report to associated the evaluations")
        }catch (e: Exception) {
            log.error("Failed to generate benchmark evaluation for benchmark '$benchmarkId'", e)
            throw ResourceNotFoundException("Was not possible to generate the evaluation due $e")
        }
    }

    suspend fun gatewayTestsEvaluationService(file: FilePart, testId: String, reportId: String?): Evaluation? {
        try {
            val report = getReportId(reportId)
            if (report.reportId != null) {
                val reportIdentifier = report.reportId
                log.debug("Running test '$testId' for report '$reportIdentifier'")
                val maDMP = fileToJsonObject(file) // Translate a json file to json object
                val test = testService.getTest(testId)
                val evaluation = evaluationService.generateTestResultFromTest(test, maDMP, reportIdentifier.toString())
                if (evaluation != null) {
                    val savedEvaluation = evaluation.let { resultEvaluationResultRepository.save(it).awaitSingle() }
                    val updateReport = report.copy(
                        evaluations = report.evaluations + listOfNotNull(savedEvaluation?.evaluationId)
                    )
                    evaluationReportRepository.save(updateReport).awaitSingle()
                    log.info("Test '$testId' completed with result '${savedEvaluation?.result}' for report '$reportIdentifier'")
                    return savedEvaluation
                }else throw ApiException("There is a problem in the execution of the test $testId",)
            }else throw ResourceNotFoundException("Not found the report to associated the evaluations")
        }catch (e: Exception) {
            log.error("Failed to generate test evaluation for test '$testId'", e)
            throw ResourceNotFoundException("Was not possible to generate the evaluation due $e")
        }
    }

    suspend fun gatewayBenchmarkEvaluationJsonLD(file: FilePart, benchmarkId: String, reportId: String?): TestResultSetJsonLD {
        log.debug("Running benchmark '$benchmarkId' (JSON-LD) for report '$reportId'")
        val evaluations = gatewayBenchmarkEvaluationService(file, benchmarkId, reportId)
        val benchmark = benchmarkService.getBenchmarkDetail(benchmarkId)
        val effectiveReportId = evaluations.firstOrNull()?.reportId
            ?: reportId
            ?: "urn:dmpEvaluationService:unknown"
        return evaluationService.buildTestResultSetJsonLD(evaluations, benchmark.title, effectiveReportId)
    }

    suspend fun fileToJsonObject(file: FilePart): JsonObject {
        val content = file.content()
            .map { dataBuffer -> dataBuffer.toByteBuffer().array().decodeToString() }
            .reduce { acc, text -> acc + text }
            .awaitFirst()

        val original = Json.parseToJsonElement(content).jsonObject


        val extension = file.filename().substringAfterLast('.', "").lowercase()
        val fileName = file.filename()

        return buildJsonObject {
            original.forEach { (key, value) ->
                put(key, value)
            }
            put("fileExtension", JsonPrimitive(extension))
            put("fileName", JsonPrimitive(fileName))
        }
    }

    fun jsonFilevalidator(file: FilePart){
        val filename = file.filename().lowercase()
        if (!filename.endsWith(".json")) {
            throw InputvalidationException("Invalid file type: $filename. Only .json files are allowed.")
        }
    }

}
