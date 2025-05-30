package niconicotagger.dto.api.request

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import niconicotagger.dto.api.misc.ApiType
import niconicotagger.dto.api.misc.ClientType
import niconicotagger.dto.inner.vocadb.VocaDbTag

@JsonIgnoreProperties(ignoreUnknown = false)
data class DeleteTagsRequestWrapper(@field:Valid val request: DeleteTagsRequest, val clientType: ClientType)

data class DeleteTagsRequest(
    override val apiType: ApiType,
    override val entryId: Long,
    @field:NotEmpty @field:Valid override val tags: List<VocaDbTag>,
) : TagDeletionRequest
