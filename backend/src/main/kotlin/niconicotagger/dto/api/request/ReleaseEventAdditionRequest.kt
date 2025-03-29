package niconicotagger.dto.api.request

import niconicotagger.dto.api.misc.ReleaseEvent

interface ReleaseEventAdditionRequest {
    val entryId: Long
    val event: ReleaseEvent?
}
