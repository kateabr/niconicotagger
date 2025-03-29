package niconicotagger.dto.api.request

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import jakarta.validation.constraints.NotBlank
import niconicotagger.dto.api.misc.ClientType

@JsonIgnoreProperties(ignoreUnknown = false)
data class LoginRequest(
    @field:NotBlank val userName: String,
    @field:NotBlank val password: String,
    val clientType: ClientType
)
