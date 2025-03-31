package io.github.ostrails.dmpevaluatorservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class DmpEvaluatorServiceApplication

fun main(args: Array<String>) {
    runApplication<DmpEvaluatorServiceApplication>(*args)
}
