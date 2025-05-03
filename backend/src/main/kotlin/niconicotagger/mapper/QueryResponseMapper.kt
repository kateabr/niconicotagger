package niconicotagger.mapper

import niconicotagger.dto.api.misc.QueryConsoleArtistData
import niconicotagger.dto.api.misc.QueryConsoleData
import niconicotagger.dto.api.misc.QueryConsoleSongData
import niconicotagger.dto.api.response.QueryConsoleResponse
import niconicotagger.dto.inner.vocadb.VocaDbCustomQueryArtistData
import niconicotagger.dto.inner.vocadb.VocaDbCustomQuerySongData
import niconicotagger.dto.inner.vocadb.search.result.VocaDbCustomQuerySearchResult
import org.mapstruct.Mapper
import org.mapstruct.MappingConstants.ComponentModel.SPRING

@Mapper(componentModel = SPRING)
abstract class QueryResponseMapper {

    fun <T : QueryConsoleData> map(response: VocaDbCustomQuerySearchResult<*>): QueryConsoleResponse<T> {
        return QueryConsoleResponse(
            response.items.map {
                when (it) {
                    is VocaDbCustomQueryArtistData -> mapArtist(it)
                    is VocaDbCustomQuerySongData -> mapSong(it)
                }
                    as T
            },
            response.items.flatMap { it.tags }.distinctBy { it.id },
            response.totalCount,
        )
    }

    protected abstract fun mapArtist(item: VocaDbCustomQueryArtistData): QueryConsoleArtistData

    protected abstract fun mapSong(item: VocaDbCustomQuerySongData): QueryConsoleSongData
}
