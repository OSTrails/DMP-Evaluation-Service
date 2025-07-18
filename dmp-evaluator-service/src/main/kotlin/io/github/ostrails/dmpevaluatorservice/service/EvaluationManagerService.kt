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
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.core.io.buffer.DataBufferUtils

import org.eclipse.rdf4j.model.Model
import org.eclipse.rdf4j.model.util.Models
import org.eclipse.rdf4j.rio.RDFFormat
import org.eclipse.rdf4j.rio.Rio
import org.eclipse.rdf4j.repository.sail.SailRepository
import org.eclipse.rdf4j.sail.memory.MemoryStore
import org.eclipse.rdf4j.sail.shacl.ShaclSail
import org.eclipse.rdf4j.model.util.Values
import org.eclipse.rdf4j.model.vocabulary.RDF
import org.eclipse.rdf4j.model.vocabulary.SHACL
import java.io.File
import java.util.UUID

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


    suspend fun gatewayBenchmarkEvaluationService(file: FilePart, benchmarkId: String, reportId: String?): List<Evaluation> {
        try {
            val report = getReportId(reportId)
            if (report.reportId != null) {
                val reportIdentifier = report.reportId
                //jsonFilevalidator(file)
                val maDMP = fileToJsonObject(file) // Translate a json file to json object
                val benchmark = benchmarService.getBenchmarkDetail(benchmarkId)
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
                } else throw ApiException("There is a problem in the execution of the test $testId",)
                //TODO()
                // here I´m going to call the function that can trigger the evaluations for each plugin evaluator based on the test.evaluatzor and test.function.
            } else throw ResourceNotFoundException("Not found the report to associated the evaluations")
        }catch (e: Exception) {
            throw ResourceNotFoundException("Was not possible to generate the evaluation due $e")
        }
    }


    suspend fun shaclValidationService(maDMP: String): List<Evaluation> {
        val baseURI = "https://w3id.org/validation/ns/core#"

        // Parse the maDMP Turtle content into a Model
        val maDMPModel: Model = Rio.parse(maDMP.byteInputStream(), baseURI, RDFFormat.TURTLE)

        val evaluationList = mutableListOf<Evaluation>()

        val shapesDir = File("src/main/resources/shapes")
        val shapeFiles = shapesDir.listFiles { file -> file.extension == "ttl" } ?: emptyArray()

        for (shapeFile in shapeFiles) {
            // Parse shape model
            val shapeModel = Rio.parse(shapeFile.inputStream(), baseURI, RDFFormat.TURTLE)

            // Register the base prefix ":" for prettier output (if not already present)
            shapeModel.setNamespace("", baseURI)
            
            // Extract all shapes (NodeShape or PropertyShape)
            val shapeSubjects = shapeModel
                .filter(null, RDF.TYPE, null)
                .filter { it.`object` == SHACL.NODE_SHAPE || it.`object` == SHACL.PROPERTY_SHAPE }
                .map { stmt ->
                    val subj = stmt.subject
                    when {
                        subj.isIRI -> shapeModel.namespaces.firstOrNull { ns -> subj.stringValue().startsWith(ns.name) }?.let { ns ->
                            val localName = subj.stringValue().removePrefix(ns.name)
                            if (ns.prefix.isEmpty()) ":$localName" else "${ns.prefix}:$localName"
                        } ?: "<${subj.stringValue()}>"
                        else -> subj.stringValue()
                    }
                }
                .distinct()

            // Create a comma-separated list for the detailed report
            val shapeListString = shapeSubjects.joinToString(", ")

            // Create a SHACL-enabled repository
            val shaclSail = ShaclSail(MemoryStore())
            val repo = SailRepository(shaclSail)
            repo.init()

            // Load shapes into the SHACL Sail
            repo.connection.use { conn ->
                conn.begin()
                conn.add(shapeModel)
                conn.commit()

                // Validate the maDMPModel against the loaded shapes
                try {
                    conn.begin()
                    conn.add(maDMPModel)
                    conn.commit()

                    evaluationList.add(
                        Evaluation(
                            evaluationId = UUID.randomUUID().toString(),
                            title = "SHACL Validation with shapes from the file: ${shapeFile.name}",
                            result = ResultTestEnum.PASS,
                            details = "Validation based on the file ${shapeFile.name} which has following shapes: ${shapeListString}",
                            reportId = null,
                        )
                    )
                } catch (e: Exception) {
                    evaluationList.add(
                        Evaluation(
                            evaluationId = UUID.randomUUID().toString(),
                            title = "SHACL Validation for ${shapeFile.name}",
                            result = ResultTestEnum.FAIL,
                            details = "Validation failed: ${e.message}",
                            reportId = null,
                        )
                    )
                }
            }

            repo.shutDown()
        }

        return evaluationList
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

    suspend fun mapToRDF(maDMP: FilePart): String {
        val dataBuffer = DataBufferUtils.join(maDMP.content()).awaitFirstOrNull() ?: throw IllegalArgumentException("Empty file")
        val json = dataBuffer.toString(StandardCharsets.UTF_8)
        DataBufferUtils.release(dataBuffer) 

        val maDMPTurtle = toRDFService.jsonToRDF(json)
        return maDMPTurtle
    }

    fun jsonFilevalidator(file: FilePart){
        val filename = file.filename().lowercase()
        if (!filename.endsWith(".json")) {
            throw InputvalidationException("Invalid file type: $filename. Only .json files are allowed.")
        }
    }

}