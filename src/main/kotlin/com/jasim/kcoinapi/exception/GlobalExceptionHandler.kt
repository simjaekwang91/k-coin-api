package com.jasim.kcoinapi.exception

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import com.fasterxml.jackson.annotation.JsonFormat
import java.time.OffsetDateTime
import java.time.ZoneId

@RestControllerAdvice
class GlobalExceptionHandler {
    companion object {
        private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)
    }

    @ExceptionHandler(Exception::class)
    fun handleAll(exception: Exception): ResponseEntity<ApiError> {
        logger.error(exception.message, exception)
        val status = HttpStatus.INTERNAL_SERVER_ERROR

        return ResponseEntity.status(status).body(
            ApiError(errorCode = "UnHandled", errorMessage = exception.message)
        )
    }

    @ExceptionHandler(DBException::class)
    fun dbExceptionHandler(exception: DBException): ResponseEntity<ApiError> {
        logger.error(exception.message, exception)
        exception.errorType?.let { logger.error(it.errorMessage, exception) }
        val status = HttpStatus.INTERNAL_SERVER_ERROR

        return ResponseEntity.status(status).body(
            ApiError(errorCode = "DB_ERROR", errorMessage = exception.message)
        )
    }

    data class ApiError(
        val errorCode: String,
        val errorMessage: String? = null,
        @field:JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            timezone = "Asia/Seoul"
        )
        val timestamp: OffsetDateTime = OffsetDateTime.now(ZoneId.of("Asia/Seoul"))
    )
}