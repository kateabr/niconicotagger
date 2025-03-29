package niconicotagger.dto.api.request

import niconicotagger.dto.api.misc.ApiType
import niconicotagger.dto.inner.vocadb.VocaDbTag

interface TagDeletionRequest {
    val apiType: ApiType
    val entryId: Long
    val tags: List<VocaDbTag>
}
