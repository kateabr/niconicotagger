package niconicotagger.dto.api.response

import niconicotagger.dto.api.misc.SongEntryByVocaDbTagForEvent
import niconicotagger.dto.inner.misc.SongType

data class SongsByVocaDbEventTagResponse(
    override val items: List<SongEntryByVocaDbTagForEvent>,
    val songTypeStats: Map<SongType, Int>,
    override val totalCount: Long
) : ApiResponse<SongEntryByVocaDbTagForEvent>
