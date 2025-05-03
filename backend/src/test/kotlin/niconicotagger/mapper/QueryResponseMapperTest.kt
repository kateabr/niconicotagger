package niconicotagger.mapper

import niconicotagger.dto.api.misc.ApiType
import niconicotagger.dto.api.misc.ApiType.ARTISTS
import niconicotagger.dto.api.misc.ApiType.SONGS
import niconicotagger.dto.api.misc.QueryConsoleArtistData
import niconicotagger.dto.api.misc.QueryConsoleSongData
import niconicotagger.dto.api.response.QueryConsoleResponse
import niconicotagger.dto.inner.vocadb.VocaDbCustomQueryArtistData
import niconicotagger.dto.inner.vocadb.VocaDbCustomQuerySongData
import niconicotagger.dto.inner.vocadb.VocaDbTag
import niconicotagger.dto.inner.vocadb.search.result.VocaDbCustomQuerySearchResult
import org.assertj.core.api.Assertions.assertThat
import org.instancio.Instancio
import org.instancio.Select.field
import org.junit.jupiter.api.Test
import org.mapstruct.factory.Mappers

class QueryResponseMapperTest {
    val mapper: QueryResponseMapper = Mappers.getMapper(QueryResponseMapper::class.java)

    @Test
    fun `map response test (artists)`() {
        val responseItem1 =
            Instancio.of(VocaDbCustomQueryArtistData::class.java)
                .set(field("id"), 10)
                .set(field("tags"), listOf(VocaDbTag(1, "1"), VocaDbTag(2, "2")))
                .create()
        val responseItem2 =
            Instancio.of(VocaDbCustomQueryArtistData::class.java)
                .set(field("id"), 20)
                .set(field("tags"), listOf(VocaDbTag(1, "1"), VocaDbTag(3, "3")))
                .create()
        val response =
            VocaDbCustomQuerySearchResult<VocaDbCustomQueryArtistData>(listOf(responseItem1, responseItem2), 2)

        assertThat(mapper.map<QueryConsoleArtistData>(response))
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .isEqualTo(
                QueryConsoleResponse(
                    listOf(
                        QueryConsoleArtistData(
                            response.items[0].id,
                            response.items[0].name,
                            response.items[0].tags,
                            response.items[0].type,
                        ),
                        QueryConsoleArtistData(
                            response.items[1].id,
                            response.items[1].name,
                            response.items[1].tags,
                            response.items[1].type,
                        ),
                    ),
                    listOf(VocaDbTag(1, "1"), VocaDbTag(2, "2"), VocaDbTag(3, "3")),
                    response.totalCount,
                )
            )
    }

    @Test
    fun `map response test (songs)`() {
        val responseItem1 =
            Instancio.of(VocaDbCustomQuerySongData::class.java)
                .set(field("id"), 10)
                .set(field("tags"), listOf(VocaDbTag(1, "1"), VocaDbTag(2, "2")))
                .create()
        val responseItem2 =
            Instancio.of(VocaDbCustomQuerySongData::class.java)
                .set(field("id"), 20)
                .set(field("tags"), listOf(VocaDbTag(1, "1"), VocaDbTag(3, "3")))
                .create()
        val response = VocaDbCustomQuerySearchResult<VocaDbCustomQuerySongData>(listOf(responseItem1, responseItem2), 2)

        assertThat(mapper.map<QueryConsoleSongData>(response))
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .isEqualTo(
                QueryConsoleResponse(
                    listOf(
                        QueryConsoleSongData(
                            response.items[0].id,
                            response.items[0].name,
                            response.items[0].tags,
                            response.items[0].type,
                            response.items[0].artistString,
                        ),
                        QueryConsoleSongData(
                            response.items[1].id,
                            response.items[1].name,
                            response.items[1].tags,
                            response.items[1].type,
                            response.items[1].artistString,
                        ),
                    ),
                    listOf(VocaDbTag(1, "1"), VocaDbTag(2, "2"), VocaDbTag(3, "3")),
                    response.totalCount,
                )
            )
    }

    @Test
    fun `api type values test`() {
        assertThat(ApiType.entries).containsExactlyInAnyOrder(ARTISTS, SONGS)
    }
}
