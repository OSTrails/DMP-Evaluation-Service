package io.github.ostrails.dmpevaluatorservice.service

import io.github.ostrails.dmpevaluatorservice.database.model.Evaluation
import io.github.ostrails.dmpevaluatorservice.database.model.EvaluationReport
import io.github.ostrails.dmpevaluatorservice.database.model.TestRecord
import io.github.ostrails.dmpevaluatorservice.database.repository.BenchmarkRepository
import io.github.ostrails.dmpevaluatorservice.database.repository.EvaluationReportRepository
import io.github.ostrails.dmpevaluatorservice.database.repository.EvaluationResultRepository
import io.github.ostrails.dmpevaluatorservice.exceptionHandler.ResourceNotFoundException
import io.github.ostrails.dmpevaluatorservice.model.EvaluationReportResponse
import io.github.ostrails.dmpevaluatorservice.model.EvaluationRequest
import io.github.ostrails.dmpevaluatorservice.model.EvaluationResult
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service

@Service
class EvaluationManagerService(
    private val resultEvaluationResultRepository: EvaluationResultRepository,
    private val evaluationReportRepository: EvaluationReportRepository,
    private val benchmarkRepository: BenchmarkRepository,
    private val benchmarService: BenchmarService,
    private val evaluationService: EvaluationService
) {

    suspend fun generateEvaluations(request: EvaluationRequest): EvaluationResult {
        // fetch the report id from the request or from the db.
        val reportEvaluation = getReportId(request)
        val evaluationsResults = evaluationResults(reportEvaluation, request)
        return EvaluationResult(
            reportId = reportEvaluation.reportId.toString(),
            evaluations = evaluationsResults
        )
    }

    /*
    * Funtion to fecth or create the report for the evaluations
    * */
    suspend fun getReportId(request: EvaluationRequest): EvaluationReport {
        val report = request.reportId?.let {
            evaluationReportRepository.findById(it).awaitFirstOrNull()
        }?:evaluationReportRepository.save(EvaluationReport()).awaitSingle()
        return report
    }

    /*
    * Function to generate the evaluations for a specific request
    * */
    suspend fun evaluationResults(report: EvaluationReport, evaluationRequest: EvaluationRequest): List<Evaluation> {
        val evaluators = evaluationRequest.evaluationParams as? List<String> ?: emptyList()

        val evaluations = evaluators.mapIndexed { index, evaluation ->
            Evaluation(
                result = (1..10).random(),
                details = "Auto-generated evaluation " + evaluation,
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

    /*
    * Funtion to return all the evaluations
    * */
    suspend fun getEvaluations(): List<Evaluation> {
        val evaluations = resultEvaluationResultRepository.findAll().asFlow().toList()
        return evaluations
    }

    /*
    * Function to generate the evaluation report with all the evlauations
    * */
    suspend fun getFullreport(reportId: String): EvaluationReportResponse? {
        val report = evaluationReportRepository.findById(reportId).awaitFirstOrNull()?: throw ResourceNotFoundException("There is exist report with the id $reportId")
        val evaluations = report.let{ resultEvaluationResultRepository.findByReportId(reportId).asFlow().toList() } ?: emptyList()
        return EvaluationReportResponse(
            report= report,
            evaluations = evaluations
        )
    }

    suspend fun gatewayEvaluationService(file: FilePart, benchmarkTitle: String): List<TestRecord> {
        val file = fileToJsonObject(file)
        System.out.println("------------------------------------- $benchmarkTitle")
        val benchmark = benchmarService.getbenchmarkByTitle(benchmarkTitle)
        println("benchmark finding --------------------------- $benchmarkTitle")
        val tests = evaluationService.testsToExecute(benchmark)
        //val metrics:
        return tests
    }

    suspend fun fileToJsonObject(file: FilePart): JsonObject {
        val content = file.content()
            .map { dataBuffer -> dataBuffer.toByteBuffer().array().decodeToString() }
            .reduce { acc, text -> acc + text }
            .awaitFirst()
        return Json.parseToJsonElement(content).jsonObject
    }



}