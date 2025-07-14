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
import io.github.ostrails.dmpevaluatorservice.utils.madmp2rdf.ToRDFService
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.serialization.json.*
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service

@Service
class EvaluationManagerService(
    private val resultEvaluationResultRepository: EvaluationResultRepository,
    private val evaluationReportRepository: EvaluationReportRepository,
    private val benchmarService: BenchmarService,
    private val evaluationService: EvaluationService,
    private val toRDFService: ToRDFService,
    private val testService: TestService
) {

    suspend fun generateEvaluations(request: EvaluationRequest): EvaluationResult {
        // fetch the report id from the request or from the db.
        val reportEvaluation = getReportId(request.reportId)
        val evaluationsResults = evaluationResults(reportEvaluation, request)
        return EvaluationResult(
            reportId = reportEvaluation.reportId.toString(),
            evaluations = evaluationsResults
        )
    }


    suspend fun getReportId(request: String?): EvaluationReport {
        val report = request.let {
            if (it != null) {
                evaluationReportRepository.findById(it).awaitFirstOrNull()
            }else evaluationReportRepository.save(EvaluationReport()).awaitSingle()
        }?:evaluationReportRepository.save(EvaluationReport()).awaitSingle()
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
            evaluations = report.evaluations + savedEvaluations.map { it.evaluationId }
        )
        evaluationReportRepository.save(updateReport).awaitSingle()
        return (savedEvaluations)
    }


    suspend fun getEvaluations(): List<Evaluation> {
        val evaluations = resultEvaluationResultRepository.findAll().asFlow().toList()
        return evaluations
    }


    suspend fun getFullReport(reportId: String): EvaluationReportResponse? {
        val report = evaluationReportRepository.findById(reportId).awaitFirstOrNull()?: throw ResourceNotFoundException("There is exist report with the id $reportId")
        val evaluations = report.let{ resultEvaluationResultRepository.findByReportId(reportId).asFlow().toList() }
        return EvaluationReportResponse(
            report= report,
            evaluations = evaluations
        )
    }


    suspend fun gatewayBenchmarkEvaluationService(file: FilePart, benchmarkTitle: String, reportId: String?): List<Evaluation> {
        try {
            val report = getReportId(reportId)
            if (report.reportId != null) {
                val reportIdentifier = report.reportId
                //jsonFilevalidator(file)
                val maDMP = fileToJsonObject(file) // Translate a json file to json object
                val benchmark = benchmarService.benchmarkByTitle(benchmarkTitle)
                    val evaluations = evaluationService.generateTestsResultsFromBenchmark(benchmark, maDMP, reportIdentifier.toString())
                    val savedEvaluations = evaluations.map { resultEvaluationResultRepository.save(it).awaitSingle() }
                    val updateReport = report.copy(
                        evaluations = report.evaluations + savedEvaluations.map { it.evaluationId }
                    )
                    evaluationReportRepository.save(updateReport).awaitSingle()
                    //TODO()
                    // here I´m going to call the function that can trigger the evaluations for each plugin evaluator based on the test.evaluator and test.function.
                    return savedEvaluations

            }else throw ResourceNotFoundException("Not found the report to associated the evaluations")
        }catch (e: Exception) {
            throw ResourceNotFoundException("Was not possible to generate the evaluation due $e")
        }
    }

    suspend fun gatewayTestsEvaluationService(file: FilePart, testId: String, reportId: String?): Evaluation? {
        try {
            val report = getReportId(reportId)
            if (report.reportId != null) {
                val reportIdentifier = report.reportId
                //jsonFilevalidator(file)
                val maDMP = fileToJsonObject(file) // Translate a json file to json object
                val test = testService.getTest(testId)
                val evaluation = evaluationService.generateTestResultFromTest(test, maDMP, reportIdentifier.toString())
                if (evaluation != null) {
                    val savedEvaluation = evaluation.let { resultEvaluationResultRepository.save(it).awaitSingle() }
                    val updateReport = report.copy(
                        evaluations = report.evaluations + (savedEvaluation?.evaluationId)
                    )
                    evaluationReportRepository.save(updateReport).awaitSingle()
                    return savedEvaluation
                }else throw ApiException("There is a problem in the execution of the test $testId",)
                //TODO()
                // here I´m going to call the function that can trigger the evaluations for each plugin evaluator based on the test.evaluatzor and test.function.
            }else throw ResourceNotFoundException("Not found the report to associated the evaluations")
        }catch (e: Exception) {
            throw ResourceNotFoundException("Was not possible to generate the evaluation due $e")
        }
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

    suspend fun mapToRDF(maDMP: FilePart): Any {
        toRDFService.jsonToRDF(maDMP.toString())
        return true
    }

    fun jsonFilevalidator(file: FilePart){
        val filename = file.filename().lowercase()
        if (!filename.endsWith(".json")) {
            throw InputvalidationException("Invalid file type: $filename. Only .json files are allowed.")
        }
    }

}