package niconicotagger.dto.api.request

import niconicotagger.dto.api.misc.ClientType
import org.hibernate.validator.constraints.Range

data class EventScheduleRequest(
    val clientType: ClientType,
    val useCached: Boolean = true,
    @field:Range(min = 1, max = 365) val eventScopeDays: Long? = null,
)
