package niconicotagger.dto.inner.vocadb

import niconicotagger.dto.inner.misc.ReleaseEventCategory
import niconicotagger.dto.inner.misc.WebLink

data class VocaDbReleaseEventSeries(
    val id: Long,
    val category: ReleaseEventCategory,
    val webLinks: List<WebLink> = emptyList(),
)
