package niconicotagger.dto.api.misc

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import jakarta.validation.constraints.NotBlank
import niconicotagger.dto.inner.vocadb.DatabaseEntity

@JsonIgnoreProperties(ignoreUnknown = false)
data class ReleaseEvent(
    override val id: Long,
    @field:NotBlank
    override val name: String,
    val seriesId: Long?
) : DatabaseEntity {
    @JsonIgnore
    override fun getPathSegment() = "E"
}
