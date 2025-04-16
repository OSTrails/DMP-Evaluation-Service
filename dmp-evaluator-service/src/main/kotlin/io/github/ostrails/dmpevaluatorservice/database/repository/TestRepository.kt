package io.github.ostrails.dmpevaluatorservice.database.repository

import io.github.ostrails.dmpevaluatorservice.database.model.TestRecord
import org.springframework.data.mongodb.repository.ReactiveMongoRepository

interface TestRepository: ReactiveMongoRepository<TestRecord, String> {
}
