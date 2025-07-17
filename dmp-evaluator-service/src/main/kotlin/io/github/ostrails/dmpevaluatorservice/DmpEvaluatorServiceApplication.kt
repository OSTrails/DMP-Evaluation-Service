package io.github.ostrails.dmpevaluatorservice

import io.github.ostrails.dmpevaluatorservice.plugin.EvaluatorPlugin
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.plugin.core.config.EnablePluginRegistries

@SpringBootApplication
@ComponentScan(basePackages = ["io.github.ostrails"])
@EnablePluginRegistries(EvaluatorPlugin::class)

class DmpEvaluatorServiceApplication{
    companion object {
        val logger: Logger = LoggerFactory.getLogger(DmpEvaluatorServiceApplication::class.java)
    }
}

fun main(args: Array<String>) {
//    DmpEvaluatorServiceApplication.logger.debug("Starting DMP Evaluator Service")
    runApplication<DmpEvaluatorServiceApplication>(*args)
}
