package niconicotagger.dto.api.request

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import jakarta.validation.Valid
import niconicotagger.dto.api.misc.ClientType
import niconicotagger.dto.api.misc.PvToDisable

@JsonIgnoreProperties(ignoreUnknown = false)
data class SongTagsAndPvsMassUpdateRequest(
    @field:Valid override val subRequests: List<SongTagsAndPvsUpdateRequest>,
    override val clientType: ClientType,
) : MassUpdateRequest<SongTagsAndPvsUpdateRequest>

data class SongTagsAndPvsUpdateRequest(
    val songId: Long,
    val tags: List<Long>,
    @field:Valid val nndPvsToDisable: Set<PvToDisable>,
)
