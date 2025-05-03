package niconicotagger.dto.api.response

import niconicotagger.dto.api.misc.VocaDbSongEntryWithPvs
import niconicotagger.dto.inner.misc.SongType

data class SongsWithPvsResponse(
    override val items: List<VocaDbSongEntryWithPvs>,
    val songTypeStats: Map<SongType, Int>,
    override val totalCount: Long,
) : ApiResponse<VocaDbSongEntryWithPvs>
