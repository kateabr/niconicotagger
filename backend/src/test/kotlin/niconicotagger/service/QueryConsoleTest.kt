package niconicotagger.service

import io.mockk.coEvery
import io.mockk.coVerifyAll
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifyAll
import kotlinx.coroutines.runBlocking
import niconicotagger.dto.api.misc.ApiType
import niconicotagger.dto.api.misc.ApiType.ARTISTS
import niconicotagger.dto.api.misc.ApiType.SONGS
import niconicotagger.dto.api.misc.ClientType
import niconicotagger.dto.api.misc.QueryConsoleArtistData
import niconicotagger.dto.api.misc.QueryConsoleSongData
import niconicotagger.dto.api.request.QueryConsoleRequest
import niconicotagger.dto.api.response.QueryConsoleResponse
import niconicotagger.dto.inner.vocadb.VocaDbCustomQueryArtistData
import niconicotagger.dto.inner.vocadb.VocaDbCustomQuerySongData
import niconicotagger.dto.inner.vocadb.search.result.VocaDbCustomQuerySearchResult
import org.assertj.core.api.Assertions.assertThat
import org.instancio.Instancio
import org.instancio.Select.types
import org.instancio.TypeToken
import org.instancio.junit.Given
import org.instancio.junit.InstancioExtension
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

@ExtendWith(InstancioExtension::class)
class QueryConsoleTest : AggregatingServiceTest() {

    @AfterEach
    fun confirm() {
        verifyAll { dbClientHolder.getClient(any()) }
    }

    @EnumSource(ApiType::class)
    @ParameterizedTest
    fun `get data with tags by custom query test`(
        apiType: ApiType,
        @Given query: String,
        @Given clientType: ClientType,
    ): Unit = runBlocking {
        val response =
            Instancio.of(
                    when (apiType) {
                        ARTISTS -> object : TypeToken<VocaDbCustomQuerySearchResult<VocaDbCustomQueryArtistData>> {}
                        SONGS -> object : TypeToken<VocaDbCustomQuerySearchResult<VocaDbCustomQuerySongData>> {}
                    }
                )
                .generate(types().of(List::class.java)) { gen -> gen.collection<Any>().size(1) }
                .create()
        coEvery { dbClient.getDataWithTagsByCustomQuery(eq(apiType), eq(query)) } returns response
        coEvery { dbClient.getDataWithTagsByCustomQuery(eq(apiType), eq(query)) } returns response
        when (apiType) {
            ARTISTS ->
                every {
                    queryResponseMapper.map<QueryConsoleArtistData>(
                        eq(response as VocaDbCustomQuerySearchResult<VocaDbCustomQueryArtistData>)
                    )
                } returns QueryConsoleResponse(mockk<List<QueryConsoleArtistData>>(), emptyList(), 1)

            SONGS ->
                every {
                    queryResponseMapper.map<QueryConsoleSongData>(
                        eq(response as VocaDbCustomQuerySearchResult<VocaDbCustomQuerySongData>)
                    )
                } returns QueryConsoleResponse(mockk<List<QueryConsoleSongData>>(), emptyList(), 1)
        }

        assertThat(aggregatingService.getDataWithTagsByCustomQuery(QueryConsoleRequest(apiType, query, clientType)))
            .extracting { it.totalCount }
            .isEqualTo(1L)

        coVerifyAll { dbClient.getDataWithTagsByCustomQuery(any(), any()) }
        verifyAll {
            when (apiType) {
                ARTISTS -> queryResponseMapper.map<QueryConsoleArtistData>(any())

                SONGS -> queryResponseMapper.map<QueryConsoleSongData>(any())
            }
        }
    }
}
