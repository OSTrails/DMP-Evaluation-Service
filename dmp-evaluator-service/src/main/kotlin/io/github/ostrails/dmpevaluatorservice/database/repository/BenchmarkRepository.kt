package io.github.ostrails.dmpevaluatorservice.database.repository

import io.github.ostrails.dmpevaluatorservice.database.model.BenchmarkRecord
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import reactor.core.publisher.Mono

interface BenchmarkRepository: ReactiveMongoRepository<BenchmarkRecord, String>  {
    fun findByTitle(title: String): Mono<BenchmarkRecord>
}