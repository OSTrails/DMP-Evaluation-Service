package io.github.ostrails.dmpevaluatorservice.database.repository

import io.github.ostrails.dmpevaluatorservice.database.model.BenchmarkRecord
import org.springframework.data.mongodb.repository.ReactiveMongoRepository

interface BenchmarkRepository: ReactiveMongoRepository<BenchmarkRecord, String>  {}