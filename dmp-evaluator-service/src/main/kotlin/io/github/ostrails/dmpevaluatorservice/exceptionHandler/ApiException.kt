package io.github.ostrails.dmpevaluatorservice.exceptionHandler

// This is the base class for custom exceptions
open class ApiException(
    message: String,
    cause: Throwable? = null
): RuntimeException(message, cause)
