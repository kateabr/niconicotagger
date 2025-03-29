package niconicotagger.dto.api.request

import niconicotagger.dto.api.misc.ClientType

interface MassUpdateRequest<T> {
    val subRequests: List<T>
    val clientType: ClientType
}
