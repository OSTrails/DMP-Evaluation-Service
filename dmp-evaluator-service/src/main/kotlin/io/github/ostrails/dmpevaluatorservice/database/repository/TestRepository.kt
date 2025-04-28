package io.github.ostrails.dmpevaluatorservice.database.repository

import io.github.ostrails.dmpevaluatorservice.database.model.TestRecord
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import reactor.core.publisher.Flux

interface TestRepository: ReactiveMongoRepository<TestRecord, String> {
    fun findByIdIn(ids: List<String>): Flux<TestRecord>

    fun findBymetricImplemented(implemented: String): Flux<TestRecord>
}
