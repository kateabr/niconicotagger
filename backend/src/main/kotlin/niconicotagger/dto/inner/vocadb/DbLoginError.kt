package niconicotagger.dto.inner.vocadb

import org.springframework.http.ResponseCookie
import org.springframework.util.MultiValueMap

sealed interface DbLoginResponse

data class DbLoginError(val errors: Map<String, List<String>>, val status: Int) : DbLoginResponse

data class DbLoginSuccess(val cookies: MultiValueMap<String, ResponseCookie>) : DbLoginResponse
