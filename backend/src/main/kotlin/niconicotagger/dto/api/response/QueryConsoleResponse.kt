package niconicotagger.dto.api.response

import niconicotagger.dto.api.misc.QueryConsoleData
import niconicotagger.dto.inner.vocadb.VocaDbTag

data class QueryConsoleResponse<T : QueryConsoleData>(
    override val items: List<T>,
    val tagPool: List<VocaDbTag>,
    override val totalCount: Long
) : ApiResponse<T>
