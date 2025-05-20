package niconicotagger.service

import com.fasterxml.jackson.core.type.TypeReference
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyAll
import io.mockk.coVerifyCount
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.spyk
import io.mockk.verifyAll
import java.time.ZoneOffset.UTC
import java.time.format.DateTimeFormatter.ISO_DATE_TIME
import java.util.function.Predicate
import java.util.stream.Stream
import kotlinx.coroutines.runBlocking
import niconicotagger.Utils.createSampleSongTypeStats
import niconicotagger.Utils.jsonMapper
import niconicotagger.Utils.loadResource
import niconicotagger.client.DbClientHolder
import niconicotagger.client.NicologClient
import niconicotagger.client.NndClient
import niconicotagger.client.VocaDbClient
import niconicotagger.configuration.PublisherLinkConfig
import niconicotagger.constants.Constants.FIRST_WORK_TAG_ID
import niconicotagger.dto.api.misc.ApiType
import niconicotagger.dto.api.misc.ApiType.ARTISTS
import niconicotagger.dto.api.misc.ApiType.SONGS
import niconicotagger.dto.api.misc.ClientType
import niconicotagger.dto.api.misc.NndTagType.MAPPED
import niconicotagger.dto.api.misc.NndTagType.NONE
import niconicotagger.dto.api.misc.NndTagType.SCOPE
import niconicotagger.dto.api.misc.NndTagType.TARGET
import niconicotagger.dto.api.misc.NndVideoWithAssociatedVocaDbEntryForEvent
import niconicotagger.dto.api.misc.NndVideoWithAssociatedVocaDbEntryForTag
import niconicotagger.dto.api.misc.QueryConsoleArtistData
import niconicotagger.dto.api.misc.QueryConsoleSongData
import niconicotagger.dto.api.misc.SongEntryWithReleaseEventInfo
import niconicotagger.dto.api.misc.SongEntryWithTagAssignmentInfo
import niconicotagger.dto.api.request.GetReleaseEventRequest
import niconicotagger.dto.api.request.QueryConsoleRequest
import niconicotagger.dto.api.request.SongsWithPvsRequest
import niconicotagger.dto.api.request.VideosByNndEventTagsRequest
import niconicotagger.dto.api.request.VideosByNndTagsRequest
import niconicotagger.dto.api.request.VideosByVocaDbTagRequest
import niconicotagger.dto.api.response.QueryConsoleResponse
import niconicotagger.dto.api.response.ReleaseEventWithVocaDbTagsResponse
import niconicotagger.dto.api.response.ReleaseEventWitnNndTagsResponse
import niconicotagger.dto.api.response.SongsWithPvsResponse
import niconicotagger.dto.api.response.VideosByNndTagsResponseForEvent
import niconicotagger.dto.api.response.VideosByNndTagsResponseForTagging
import niconicotagger.dto.api.response.VideosByTagsResponseForTagging
import niconicotagger.dto.inner.misc.PvService.NicoNicoDouga
import niconicotagger.dto.inner.misc.PvService.Youtube
import niconicotagger.dto.inner.misc.ReleaseEventCategory
import niconicotagger.dto.inner.misc.SongPv
import niconicotagger.dto.inner.misc.WebLink
import niconicotagger.dto.inner.nnd.NndApiSearchResult
import niconicotagger.dto.inner.nnd.NndMeta
import niconicotagger.dto.inner.nnd.NndTag
import niconicotagger.dto.inner.nnd.NndThumbnailOk
import niconicotagger.dto.inner.nnd.NndVideoData
import niconicotagger.dto.inner.nnd.ThumbData
import niconicotagger.dto.inner.vocadb.PublisherInfo
import niconicotagger.dto.inner.vocadb.VocaDbCustomQueryArtistData
import niconicotagger.dto.inner.vocadb.VocaDbCustomQuerySongData
import niconicotagger.dto.inner.vocadb.VocaDbReleaseEvent
import niconicotagger.dto.inner.vocadb.VocaDbReleaseEventSeries
import niconicotagger.dto.inner.vocadb.VocaDbSongEntryWithNndPvsAndTags
import niconicotagger.dto.inner.vocadb.VocaDbSongEntryWithTags
import niconicotagger.dto.inner.vocadb.VocaDbSongWithReleaseEvents
import niconicotagger.dto.inner.vocadb.VocaDbTag
import niconicotagger.dto.inner.vocadb.VocaDbTagMapping
import niconicotagger.dto.inner.vocadb.VocaDbTagSelectable
import niconicotagger.dto.inner.vocadb.search.result.VocaDbCustomQuerySearchResult
import niconicotagger.dto.inner.vocadb.search.result.VocaDbSongEntryWithNndPvsAndTagsSearchResult
import niconicotagger.mapper.NndVideoWithAssociatedVocaDbEntryMapper
import niconicotagger.mapper.QueryResponseMapper
import niconicotagger.mapper.ReleaseEventMapper
import niconicotagger.mapper.RequestMapper
import niconicotagger.mapper.SongWithPvsMapper
import niconicotagger.serde.Utils.normalizeToken
import org.assertj.core.api.Assertions.assertThat
import org.instancio.Assign.given
import org.instancio.Instancio
import org.instancio.Select.field
import org.instancio.Select.root
import org.instancio.Select.types
import org.instancio.TypeToken
import org.instancio.junit.Given
import org.instancio.junit.InstancioExtension
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.ArgumentSet
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import java.time.Duration

@ExtendWith(InstancioExtension::class)
class QueryConsoleTest: AggregatingServiceTest() {

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
