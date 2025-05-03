package niconicotagger.dto.api.misc

import niconicotagger.dto.inner.misc.ArtistType
import niconicotagger.dto.inner.misc.SongType
import niconicotagger.dto.inner.vocadb.VocaDbTag

sealed interface QueryConsoleData {
    val id: Long
    val name: String
    val tags: List<VocaDbTag>
}

data class QueryConsoleArtistData(
    override val id: Long,
    override val name: String,
    override val tags: List<VocaDbTag>,
    val type: ArtistType,
) : QueryConsoleData

data class QueryConsoleSongData(
    override val id: Long,
    override val name: String,
    override val tags: List<VocaDbTag>,
    val type: SongType,
    val artistString: String,
) : QueryConsoleData
