package niconicotagger.dto.api.request

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import jakarta.validation.Valid
import jakarta.validation.constraints.Size
import niconicotagger.dto.api.misc.ApiType.SONGS
import niconicotagger.dto.api.misc.ClientType
import niconicotagger.dto.api.misc.ReleaseEvent
import niconicotagger.dto.inner.vocadb.VocaDbTag

@JsonIgnoreProperties(ignoreUnknown = false)
data class SongTagsAndEventsMassUpdateRequest(
    @field:Valid override val subRequests: List<SongTagsAndEventsUpdateRequest>,
    override val clientType: ClientType,
) : MassUpdateRequest<SongTagsAndEventsUpdateRequest>

data class SongTagsAndEventsUpdateRequest(
    override val entryId: Long,
    @field:Valid @field:Size(min = 1, max = 1) override val tags: List<VocaDbTag>,
    @field:Valid override val event: ReleaseEvent?,
) : TagDeletionRequest, ReleaseEventAdditionRequest {
    override val apiType = SONGS
}
