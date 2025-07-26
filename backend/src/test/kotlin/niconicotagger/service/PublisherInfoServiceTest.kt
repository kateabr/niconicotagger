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
import niconicotagger.client.DbClient
import niconicotagger.client.DbClientHolder
import niconicotagger.client.NicologClient
import niconicotagger.client.NndClient
import niconicotagger.configuration.dto.NndPublisherType
import niconicotagger.configuration.dto.NndPublisherType.NND_CHANNEL
import niconicotagger.configuration.dto.NndPublisherType.NND_CHANNEL_HANDLE
import niconicotagger.configuration.dto.NndPublisherType.NND_CHANNEL_SHORTENED
import niconicotagger.configuration.dto.NndPublisherType.NND_USER
import niconicotagger.constants.Constants.DATABASE_PLACEHOLDER
import niconicotagger.dto.api.misc.ClientType
import niconicotagger.dto.api.misc.ClientType.VOCADB
import niconicotagger.dto.api.misc.ClientType.VOCADB_BETA
import niconicotagger.dto.inner.nnd.NndThumbnail
import niconicotagger.dto.inner.nnd.NndThumbnailError
import niconicotagger.dto.inner.nnd.NndThumbnailOk
import niconicotagger.dto.inner.nnd.NndVideoData
import niconicotagger.dto.inner.nnd.ThumbData
import niconicotagger.dto.inner.vocadb.PublisherInfo
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

class PublisherInfoServiceTest : AbstractApplicationContextTest() {
    @MockkBean lateinit var dbClientHolder: DbClientHolder

    @MockkBean lateinit var nndClient: NndClient

    @MockkBean lateinit var dbClient: DbClient

    @MockkBean lateinit var nicologClient: NicologClient

