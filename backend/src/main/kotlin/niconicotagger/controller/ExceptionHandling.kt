package niconicotagger.controller

import niconicotagger.constants.Constants.COOKIE_HEADER_KEY
import niconicotagger.dto.VocaDbLoginException
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MissingRequestCookieException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.zalando.problem.Problem
import org.zalando.problem.Status
import org.zalando.problem.spring.web.advice.AdviceTrait
import org.zalando.problem.spring.web.advice.ProblemHandling

@RestControllerAdvice
class ExceptionHandling : ProblemHandling, AdviceTrait {
    @ExceptionHandler
    fun handleVocadbLoginException(
        exception: VocaDbLoginException,
        request: NativeWebRequest,
    ): ResponseEntity<Problem> {
        val message =
            exception.errorData.errors[""]?.joinToString(", ")
                ?: exception.errorData.errors.entries
                    .filter { it.key.isNotBlank() }
                    .joinToString("; ") { "${it.key}: ${it.value}" }
        return defaultHandler(exception, HttpStatusCode.valueOf(exception.errorData.status), message, request)
    }

    @ExceptionHandler
    fun handleWebClientResponseException(
        exception: WebClientResponseException,
        request: NativeWebRequest,
    ): ResponseEntity<Problem> = defaultHandler(exception, exception.statusCode, exception.message, request)

    @ExceptionHandler
    fun handleMissingCookieException(
        exception: MissingRequestCookieException,
        request: NativeWebRequest,
    ): ResponseEntity<Problem> =
        defaultHandler(
            exception,
            exception.statusCode,
            if (exception.cookieName == COOKIE_HEADER_KEY) "Authentication cookie has expired" else exception.message,
            request,
        )

    private fun defaultHandler(
        exception: Throwable,
        statusCode: HttpStatusCode,
        message: String,
        request: NativeWebRequest,
    ): ResponseEntity<Problem> =
        this.create(exception, Problem.valueOf(Status.valueOf(statusCode.value()), message), request)
}
