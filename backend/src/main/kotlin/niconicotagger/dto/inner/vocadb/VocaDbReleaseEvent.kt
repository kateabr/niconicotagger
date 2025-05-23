package niconicotagger.dto.inner.vocadb

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import java.net.URI
import java.time.Instant
import niconicotagger.dto.inner.misc.ReleaseEventCategory
import niconicotagger.dto.inner.misc.WebLink
import niconicotagger.serde.VocaDbTagDeserializer

data class VocaDbReleaseEvent(
    val id: Long,
    val date: Instant?,
    val endDate: Instant?,
    val name: String,
    val category: ReleaseEventCategory,
    val webLinks: List<WebLink> = emptyList(),
    @JsonDeserialize(contentUsing = VocaDbTagDeserializer::class) val tags: List<VocaDbTag> = emptyList(),
    val seriesId: Long?,
    val series: VocaDbReleaseEventSeries?,
    val mainPicture: MainPicture?,
)

data class MainPicture(val urlOriginal: URI)