    @Autowired lateinit var publisherInfoService: PublisherInfoService

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
        nndPublisherType: NndPublisherType,
        artistQuery: String,
        channelHandle: String?,
        vocaDbArtist: VocaDbArtist?,
        nndThumbnail: NndThumbnail,
        nicologResponse: String?,
        expectedPublisher: PublisherInfo,
        clientType: ClientType,
    ): Unit = runBlocking {
        when (nndPublisherType) {
            NND_USER -> coEvery { dbClient.getArtistByQuery(eq(artistQuery)) } returns vocaDbArtist
            NND_CHANNEL,
            NND_CHANNEL_SHORTENED,
            NND_CHANNEL_HANDLE -> {
                coEvery { dbClient.findArtistDuplicate(eq(artistQuery)) } returns vocaDbArtist
                coEvery { dbClient.findArtistDuplicate(neq(artistQuery)) } returns null
            }
        }
        if (video.channelId != null && (channelHandle != null || vocaDbArtist == null)) {
            coEvery { nndClient.getChannelHandle(eq(video.channelId!!)) } returns channelHandle
        }
        if (vocaDbArtist == null) {
            coEvery { nndClient.getThumbInfo(eq(video.id)) } returns nndThumbnail
        }
        if (nicologResponse != null) {
            if (nndPublisherType == NND_USER)
                coEvery { nicologClient.getUserName(video.userId!!) } returns nicologResponse
            else if (nndPublisherType == NND_CHANNEL)
                coEvery { nicologClient.getChannelName(video.channelId!!) } returns nicologResponse
        }

        assertThat(publisherInfoService.getPublisher(video, clientType))
            .usingRecursiveComparison()
            .isEqualTo(expectedPublisher)
        coVerifyCount {
            (if (nndPublisherType == NND_USER) 1 else 0) * { dbClient.getArtistByQuery(any()) }
            (if (nndPublisherType == NND_CHANNEL && vocaDbArtist != null) 1
            else if (nndPublisherType == NND_CHANNEL || nndPublisherType == NND_CHANNEL_SHORTENED) 2
            else if (nndPublisherType == NND_CHANNEL_HANDLE) 3 else 0) * { dbClient.findArtistDuplicate(any()) }
            (if (video.channelId != null && (channelHandle != null || vocaDbArtist == null)) 1 else 0) *
                {
                    nndClient.getChannelHandle(any())
                }
            (if (vocaDbArtist == null) 1 else 0) * { nndClient.getThumbInfo(any()) }
            (if (nndPublisherType == NND_USER && nicologResponse != null) 1 else 0).times {
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
                val channelVideoForShortenedPath =
                    Instancio.of(NndVideoData::class.java).ignore(field("userId")).create()
                val channelVideoForHandle = Instancio.of(NndVideoData::class.java).ignore(field("userId")).create()
                val channelHandle = Instancio.create(String::class.java)
                val vocaDbEntryArtist = Instancio.create(VocaDbArtist::class.java)
                return listOf(
                    argumentSet(
                        "publisher in the database, published by a user",
                        userVideo,
                        NND_USER,
                        "user/" + userVideo.userId,
                        null,
                        vocaDbEntryArtist,
                        Instancio.create(NndThumbnailOk::class.java),
                        null,
                        PublisherInfo(
                            "https://vocadb.net/Ar/" + vocaDbEntryArtist.id,
                            "Ar/" + vocaDbEntryArtist.id,
                            vocaDbEntryArtist.name,
                            DATABASE_PLACEHOLDER,
                        ),
                        VOCADB,
                    ),
                    argumentSet(
                        "publisher in the database, published by a channel, full id link",
                        channelVideo,
                        NND_CHANNEL,
                        "https://ch.nicovideo.jp/channel/ch" + channelVideo.channelId,
                        null,
                        vocaDbEntryArtist,
                        Instancio.create(NndThumbnailOk::class.java),
                        null,
                        PublisherInfo(
                            "https://beta.vocadb.net/Ar/" + vocaDbEntryArtist.id,
                            "Ar/" + vocaDbEntryArtist.id,
                            vocaDbEntryArtist.name,
                            DATABASE_PLACEHOLDER,
                        ),
                        VOCADB_BETA,
                    ),
                    argumentSet(
                        "publisher in the database, published by a channel, shortened id link",
                        channelVideoForShortenedPath,
                        NND_CHANNEL_SHORTENED,
                        "https://ch.nicovideo.jp/ch" + channelVideoForShortenedPath.channelId,
                        null,
                        vocaDbEntryArtist,
                        Instancio.create(NndThumbnailOk::class.java),
                        null,
                        PublisherInfo(
                            "https://beta.vocadb.net/Ar/" + vocaDbEntryArtist.id,
                            "Ar/" + vocaDbEntryArtist.id,
                            vocaDbEntryArtist.name,
                            DATABASE_PLACEHOLDER,
                        ),
                        VOCADB_BETA,
                    ),
                    argumentSet(
                        "publisher in the database, published by a channel, handle link",
                        channelVideoForHandle,
                        NND_CHANNEL_HANDLE,
                        "https://ch.nicovideo.jp/$channelHandle",
                        channelHandle,
                        vocaDbEntryArtist,
                        Instancio.create(NndThumbnailOk::class.java),
                        null,
                        PublisherInfo(
                            "https://beta.vocadb.net/Ar/" + vocaDbEntryArtist.id,
                            "Ar/" + vocaDbEntryArtist.id,
                            vocaDbEntryArtist.name,
                            DATABASE_PLACEHOLDER,
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
                        null,
                        thumbnailUser,
                        null,
                        PublisherInfo(
                            "https://www.nicovideo.jp/user/" + thumbnailUser.data.userId,
                            "user/" + thumbnailUser.data.userId,
                            thumbnailUser.data.publisherName,
                            NND_USER.name,
                        ),
                        VOCADB_BETA,
                    ),
                    argumentSet(
                        "publisher not in the database, published by a channel, extracted name from the thumbnail",
                        channelVideo,
                        NND_CHANNEL,
                        "https://ch.nicovideo.jp/channel/ch" + channelVideo.channelId,
                        null,
                        null,
                        thumbnailChannel,
                        null,
                        PublisherInfo(
                            "https://ch.nicovideo.jp/channel/ch" + thumbnailChannel.data.channelId,
                            "channel/ch${thumbnailChannel.data.channelId}",
                            thumbnailChannel.data.publisherName,
                            NND_CHANNEL.name,
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
                        null,
                        thumbnail,
                        nicologPublisherName,
                        PublisherInfo(
                            "https://www.nicovideo.jp/user/" + userVideo.userId,
                            "user/" + userVideo.userId,
                            nicologPublisherName,
                            NND_USER.name,
                        ),
                        VOCADB_BETA,
                    ),
                    argumentSet(
                        "publisher not in the database, published by a channel, thumbnail doesn't provide information about publisher, fall back to nicolog",
                        channelVideo,
                        NND_CHANNEL,
                        "https://ch.nicovideo.jp/channel/ch" + channelVideo.channelId,
                        null,
                        null,
                        thumbnail,
                        nicologPublisherName,
                        PublisherInfo(
                            "https://ch.nicovideo.jp/channel/ch" + channelVideo.channelId,
                            "channel/ch${channelVideo.channelId}",
                            nicologPublisherName,
                            NND_CHANNEL.name,
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
                        null,
                        thumbnail,
                        nicologPublisherName,
                        PublisherInfo(
                            "https://www.nicovideo.jp/user/" + userVideo.userId,
                            "user/" + userVideo.userId,
                            nicologPublisherName,
                            NND_USER.name,
                        ),
                        VOCADB_BETA,
                    ),
                    argumentSet(
                        "publisher not in the database, published by a channel, thumbnail invalid, fall back to nicolog",
                        channelVideo,
                        NND_CHANNEL,
                        "https://ch.nicovideo.jp/channel/ch" + channelVideo.channelId,
                        null,
                        null,
                        thumbnail,
                        nicologPublisherName,
                        PublisherInfo(
                            "https://ch.nicovideo.jp/channel/ch" + channelVideo.channelId,
                            "channel/ch${channelVideo.channelId}",
                            nicologPublisherName,
                            NND_CHANNEL.name,
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
