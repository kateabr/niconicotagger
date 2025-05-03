package niconicotagger.dto.api.request

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import jakarta.validation.constraints.NotBlank
import niconicotagger.dto.api.misc.ClientType

@JsonIgnoreProperties(ignoreUnknown = false)
data class GetReleaseEventRequest(@field:NotBlank val eventName: String, val clientType: ClientType)
