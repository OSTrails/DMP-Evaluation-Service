package io.github.ostrails.dmpevaluatorservice.ratelimit.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "ratelimit")
class RateLimitProperties(
    var enabled: Boolean = true,
    var capacity: Long = 100,
    var refillTokens: Long = 100,
    var refillDurationSeconds: Long = 60,
) {}
