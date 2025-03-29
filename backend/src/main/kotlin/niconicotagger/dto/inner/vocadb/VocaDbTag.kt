package niconicotagger.dto.inner.vocadb

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.validation.constraints.NotBlank

data class VocaDbTag(
    override val id: Long,
    @field:NotBlank
    override val name: String
) : DatabaseEntity {
    @JsonIgnore
    override fun getPathSegment() = "T"
}

data class VocaDbTagSelectable(
    val tag: VocaDbTag,
    val selected: Boolean
)
