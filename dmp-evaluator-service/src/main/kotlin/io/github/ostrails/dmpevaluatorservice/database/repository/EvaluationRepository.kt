package io.github.ostrails.dmpevaluatorservice.database.repository

import io.github.ostrails.dmpevaluatorservice.database.model.Evaluation
import io.github.ostrails.dmpevaluatorservice.database.model.EvaluationReport
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import reactor.core.publisher.Flux

interface EvaluationReportRepository: ReactiveMongoRepository<EvaluationReport, String> {}

interface EvaluationResultRepository: ReactiveMongoRepository<Evaluation, String> {
    fun findByReportId(reportId: String): Flux<Evaluation>
}