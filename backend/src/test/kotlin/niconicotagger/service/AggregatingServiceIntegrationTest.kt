package niconicotagger.service

import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import io.mockk.coVerifyCount
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.verifyAll
import java.util.stream.Stream
import kotlinx.coroutines.runBlocking
import niconicotagger.AbstractApplicationContextTest
import niconicotagger.client.DbClientHolder
import niconicotagger.client.NicologClient
import niconicotagger.client.NndClient
import niconicotagger.client.VocaDbClient
import niconicotagger.dto.api.misc.ClientType
import niconicotagger.dto.api.misc.ClientType.VOCADB
import niconicotagger.dto.api.misc.ClientType.VOCADB_BETA
import niconicotagger.dto.inner.nnd.NndThumbnail
import niconicotagger.dto.inner.nnd.NndThumbnailError
import niconicotagger.dto.inner.nnd.NndThumbnailOk
import niconicotagger.dto.inner.nnd.NndVideoData
import niconicotagger.dto.inner.nnd.ThumbData
import niconicotagger.dto.inner.vocadb.PublisherInfo
import niconicotagger.dto.inner.vocadb.PublisherType
import niconicotagger.dto.inner.vocadb.PublisherType.DATABASE
import niconicotagger.dto.inner.vocadb.PublisherType.NND_CHANNEL
import niconicotagger.dto.inner.vocadb.PublisherType.NND_USER
import niconicotagger.dto.inner.vocadb.VocaDbArtist
import org.assertj.core.api.Assertions.assertThat
import org.instancio.Instancio
import org.instancio.Select.all
import org.instancio.Select.field
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.ArgumentSet
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import org.springframework.beans.factory.annotation.Autowired

class AggregatingServiceIntegrationTest : AbstractApplicationContextTest() {
    @MockkBean lateinit var dbClientHolder: DbClientHolder

    @MockkBean lateinit var nndClient: NndClient

    @MockkBean lateinit var dbClient: VocaDbClient

    @MockkBean lateinit var nicologClient: NicologClient

    @Autowired lateinit var aggregatingService: AggregatingService

    @BeforeEach
    fun setup() {
        every { dbClientHolder.getClient(any()) } returns dbClient
    }

    @AfterEach
    fun verify() {
        verifyAll { dbClientHolder.getClient(any()) }
    }

    @ParameterizedTest
    @ArgumentsSource(GetPublisherTestData::class)
    @Suppress("CognitiveComplexMethod")
    fun `get publisher test`(
        video: NndVideoData,
        nndPublisherType: PublisherType,
        artistQuery: String,
        vocaDbArtist: VocaDbArtist?,
        nndThumbnail: NndThumbnail,
        nicologResponse: String?,
        expectedPublisher: PublisherInfo,
        clientType: ClientType,
    ): Unit = runBlocking {
        if (nndPublisherType == NND_USER) coEvery { dbClient.getArtistByQuery(eq(artistQuery)) } returns vocaDbArtist
        else if (nndPublisherType == NND_CHANNEL)
            coEvery { dbClient.findArtistDuplicate(eq(artistQuery)) } returns vocaDbArtist
        if (vocaDbArtist == null) {
            coEvery { nndClient.getThumbInfo(eq(video.id)) } returns nndThumbnail
        }
        if (nicologResponse != null) {
            if (nndPublisherType == NND_USER)
                coEvery { nicologClient.getUserName(requireNotNull(video.userId)) } returns nicologResponse
            else if (nndPublisherType == NND_CHANNEL)
                coEvery { nicologClient.getChannelName(requireNotNull(video.channelId)) } returns nicologResponse
        }

        assertThat(aggregatingService.getPublisher(video, clientType))
            .usingRecursiveComparison()
            .isEqualTo(expectedPublisher)
        coVerifyCount {
            (if (nndPublisherType == NND_USER) 1 else 0) * { dbClient.getArtistByQuery(any()) }
            (if (nndPublisherType == NND_CHANNEL) 1 else 0) * { dbClient.findArtistDuplicate(any()) }
            (if (vocaDbArtist == null) 1 else 0) * { nndClient.getThumbInfo(any()) }
            (if (nndPublisherType == NND_USER && (nicologResponse != null)) 1 else 0).times {
                nicologClient.getUserName(any())
            }
            (if (nndPublisherType == NND_CHANNEL && (nicologResponse != null)) 1 else 0).times {
                nicologClient.getChannelName(any())
            }
        }
        confirmVerified(dbClient, nicologClient, nndClient)
    }

