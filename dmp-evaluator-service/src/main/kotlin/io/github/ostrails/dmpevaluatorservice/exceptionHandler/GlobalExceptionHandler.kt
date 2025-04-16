package io.github.ostrails.dmpevaluatorservice.exceptionHandler

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ServerWebExchange


@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleNotFound(ex: ResourceNotFoundException, exchange: ServerWebExchange): ErrorResponse{
        return ErrorResponse(
            code = "NOT_FOUND",
            message = ex.message.orEmpty(),
            path = exchange.request.path.toString()
        )
    }

    @ExceptionHandler(DatabaseException::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleDatabaseError(ex: DatabaseException, exchange: ServerWebExchange): ErrorResponse{
        return ErrorResponse(
            code = "DATABASE ERROR",
            message = ex.message.orEmpty(),
            path = exchange.request.path.toString()
        )
    }

    @ExceptionHandler(ApiException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleAGenericApi(ex: ApiException, exchange: ServerWebExchange): ErrorResponse{
        return ErrorResponse(
            code = "API_ERROR",
            message = ex.message.orEmpty(),
            path = exchange.request.path.toString()
        )
    }

    @ExceptionHandler(Exception::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleUnknown(ex: Exception, exchange: ServerWebExchange): ErrorResponse{
        return ErrorResponse(
            code = "UNEXPECTED_ERROR",
            message = ex.message.orEmpty(),
            path = exchange.request.path.toString()
        )
    }


}