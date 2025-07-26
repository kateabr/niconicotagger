package niconicotagger.controller

import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import java.time.Duration
import niconicotagger.client.DbClientHolder
import niconicotagger.dto.api.misc.SupportedDatabase
import niconicotagger.dto.api.request.LoginRequest
import org.springframework.boot.web.server.Cookie.SameSite.STRICT
import org.springframework.http.HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS
import org.springframework.http.HttpHeaders.SET_COOKIE
import org.springframework.http.ResponseCookie
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(value = ["/api/authorize"])
@Validated
class AuthorizationController(private val dbClientHolder: DbClientHolder) {

    @PostMapping
    suspend fun login(@RequestBody @Valid request: LoginRequest, response: HttpServletResponse) {
        val cookie = dbClientHolder.getClient(request.clientType).login(request.userName, request.password)
        response.setHeader(
            SET_COOKIE,
            ResponseCookie.from(cookie.keys.first(), cookie.values.first()[0])
                .httpOnly(true)
                .secure(true)
                .sameSite(STRICT.attributeValue())
                .maxAge(Duration.ofDays(1))
                .build()
                .toString(),
        )
        response.setHeader(ACCESS_CONTROL_ALLOW_CREDENTIALS, "true")
    }

    @GetMapping("/databases")
    suspend fun getSupportedDatabases(): List<SupportedDatabase> = dbClientHolder.getSupportedDatabases()
}
