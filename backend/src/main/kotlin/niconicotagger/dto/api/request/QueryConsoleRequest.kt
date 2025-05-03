package niconicotagger.dto.api.request

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import niconicotagger.dto.api.misc.ApiType
import niconicotagger.dto.api.misc.ClientType

@JsonIgnoreProperties(ignoreUnknown = false)
data class QueryConsoleRequest(val apiType: ApiType, val query: String, val clientType: ClientType)
