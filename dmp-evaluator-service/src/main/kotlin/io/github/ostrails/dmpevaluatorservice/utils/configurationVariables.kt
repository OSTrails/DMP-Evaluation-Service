package io.github.ostrails.dmpevaluatorservice.utils

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "dmp.global")
class ConfigurationGlobalVariables(
    var repository: String = "",
    var mitLicense: String = "",
    var unpayWallEndPoint: String = "",
    var unpayWallEmail: String = ""
) {}

@Component
@ConfigurationProperties(prefix = "dmp.test")
class ConfigurationTestVariables(
    var endpointURL: String = "",
) {}

@Component
@ConfigurationProperties(prefix = "dmp.metric")
class ConfigurationMetricVariables(
    var endpointURL: String = "",
) {}

@Component
@ConfigurationProperties(prefix = "dmp.benchmark")
class ConfigurationBenchmarkVariables(
    var endpointURL: String = "",
) {}


