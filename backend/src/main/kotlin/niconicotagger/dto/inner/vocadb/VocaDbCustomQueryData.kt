package niconicotagger.dto.inner.vocadb

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import niconicotagger.dto.inner.misc.ArtistType
import niconicotagger.dto.inner.misc.SongType
import niconicotagger.serde.VocaDbTagDeserializer

sealed interface VocaDbCustomQueryData {
    val id: Long
    val name: String
    val tags: List<VocaDbTag>
}

data class VocaDbCustomQueryArtistData(
    override val id: Long,
    @JsonAlias("defaultName")
    override val name: String,
    @JsonDeserialize(contentUsing = VocaDbTagDeserializer::class)
    override val tags: List<VocaDbTag>,
    @JsonAlias("artistType")
    val type: ArtistType
) : VocaDbCustomQueryData

data class VocaDbCustomQuerySongData(
    override val id: Long,
    @JsonAlias("defaultName")
    override val name: String,
    @JsonDeserialize(contentUsing = VocaDbTagDeserializer::class)
    override val tags: List<VocaDbTag>,
    @JsonAlias("songType")
    val type: SongType,
    val artistString: String
) : VocaDbCustomQueryData
