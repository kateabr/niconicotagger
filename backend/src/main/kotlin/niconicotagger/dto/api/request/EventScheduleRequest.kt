package niconicotagger.dto.api.request

import niconicotagger.dto.api.misc.ClientType

data class EventScheduleRequest(val clientType: ClientType, val useCached: Boolean = true)
