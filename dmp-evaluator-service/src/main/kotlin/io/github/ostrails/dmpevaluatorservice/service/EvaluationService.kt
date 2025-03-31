package io.github.ostrails.dmpevaluatorservice.service

import io.github.ostrails.dmpevaluatorservice.model.EvaluationResult
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class EvaluationService {

    fun evaluate(maDMP: Map<String, Any>): EvaluationResult {
        //Here check the evaluation Params and call the specific evalutors
        return EvaluationResult(
            dmpId = maDMP["dmpId"] as String,
            isValid = true,
            messages = listOf("Tetsing"),
            timeStamp = LocalDateTime.now()
        )
    }

    fun getEvaluations(dmpId: String): List<EvaluationResult> {
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