package io.github.ostrails.dmpevaluatorservice.database.repository

import io.github.ostrails.dmpevaluatorservice.database.model.Evaluation
import io.github.ostrails.dmpevaluatorservice.database.model.EvaluationReport
import org.springframework.data.mongodb.repository.MongoRepository

interface EvaluationReportRepository: MongoRepository<EvaluationReport, String> {}

interface EvaluationResultRepository: MongoRepository<Evaluation, String> {
    fun findByReportId(reportId: String): List<Evaluation>
}