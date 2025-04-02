package io.github.ostrails.dmpevaluatorservice.service

import io.github.ostrails.dmpevaluatorservice.database.model.Evaluation
import io.github.ostrails.dmpevaluatorservice.database.model.EvaluationReport
import io.github.ostrails.dmpevaluatorservice.database.repository.EvaluationReportRepository
import io.github.ostrails.dmpevaluatorservice.database.repository.EvaluationResultRepository
import io.github.ostrails.dmpevaluatorservice.model.EvaluationResult
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.*

@Service
class EvaluationService(
    private val resultEvaluationResultRepository: EvaluationResultRepository,
    private val evaluationReportRepository: EvaluationReportRepository
) {

    fun evaluate(maDMP: Map<String, Any>): Evaluation {
        val reportId = reportId(maDMP)
        return resultEvaluationResultRepository.save(Evaluation(
            completenessScore = 6,
            details = "testing",
            evaluationId = UUID.randomUUID().toString(),
            reportId = reportId.toString(),
            )
        )
    }


    private fun reportId (parameters: Map<String, Any>): Any? {
        if (parameters["dmpReport"] != null) {
            return parameters["dmpReport"]
        }else {
            val newReportId =EvaluationReport(
                reportId = UUID.randomUUID().toString(),
                dmpId = parameters["dmpId"] as String,
                evaluations =  mutableListOf()
            )
            return evaluationReportRepository.save(newReportId).reportId
        }
    }


        //Here check the evaluation Params and call the specific evalutors
//        return repository.save(
//            Evaluation(
//                dmpReport = maDMP["dmpReport"] as String,
//                completenessScore = 5,
//            ))
        //    }

//    fun getEvaluations(): List<Evaluation> {
//        // retrieve all the evaluation ofr a specific report
//        return repository.findAll()
//    }

    fun getEvaluation(dmpId: String): List<EvaluationResult> {
        // retrieve all the evaluation ofr a specific report
        return listOf(
            EvaluationResult(
                dmpId = dmpId,
                isValid = true,
                messages = listOf("Tetsing"),
                timeStamp = LocalDateTime.now())
        )
    }
}