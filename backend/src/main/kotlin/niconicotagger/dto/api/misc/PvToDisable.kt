package niconicotagger.dto.api.misc

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import jakarta.validation.constraints.NotBlank

@JsonIgnoreProperties(ignoreUnknown = false)
data class PvToDisable(@field:NotBlank val id: String, @field:NotBlank val reason: String)