    companion object {
        class GetPublisherTestData : ArgumentsProvider {
            private fun artistInDb(): List<ArgumentSet> {
                val userVideo = Instancio.of(NndVideoData::class.java).ignore(field("channelId")).create()
                val channelVideo = Instancio.of(NndVideoData::class.java).ignore(field("userId")).create()
                val vocaDbEntryArtist = Instancio.create(VocaDbArtist::class.java)
                return listOf(
                    argumentSet(
                        "publisher in the database, published by a user",
                        userVideo,
                        NND_USER,
                        "user/" + userVideo.userId,
                        vocaDbEntryArtist,
                        Instancio.create(NndThumbnailOk::class.java),
                        null,
                        PublisherInfo(
                            "https://vocadb.net/Ar/" + vocaDbEntryArtist.id,
                            "Ar/" + vocaDbEntryArtist.id,
                            vocaDbEntryArtist.name,
                            DATABASE,
                        ),
                        VOCADB,
                    ),
                    argumentSet(
                        "publisher in the database, published by a channel",
                        channelVideo,
                        NND_CHANNEL,
                        "https://ch.nicovideo.jp/channel/ch" + channelVideo.channelId,
                        vocaDbEntryArtist,
                        Instancio.create(NndThumbnailOk::class.java),
                        null,
                        PublisherInfo(
                            "https://beta.vocadb.net/Ar/" + vocaDbEntryArtist.id,
                            "Ar/" + vocaDbEntryArtist.id,
                            vocaDbEntryArtist.name,
                            DATABASE,
                        ),
                        VOCADB_BETA,
                    ),
                )
            }

            private fun artistNotInDbButCouldExtractNameFromThumbnail(): List<ArgumentSet> {
                val userVideo = Instancio.of(NndVideoData::class.java).ignore(field("channelId")).create()
                val channelVideo = Instancio.of(NndVideoData::class.java).ignore(field("userId")).create()
                val thumbnailUser =
                    Instancio.of(NndThumbnailOk::class.java).ignore(field(ThumbData::class.java, "channelId")).create()
                val thumbnailChannel =
                    Instancio.of(NndThumbnailOk::class.java).ignore(field(ThumbData::class.java, "userId")).create()
                return listOf(
                    argumentSet(
                        "publisher not in the database, published by a user, extracted name from the thumbnail",
                        userVideo,
                        NND_USER,
                        "user/" + userVideo.userId,
                        null,
                        thumbnailUser,
                        null,
                        PublisherInfo(
                            "https://www.nicovideo.jp/user/" + thumbnailUser.data.userId,
                            "user/" + thumbnailUser.data.userId,
                            thumbnailUser.data.publisherName,
                            NND_USER,
                        ),
                        VOCADB_BETA,
                    ),
                    argumentSet(
                        "publisher not in the database, published by a channel, extracted name from the thumbnail",
                        channelVideo,
                        NND_CHANNEL,
                        "https://ch.nicovideo.jp/channel/ch" + channelVideo.channelId,
                        null,
                        thumbnailChannel,
                        null,
                        PublisherInfo(
                            "https://ch.nicovideo.jp/channel/ch" + thumbnailChannel.data.channelId,
                            "channel/ch${thumbnailChannel.data.channelId}",
                            thumbnailChannel.data.publisherName,
                            NND_CHANNEL,
                        ),
                        VOCADB,
                    ),
                )
            }

            private fun artistNotInDatabaseNoInfoInThumbnailFallBackToNicolog(): List<ArgumentSet> {
                val userVideo = Instancio.of(NndVideoData::class.java).ignore(field("channelId")).create()
                val channelVideo = Instancio.of(NndVideoData::class.java).ignore(field("userId")).create()
                val thumbnail =
                    Instancio.of(NndThumbnailOk::class.java)
                        .ignore(
                            all(
                                field(ThumbData::class.java, "userId"),
                                field(ThumbData::class.java, "channelId"),
                                field(ThumbData::class.java, "publisherName"),
                            )
                        )
                        .create()
                val nicologPublisherName = Instancio.create(String::class.java)
                return listOf(
                    argumentSet(
                        "publisher not in the database, published by a user, thumbnail doesn't provide information about publisher, fall back to nicolog",
                        userVideo,
                        NND_USER,
                        "user/" + userVideo.userId,
                        null,
                        thumbnail,
                        nicologPublisherName,
                        PublisherInfo(
                            "https://www.nicovideo.jp/user/" + userVideo.userId,
                            "user/" + userVideo.userId,
                            nicologPublisherName,
                            NND_USER,
                        ),
                        VOCADB_BETA,
                    ),
                    argumentSet(
                        "publisher not in the database, published by a channel, thumbnail doesn't provide information about publisher, fall back to nicolog",
                        channelVideo,
                        NND_CHANNEL,
                        "https://ch.nicovideo.jp/channel/ch" + channelVideo.channelId,
                        null,
                        thumbnail,
                        nicologPublisherName,
                        PublisherInfo(
                            "https://ch.nicovideo.jp/channel/ch" + channelVideo.channelId,
                            "channel/ch${channelVideo.channelId}",
                            nicologPublisherName,
                            NND_CHANNEL,
                        ),
                        VOCADB,
                    ),
                )
            }

            private fun artistNotInDatabaseThumbnailInvalidFallBackToNicolog(): List<ArgumentSet> {
                val userVideo = Instancio.of(NndVideoData::class.java).ignore(field("channelId")).create()
                val channelVideo = Instancio.of(NndVideoData::class.java).ignore(field("userId")).create()
                val thumbnail = Instancio.create(NndThumbnailError::class.java)
                val nicologPublisherName = Instancio.create(String::class.java)
                return listOf(
                    argumentSet(
                        "publisher not in the database, published by a user, thumbnail invalid, fall back to nicolog",
                        userVideo,
                        NND_USER,
                        "user/" + userVideo.userId,
                        null,
                        thumbnail,
                        nicologPublisherName,
                        PublisherInfo(
                            "https://www.nicovideo.jp/user/" + userVideo.userId,
                            "user/" + userVideo.userId,
                            nicologPublisherName,
                            NND_USER,
                        ),
                        VOCADB_BETA,
                    ),
                    argumentSet(
                        "publisher not in the database, published by a channel, thumbnail invalid, fall back to nicolog",
                        channelVideo,
                        NND_CHANNEL,
                        "https://ch.nicovideo.jp/channel/ch" + channelVideo.channelId,
                        null,
                        thumbnail,
                        nicologPublisherName,
                        PublisherInfo(
                            "https://ch.nicovideo.jp/channel/ch" + channelVideo.channelId,
                            "channel/ch${channelVideo.channelId}",
                            nicologPublisherName,
                            NND_CHANNEL,
                        ),
                        VOCADB,
                    ),
                )
            }

            override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
                return listOf(
                        artistInDb(),
                        artistNotInDbButCouldExtractNameFromThumbnail(),
                        artistNotInDatabaseNoInfoInThumbnailFallBackToNicolog(),
                        artistNotInDatabaseThumbnailInvalidFallBackToNicolog(),
                    )
                    .flatten()
                    .stream()
            }
        }
    }
}
