package niconicotagger.dto.api.request

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import jakarta.validation.Valid
import niconicotagger.dto.api.misc.ClientType
import niconicotagger.dto.api.misc.ReleaseEvent

@JsonIgnoreProperties(ignoreUnknown = false)
data class MassAddReleaseEventRequest(
    @field:Valid
    override val subRequests: List<AddReleaseEventRequest>,
    override val clientType: ClientType
) : MassUpdateRequest<AddReleaseEventRequest>

data class AddReleaseEventRequest(
    override val entryId: Long,
    @field:Valid
    override val event: ReleaseEvent
) : ReleaseEventAdditionRequest
