package niconicotagger.dto.api.response

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import java.net.URI
import java.time.Instant
import niconicotagger.dto.inner.misc.EventStatus
import niconicotagger.dto.inner.misc.ReleaseEventCategory
import niconicotagger.dto.inner.vocadb.VocaDbTag
import niconicotagger.serde.InstantToLocalDateSerializer

sealed interface ReleaseEventBaseResponse {
    val id: Long
    val date: Instant?
    val endDate: Instant?
    val name: String
    val category: ReleaseEventCategory
}

data class ReleaseEventPreviewResponse(
    override val id: Long,
    @JsonSerialize(using = InstantToLocalDateSerializer::class) override val date: Instant?,
    @JsonSerialize(using = InstantToLocalDateSerializer::class) override val endDate: Instant?,
    override val name: String,
    override val category: ReleaseEventCategory,
    val status: EventStatus,
    val pictureUrl: URI?,
) : ReleaseEventBaseResponse

data class ReleaseEventWitnNndTagsResponse(
    override val id: Long,
    @JsonSerialize(using = InstantToLocalDateSerializer::class) override val date: Instant?,
    @JsonSerialize(using = InstantToLocalDateSerializer::class) override val endDate: Instant?,
    override val name: String,
    override val category: ReleaseEventCategory,
    val suggestFiltering: Boolean,
    val nndTags: List<String>,
    val seriesId: Long?,
) : ReleaseEventBaseResponse

data class ReleaseEventWithVocaDbTagsResponse(
    override val id: Long,
    @JsonSerialize(using = InstantToLocalDateSerializer::class) override val date: Instant?,
    @JsonSerialize(using = InstantToLocalDateSerializer::class) override val endDate: Instant?,
    override val name: String,
    override val category: ReleaseEventCategory,
    val vocaDbTags: List<VocaDbTag>,
) : ReleaseEventBaseResponse
