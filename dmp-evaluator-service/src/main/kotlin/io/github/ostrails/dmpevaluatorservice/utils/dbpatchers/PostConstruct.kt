package io.github.ostrails.dmpevaluatorservice.utils.dbpatchers

import io.github.ostrails.dmpevaluatorservice.database.model.BenchmarkRecord
import jakarta.annotation.PostConstruct
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.stereotype.Component


@Component
class PostConstruct(
    private val mongoTemplate: MongoTemplate
) {
    @PostConstruct
    fun run() {
        println("🚀 Mongo patcher starting up...")
        patchMongoCollection<BenchmarkRecord>(mongoTemplate, "benchmarks")
        patchMongoCollection<BenchmarkRecord>(mongoTemplate, "metrics")
        patchMongoCollection<BenchmarkRecord>(mongoTemplate, "tests")
    }
}