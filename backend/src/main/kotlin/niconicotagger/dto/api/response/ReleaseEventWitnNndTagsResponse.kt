package niconicotagger.dto.api.response

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import niconicotagger.dto.inner.misc.ReleaseEventCategory
import niconicotagger.dto.inner.vocadb.VocaDbTag
import niconicotagger.serde.InstantToLocalDateSerializer
import java.time.Instant

sealed interface ReleaseEventBaseResponse {
    val id: Long
    val date: Instant?
    val endDate: Instant?
    val name: String
    val category: ReleaseEventCategory
}

data class ReleaseEventWitnNndTagsResponse(
    override val id: Long,
    @JsonSerialize(using = InstantToLocalDateSerializer::class)
    override val date: Instant?,
    @JsonSerialize(using = InstantToLocalDateSerializer::class)
    override val endDate: Instant?,
    override val name: String,
    override val category: ReleaseEventCategory,
    val suggestFiltering: Boolean,
    val nndTags: List<String>,
    val seriesId: Long?
) : ReleaseEventBaseResponse

data class ReleaseEventWithVocaDbTagsResponse(
    override val id: Long,
    @JsonSerialize(using = InstantToLocalDateSerializer::class)
    override val date: Instant?,
    @JsonSerialize(using = InstantToLocalDateSerializer::class)
    override val endDate: Instant?,
    override val name: String,
    override val category: ReleaseEventCategory,
    val vocaDbTags: List<VocaDbTag>
) : ReleaseEventBaseResponse
