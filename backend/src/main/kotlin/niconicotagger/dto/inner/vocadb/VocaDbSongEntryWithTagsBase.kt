package niconicotagger.dto.inner.vocadb

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import java.time.Instant
import niconicotagger.dto.inner.misc.SongPv
import niconicotagger.dto.inner.misc.SongType
import niconicotagger.serde.VocaDbTagDeserializer

sealed interface VocaDbSongEntryBase {
    val id: Long
    val name: String
    val type: SongType
    val artists: List<VocaDbEntryArtist>
    val artistString: String
    val publishedAt: Instant?
}

sealed interface VocaDbSongEntryWithTagsBase : VocaDbSongEntryBase {
    override val id: Long
    override val name: String
    override val type: SongType
    override val artists: List<VocaDbEntryArtist>
    override val artistString: String
    override val publishedAt: Instant?
    val tags: List<VocaDbTag>
}

data class VocaDbSongWithReleaseEvents(
    override val id: Long,
    override val name: String,
    @JsonAlias("songType") override val type: SongType,
    override val artists: List<VocaDbEntryArtist> = emptyList(),
    override val artistString: String,
    @JsonProperty("publishDate") override val publishedAt: Instant?,
    @JsonProperty("releaseEvents") val events: List<VocaDbReleaseEvent> = emptyList(),
) : VocaDbSongEntryBase

data class VocaDbSongEntryWithTags(
    override val id: Long,
    override val name: String,
    @JsonAlias("songType") override val type: SongType,
    override val artists: List<VocaDbEntryArtist> = emptyList(),
    override val artistString: String,
    @JsonProperty("publishDate") override val publishedAt: Instant?,
    @JsonDeserialize(contentUsing = VocaDbTagDeserializer::class) override val tags: List<VocaDbTag>,
) : VocaDbSongEntryWithTagsBase

data class VocaDbSongEntryWithNndPvsAndTags(
    override val id: Long,
    override val name: String,
    @JsonAlias("songType") override val type: SongType,
    override val artists: List<VocaDbEntryArtist> = emptyList(),
    override val artistString: String,
    @JsonProperty("publishDate") override val publishedAt: Instant?,
    @JsonDeserialize(contentUsing = VocaDbTagDeserializer::class) override val tags: List<VocaDbTag> = emptyList(),
    val pvs: List<SongPv> = emptyList(),
) : VocaDbSongEntryWithTagsBase
