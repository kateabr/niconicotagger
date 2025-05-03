package niconicotagger.controller

import niconicotagger.dto.VocaDbLoginException
import org.springframework.http.ResponseEntity
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
        return this.create(exception, Problem.valueOf(Status.valueOf(exception.errorData.status), message), request)
    }

    @ExceptionHandler
    fun handleWebClientResponseException(
        exception: WebClientResponseException,
        request: NativeWebRequest,
    ): ResponseEntity<Problem> {
        return this.create(
            exception,
            Problem.valueOf(Status.valueOf(exception.statusCode.value()), exception.message),
            request,
        )
    }
}
