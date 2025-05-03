package niconicotagger.dto.api.misc

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import java.time.Instant
import niconicotagger.dto.inner.misc.SongType
import niconicotagger.dto.inner.vocadb.PublisherInfo
import niconicotagger.dto.inner.vocadb.VocaDbTagSelectable
import niconicotagger.serde.InstantToLocalDateSerializer

sealed interface SongEntryBase {
    val id: Long
    val name: String
    val type: SongType
    val artistString: String
}

sealed interface NndVideoWithAssociatedVocaDbEntry<T : SongEntryBase> {
    val video: AvailableNndVideoWithAdditionalData
    val entry: T?
    val publisher: PublisherInfo?
}

data class SongEntry(
    override val id: Long,
    override val name: String,
    override val type: SongType,
    override val artistString: String,
    @JsonSerialize(using = InstantToLocalDateSerializer::class) @JsonProperty("publishedOn") val publishedAt: Instant?,
) : SongEntryBase

data class SongEntryWithTagAssignmentInfo(
    override val id: Long,
    override val name: String,
    override val type: SongType,
    override val artistString: String,
    @JsonSerialize(using = InstantToLocalDateSerializer::class) @JsonProperty("publishedOn") val publishedAt: Instant?,
    val mappedTags: List<VocaDbTagSelectable>,
) : SongEntryBase

data class SongEntryWithReleaseEventInfo(
    override val id: Long,
    override val name: String,
    override val type: SongType,
    override val artistString: String,
    val events: List<ReleaseEvent>,
) : SongEntryBase

data class NndVideoWithAssociatedVocaDbEntryForTag(
    override val video: AvailableNndVideoWithAdditionalData,
    override val entry: SongEntryWithTagAssignmentInfo?,
    override val publisher: PublisherInfo?,
) : NndVideoWithAssociatedVocaDbEntry<SongEntryWithTagAssignmentInfo>

data class NndVideoWithAssociatedVocaDbEntryForEvent(
    override val video: AvailableNndVideoWithAdditionalData,
    override val entry: SongEntryWithReleaseEventInfo?,
    override val publisher: PublisherInfo?,
    @JsonSerialize(using = InstantToLocalDateSerializer::class) @JsonProperty("publishedOn") val publishedAt: Instant,
    val disposition: DispositionRelativelyToDate,
) : NndVideoWithAssociatedVocaDbEntry<SongEntryWithReleaseEventInfo>

data class SongEntryByVocaDbTagForEvent(
    override val id: Long,
    override val name: String,
    override val type: SongType,
    override val artistString: String,
    @JsonSerialize(using = InstantToLocalDateSerializer::class) @JsonProperty("publishedOn") val publishedAt: Instant?,
    val events: List<ReleaseEvent>,
    val disposition: DispositionRelativelyToDate,
) : SongEntryBase
