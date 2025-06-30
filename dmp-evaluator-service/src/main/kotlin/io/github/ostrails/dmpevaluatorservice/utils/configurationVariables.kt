package io.github.ostrails.dmpevaluatorservice.utils

import com.mysql.cj.LicenseConfiguration
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "dmp.test")
class ConfigurationTestVariables(
    var endpointURL: String = "",
    var repository: String = "",
    var mitLicense: String = "",
) {}

