package io.github.ostrails.dmpevaluatorservice

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class DmpEvaluatorServiceApplication{
    companion object {
        val logger: Logger = LoggerFactory.getLogger(DmpEvaluatorServiceApplication::class.java)
    }
}

fun main(args: Array<String>) {
    DmpEvaluatorServiceApplication.logger.debug("Starting DMP Evaluator Service")
    runApplication<DmpEvaluatorServiceApplication>(*args)
}
