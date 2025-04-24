package io.github.ostrails.dmpevaluatorservice.database.repository

import io.github.ostrails.dmpevaluatorservice.database.model.MetricRecord
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import reactor.core.publisher.Flux

interface MetricRepository: ReactiveMongoRepository<MetricRecord, String> {
    fun findByIdIn(ids: List<String>): Flux<MetricRecord>
}