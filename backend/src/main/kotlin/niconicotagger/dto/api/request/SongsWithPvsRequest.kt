package niconicotagger.dto.api.request

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import jakarta.validation.constraints.Min
import niconicotagger.dto.api.misc.ClientType
import niconicotagger.dto.api.misc.VocaDbSortOrder
import org.hibernate.validator.constraints.Range

@JsonIgnoreProperties(ignoreUnknown = false)
data class SongsWithPvsRequest(
    @field:Min(0) val startOffset: Long,
    @field:Range(min = 10, max = 100) val maxResults: Long,
    val orderBy: VocaDbSortOrder,
    val clientType: ClientType,
)
