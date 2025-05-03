package niconicotagger.dto.api.response

import niconicotagger.dto.api.misc.NndVideoWithAssociatedVocaDbEntry
import niconicotagger.dto.api.misc.NndVideoWithAssociatedVocaDbEntryForEvent
import niconicotagger.dto.api.misc.NndVideoWithAssociatedVocaDbEntryForTag
import niconicotagger.dto.api.misc.SongEntryBase
import niconicotagger.dto.api.misc.SongEntryWithReleaseEventInfo
import niconicotagger.dto.api.misc.SongEntryWithTagAssignmentInfo
import niconicotagger.dto.inner.misc.SongType
import niconicotagger.dto.inner.vocadb.VocaDbTag

sealed interface VideosByTagsResponse<T1 : NndVideoWithAssociatedVocaDbEntry<T2>, T2 : SongEntryBase> :
    ApiResponse<T1> {
    override val items: List<T1>
    override val totalCount: Long
    val cleanScope: String
}

interface VideosByTagsResponseForTagging :
    VideosByTagsResponse<NndVideoWithAssociatedVocaDbEntryForTag, SongEntryWithTagAssignmentInfo> {
    override val items: List<NndVideoWithAssociatedVocaDbEntryForTag>
    override val totalCount: Long
    override val cleanScope: String
    val songTypeStats: Map<SongType, Int>
}

data class VideosByNndTagsResponseForTagging(
    override val items: List<NndVideoWithAssociatedVocaDbEntryForTag>,
    override val totalCount: Long,
    override val cleanScope: String,
    override val songTypeStats: Map<SongType, Int>,
    val tagMappings: Collection<VocaDbTag>,
) : VideosByTagsResponseForTagging

data class VideosByVocaDbTagResponse(
    override val items: List<NndVideoWithAssociatedVocaDbEntryForTag>,
    override val totalCount: Long,
    override val cleanScope: String,
    override val songTypeStats: Map<SongType, Int>,
    val tag: VocaDbTag,
    val tagMappings: Collection<String>,
) : VideosByTagsResponseForTagging

data class VideosByNndTagsResponseForEvent(
    override val items: List<NndVideoWithAssociatedVocaDbEntryForEvent>,
    override val totalCount: Long,
    override val cleanScope: String,
) : VideosByTagsResponse<NndVideoWithAssociatedVocaDbEntryForEvent, SongEntryWithReleaseEventInfo>
