package io.github.ostrails.dmpevaluatorservice.utils.dbpatchers

import io.github.ostrails.dmpevaluatorservice.database.model.BenchmarkRecord
import jakarta.annotation.PostConstruct
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.stereotype.Component


@Component
class PostConstruct(
    private val mongoTemplate: MongoTemplate
) {
    private val log: Logger = LoggerFactory.getLogger(PostConstruct::class.java)

    @PostConstruct
    fun run() {
        log.info("Mongo patcher starting up...")
        patchMongoCollection<BenchmarkRecord>(mongoTemplate, "benchmarks")
        patchMongoCollection<BenchmarkRecord>(mongoTemplate, "metrics")
        patchMongoCollection<BenchmarkRecord>(mongoTemplate, "tests")
        migrateIndeterminatedResultTypo(mongoTemplate)
    }
}