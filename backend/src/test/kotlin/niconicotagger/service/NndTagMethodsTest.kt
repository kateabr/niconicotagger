package niconicotagger.service

import com.fasterxml.jackson.core.type.TypeReference
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyAll
import io.mockk.coVerifyCount
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.slot
import io.mockk.verifyAll
import java.time.ZoneOffset.UTC
import java.time.format.DateTimeFormatter.ISO_DATE_TIME
import java.util.function.Predicate
import java.util.stream.Stream
import kotlinx.coroutines.runBlocking
import niconicotagger.Utils.createSampleSongTypeStats
import niconicotagger.Utils.jsonMapper
import niconicotagger.Utils.loadResource
import niconicotagger.constants.Constants.FIRST_WORK_TAG_ID
import niconicotagger.dto.api.misc.NndTagType.MAPPED
import niconicotagger.dto.api.misc.NndTagType.NONE
import niconicotagger.dto.api.misc.NndTagType.SCOPE
import niconicotagger.dto.api.misc.NndTagType.TARGET
import niconicotagger.dto.api.misc.NndVideoWithAssociatedVocaDbEntryForEvent
import niconicotagger.dto.api.misc.NndVideoWithAssociatedVocaDbEntryForTag
import niconicotagger.dto.api.misc.SongEntryWithReleaseEventInfo
import niconicotagger.dto.api.misc.SongEntryWithTagAssignmentInfo
import niconicotagger.dto.api.request.SongsWithPvsRequest
import niconicotagger.dto.api.request.VideosByNndEventTagsRequest
import niconicotagger.dto.api.request.VideosByNndTagsRequest
import niconicotagger.dto.api.request.VideosByVocaDbTagRequest
import niconicotagger.dto.api.response.SongsWithPvsResponse
import niconicotagger.dto.api.response.VideosByNndTagsResponseForEvent
import niconicotagger.dto.api.response.VideosByNndTagsResponseForTagging
import niconicotagger.dto.api.response.VideosByTagsResponseForTagging
import niconicotagger.dto.inner.misc.PvService.NicoNicoDouga
import niconicotagger.dto.inner.misc.PvService.Youtube
import niconicotagger.dto.inner.misc.SongPv
import niconicotagger.dto.inner.nnd.NndApiSearchResult
import niconicotagger.dto.inner.nnd.NndMeta
import niconicotagger.dto.inner.nnd.NndTag
import niconicotagger.dto.inner.nnd.NndThumbnailOk
import niconicotagger.dto.inner.nnd.NndVideoData
import niconicotagger.dto.inner.nnd.ThumbData
import niconicotagger.dto.inner.vocadb.PublisherInfo
import niconicotagger.dto.inner.vocadb.VocaDbSongEntryWithNndPvsAndTags
import niconicotagger.dto.inner.vocadb.VocaDbSongEntryWithTags
import niconicotagger.dto.inner.vocadb.VocaDbSongWithReleaseEvents
import niconicotagger.dto.inner.vocadb.VocaDbTag
import niconicotagger.dto.inner.vocadb.VocaDbTagMapping
import niconicotagger.dto.inner.vocadb.VocaDbTagSelectable
import niconicotagger.dto.inner.vocadb.search.result.VocaDbSongEntryWithNndPvsAndTagsSearchResult
import niconicotagger.serde.Utils.normalizeToken
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.InstanceOfAssertFactories.iterable
import org.assertj.core.api.InstanceOfAssertFactories.list
import org.assertj.core.api.InstanceOfAssertFactories.map
import org.instancio.Assign.given
import org.instancio.Instancio
import org.instancio.Select.field
import org.instancio.Select.root
import org.instancio.Select.types
import org.instancio.junit.Given
import org.instancio.junit.InstancioExtension
import org.junit.jupiter.api.AfterEach
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

@ExtendWith(InstancioExtension::class)
class NndTagMethodsTest : AggregatingServiceTest() {

    @AfterEach
    fun confirm() {
        verifyAll { dbClientHolder.getClient(any()) }
    }

    @ParameterizedTest
    @ArgumentsSource(GetVideoByNndTagsFirstWorkTestData::class)
    @Suppress("CognitiveComplexMethod")
    fun `get videos by NND tags test (tagging, first work)`(
        song: VocaDbSongEntryWithTags?,
        video: NndVideoData,
        tagMappings: List<VocaDbTagMapping>,
        priorSongsCheckResult: Boolean?,
        resultingTagSet: List<VocaDbTagSelectable>,
    ): Unit = runBlocking {
        val request =
            Instancio.of(VideosByNndTagsRequest::class.java)
                .set(field("startOffset"), 0)
                .set(field("tags"), tagMappings.map { it.sourceTag.lowercase() }.toSet())
                .create()
        val resultPlaceholder = mockkClass(NndVideoWithAssociatedVocaDbEntryForTag::class)
        coEvery { dbClient.getAllVocaDbTagMappings(eq(false)) } returns tagMappings
        coEvery { nndClient.getVideosByTags(eq(request)) } returns NndApiSearchResult(NndMeta(1), listOf(video))
        coEvery {
            dbClient.getSongByNndPv(eq(video.id), eq("Tags,Artists"), eq(VocaDbSongEntryWithTags::class.java))
        } returns song
        every {
            songMapper.mapForTag(eq(video), song?.let { eq(it) } ?: isNull(), any(), any(), eq(resultingTagSet), any())
        } returns resultPlaceholder
        every { resultPlaceholder.entry } returns
            if (song == null) null else Instancio.create(SongEntryWithTagAssignmentInfo::class.java)
        if (song == null) {
            coEvery { aggregatingService.getPublisher(eq(video), eq(request.clientType)) } returns null
        }
        if (priorSongsCheckResult != null) {
            coEvery {
                dbClient.artistHasSongsBeforeDate(
                    eq(requireNotNull(song?.artists?.first()?.artistEntryData).id),
                    eq(requireNotNull(song?.publishedAt).atOffset(UTC).format(ISO_DATE_TIME)),
                )
            } returns priorSongsCheckResult
        }

        aggregatingService.getVideosByNndTags(request)

        coVerify {
            dbClient.getAllVocaDbTagMappings(any())
            dbClient.getSongByNndPv<VocaDbSongEntryWithTags>(any(), any(), any())
            aggregatingService.getVideosByNndTags(any())
            aggregatingService.sortResults<NndVideoWithAssociatedVocaDbEntryForTag, SongEntryWithTagAssignmentInfo>(
                any(),
                any(),
            )
            aggregatingService.buildResultingTagSet(any(), any(), any())
        }
        coVerifyCount {
            (if (song == null) 1 else 0) * { aggregatingService.getPublisher(any(), any()) }
            (if (song != null && song.tags.none { it.id == FIRST_WORK_TAG_ID }) 1 else 0).times {
                aggregatingService.likelyEarliestWork(any(), any())
            }
            (if (priorSongsCheckResult != null) 1 else 0) * { dbClient.artistHasSongsBeforeDate(any(), any()) }
        }
        confirmVerified(dbClient, aggregatingService)
        coVerifyAll { nndClient.getVideosByTags(any()) }
        verifyAll { songMapper.mapForTag(any(), any(), any(), any(), any(), any()) }
    }

    @ParameterizedTest
    @ArgumentsSource(GetVideosByNndTagsTestData::class)
    @Suppress("CognitiveComplexMethod")
    fun `get videos by NND tags test (tagging)`(
        request: VideosByNndTagsRequest,
        video: NndVideoData,
        song: VocaDbSongEntryWithTags?,
        dbTagMappings: List<VocaDbTagMapping>,
        resultingTagSet: List<VocaDbTagSelectable>,
        mappedTags: Collection<VocaDbTag>,
    ): Unit = runBlocking {
        val resultPlaceholder = mockkClass(NndVideoWithAssociatedVocaDbEntryForTag::class)
        val songEntry = if (song == null) null else Instancio.create(SongEntryWithTagAssignmentInfo::class.java)
        val publisher = Instancio.of(PublisherInfo::class.java).withNullable(root()).create()
        val additionalDescription = Instancio.of(String::class.java).withNullable(root()).create()
        coEvery { dbClient.getAllVocaDbTagMappings(eq(false)) } returns dbTagMappings
        coEvery { nndClient.getVideosByTags(eq(request)) } returns NndApiSearchResult(NndMeta(1), listOf(video))
        if (video.description == null) {
            coEvery { nndClient.getFormattedDescription(video.id) } returns additionalDescription
        }
        if (song == null) {
            coEvery { aggregatingService.getPublisher(eq(video), eq(request.clientType)) } returns publisher
        }
        coEvery {
            dbClient.getSongByNndPv(eq(video.id), eq("Tags,Artists"), eq(VocaDbSongEntryWithTags::class.java))
        } returns song
        every {
            songMapper.mapForTag(
                eq(video),
                song?.let { eq(it) } ?: isNull(),
                eq(
                    mapOf(
                        "Tag1" to SCOPE,
                        "tag4" to SCOPE,
                        "tag5" to NONE,
                        "tag6" to MAPPED,
                        "タグ" to SCOPE,
                        "タグ1" to MAPPED,
                    ) + request.tags.associateWith { TARGET }
                ),
                video.description?.let { eq(it) } ?: additionalDescription?.let { eq(it) } ?: isNull(),
                eq(resultingTagSet),
                song?.let { isNull() } ?: (publisher?.let { eq(it) } ?: isNull()),
            )
        } returns resultPlaceholder
        every { resultPlaceholder.entry } returns songEntry

        assertThat(aggregatingService.getVideosByNndTags(request))
            .usingRecursiveComparison()
            .isEqualTo(
                VideosByNndTagsResponseForTagging(
                    listOf(resultPlaceholder),
                    1,
                    request.scope,
                    createSampleSongTypeStats(songEntry?.type),
                    mappedTags,
                )
            )

        coVerify {
            dbClient.getAllVocaDbTagMappings(any())
            nndClient.getVideosByTags(any())
            dbClient.getSongByNndPv<VocaDbSongEntryWithTags>(any(), any(), any())
            aggregatingService.getVideosByNndTags(any())
            aggregatingService.sortResults<NndVideoWithAssociatedVocaDbEntryForTag, SongEntryWithTagAssignmentInfo>(
                any(),
                any(),
            )
            aggregatingService.buildResultingTagSet(any(), any(), any())
        }
        coVerifyCount {
            (if (video.description == null) 1 else 0) * { nndClient.getFormattedDescription(any()) }
            (if (song == null) 1 else 0) * { aggregatingService.getPublisher(any(), any()) }
        }
        confirmVerified(dbClient, nndClient, eventMapper, aggregatingService)
        verifyAll { songMapper.mapForTag(any(), any(), any(), anyNullable(), any(), anyNullable()) }
    }

    @Test
    fun `get videos by NND tags test (prefetched tag id)`(@Given tag: VocaDbTag): Unit = runBlocking {
        val request = Instancio.of(VideosByNndTagsRequest::class.java).set(field("tags"), setOf(tag.name)).create()
        val mappedTags =
            listOf(
                VocaDbTagMapping("sourceTag1", tag),
                VocaDbTagMapping("sourceTag2", tag),
                VocaDbTagMapping(
                    "sourceTag2",
                    Instancio.of(VocaDbTag::class.java)
                        .filter<VocaDbTag>(root()) { it.id != tag.id && it.name != tag.name }
                        .create(),
                ),
            )
        val video =
            Instancio.of(NndVideoData::class.java).set(field("tags"), listOf("sourceTag1", "sourceTag2")).create()
        val song = Instancio.of(VocaDbSongEntryWithTags::class.java).setBlank(field("tags")).create()
        val correspondingVocaDbTagSetSlot = slot<Set<VocaDbTag>>()
        val resultPlaceholder = Instancio.createBlank(NndVideoWithAssociatedVocaDbEntryForTag::class.java)
        coEvery { dbClient.getAllVocaDbTagMappings(any()) } returns mappedTags
        coEvery { nndClient.getVideosByTags(eq(request)) } returns NndApiSearchResult(NndMeta(1), listOf(video))
        coEvery {
            dbClient.getSongByNndPv(eq(video.id), eq("Tags,Artists"), eq(VocaDbSongEntryWithTags::class.java))
        } returns song
        every {
            songMapper.mapForTag(
                eq(video),
                song,
                mapOf("sourceTag1" to MAPPED, "sourceTag2" to MAPPED),
                eq(video.description!!),
                listOf(VocaDbTagSelectable(tag, false)),
                isNull(),
            )
        } returns resultPlaceholder

        assertThat(aggregatingService.getVideosByNndTags(request, tag.id))
            .extracting { it.items }
            .asInstanceOf(list(NndVideoWithAssociatedVocaDbEntryForTag::class.java))
            .containsExactly(resultPlaceholder)

        coVerifyAll {
            dbClient.getAllVocaDbTagMappings(any())
            dbClient.getSongByNndPv<VocaDbSongEntryWithTags>(any(), any(), any())
            nndClient.getVideosByTags(any())
            aggregatingService.getVideosByNndTags(any(), any())
            aggregatingService.sortResults<NndVideoWithAssociatedVocaDbEntryForTag, SongEntryWithTagAssignmentInfo>(
                any(),
                any(),
            )
            aggregatingService.getVideosByNndTags(any(), any())
            aggregatingService.buildResultingTagSet(any(), any(), capture(correspondingVocaDbTagSetSlot))
        }
        assertThat(correspondingVocaDbTagSetSlot.captured)
            .asInstanceOf(iterable(VocaDbTag::class.java))
            .containsExactlyInAnyOrder(tag)
    }

    @ParameterizedTest
    @ArgumentsSource(GetVideosByNndEventTagsTestData::class)
    fun `get videos by NND tags test (event adding)`(
        request: VideosByNndEventTagsRequest,
        video: NndVideoData,
        song: VocaDbSongWithReleaseEvents?,
        tagMappings: List<VocaDbTagMapping>,
    ): Unit = runBlocking {
        val resultPlaceholder = mockkClass(NndVideoWithAssociatedVocaDbEntryForEvent::class)
        val publisher = Instancio.of(PublisherInfo::class.java).withNullable(root()).create()
        val additionalDescription = Instancio.of(String::class.java).withNullable(root()).create()
        coEvery { dbClient.getAllVocaDbTagMappings(eq(true)) } returns tagMappings
        coEvery { nndClient.getVideosByTags(eq(request)) } returns NndApiSearchResult(NndMeta(1), listOf(video))
        if (video.description == null) {
            coEvery { nndClient.getFormattedDescription(video.id) } returns additionalDescription
        }
        if (song == null) {
            coEvery { aggregatingService.getPublisher(eq(video), eq(request.clientType)) } returns publisher
        }
        coEvery {
            dbClient.getSongByNndPv(eq(video.id), eq("ReleaseEvent"), eq(VocaDbSongWithReleaseEvents::class.java))
        } returns song
        every {
            songMapper.mapForEvent(
                eq(video),
                song?.let { eq(it) } ?: isNull(),
                eq(minOf(video.publishedAt, song?.publishedAt ?: video.publishedAt)),
                eq(request.dates),
                eq(
                    mapOf(
                        "Tag1" to SCOPE,
                        "tag4" to SCOPE,
                        "tag5" to NONE,
                        "tag6" to MAPPED,
                        "タグ" to SCOPE,
                        "タグ1" to MAPPED,
                    ) + request.tags.associateWith { TARGET }
                ),
                video.description?.let { eq(it) } ?: additionalDescription?.let { eq(it) } ?: isNull(),
                song?.let { isNull() } ?: (publisher?.let { eq(it) } ?: isNull()),
            )
        } returns resultPlaceholder

        assertThat(aggregatingService.getVideosByEventNndTags(request))
            .usingRecursiveComparison()
            .isEqualTo(VideosByNndTagsResponseForEvent(listOf(resultPlaceholder), 1, request.scope))

        coVerify {
            dbClient.getAllVocaDbTagMappings(any())
            nndClient.getVideosByTags(any())
            dbClient.getSongByNndPv<VocaDbSongWithReleaseEvents>(any(), any(), any())
            aggregatingService.getVideosByEventNndTags(any())
            aggregatingService.sortResults<NndVideoWithAssociatedVocaDbEntryForEvent, SongEntryWithReleaseEventInfo>(
                any(),
                any(),
            )
        }
        coVerifyCount {
            (if (video.description == null) 1 else 0) * { nndClient.getFormattedDescription(any()) }
            (if (song == null) 1 else 0) * { aggregatingService.getPublisher(any(), any()) }
        }
        confirmVerified(dbClient, nndClient, aggregatingService)
        verifyAll { songMapper.mapForEvent(any(), any(), any(), any(), any(), anyNullable(), anyNullable()) }
    }

    @Test
    fun `get videos by VocaDB tag mappings test`(@Given tag: VocaDbTag) = runBlocking {
        val request = Instancio.of(VideosByVocaDbTagRequest::class.java).set(field("tag"), tag.name).create()
        val mappedTags =
            Instancio.ofList(VocaDbTagMapping::class.java)
                .generate(types().of(VocaDbTag::class.java)) { gen ->
                    gen.emit<VocaDbTag>().items(tag).whenEmptyEmitRandom()
                }
                .create()
        val tagMappings =
            mappedTags.filter { it.tag.id == tag.id }.map { it.sourceTag }.map { normalizeToken(it) }.toSet()
        val newRequest = mockkClass(VideosByNndTagsRequest::class)
        coEvery { dbClient.getTagByName(eq(tag.name)) } returns tag
        coEvery { dbClient.getAllVocaDbTagMappings(eq(false)) } returns mappedTags
        every { requestMapper.map(eq(request), eq(tagMappings)) } returns newRequest
        coEvery { aggregatingService.getVideosByNndTags(eq(newRequest), eq(tag.id)) } returns
            Instancio.create(VideosByNndTagsResponseForTagging::class.java)

        aggregatingService.getVideosByVocaDbTagMappings(request)

        coVerifyAll {
            aggregatingService.getVideosByVocaDbTagMappings(any())
            dbClient.getTagByName(any())
            dbClient.getAllVocaDbTagMappings(any())
            aggregatingService.getVideosByNndTags(any(), any())
        }
        verifyAll { requestMapper.map(any(), any()) }
    }

    @ParameterizedTest
    @CsvSource(value = ["false,false", "true,false", "true,true"])
    fun `get songs for tagging test`(
        taggedWithEarliestWork: Boolean,
        likelyEarliestWork: Boolean,
        @Given request: SongsWithPvsRequest,
    ): Unit = runBlocking {
        val thumbnailOk =
            Instancio.of(NndThumbnailOk::class.java)
                .set(field(ThumbData::class.java, "tags"), listOf(NndTag("アあA1", true), NndTag("tag", false)))
                .create()
        val mappings =
            listOf(
                VocaDbTagMapping("アあA1", VocaDbTag(if (taggedWithEarliestWork) FIRST_WORK_TAG_ID else -1, "tag")),
                VocaDbTagMapping("tag", Instancio.create(VocaDbTag::class.java)),
            )
        val songPvs =
            listOf(
                SongPv("1", "1", false, NicoNicoDouga),
                SongPv("2", "2", true, NicoNicoDouga),
                SongPv("3", "3", false, Youtube),
            )
        val song = Instancio.of(VocaDbSongEntryWithNndPvsAndTags::class.java).set(field("pvs"), songPvs).create()
        val searchResult = VocaDbSongEntryWithNndPvsAndTagsSearchResult(listOf(song), 1)
        val responsePlaceholder = mockk<SongsWithPvsResponse>()
        coEvery { dbClient.getAllVocaDbTagMappings(eq(true)) } returns mappings
        coEvery {
            dbClient.getSongs(
                eq(request.startOffset),
                eq(request.maxResults),
                eq(request.orderBy),
                eq(mapOf("pvServices" to NicoNicoDouga, "fields" to "PVs,Tags,Artists")),
            )
        } returns searchResult
        coEvery { nndClient.getThumbInfo(eq(songPvs[0].id)) } returns thumbnailOk
        if (taggedWithEarliestWork) {
            coEvery { aggregatingService.likelyEarliestWork(eq(request.clientType), eq(song)) } returns
                likelyEarliestWork
        }
        every {
            songWithPvsMapper.map(
                eq(searchResult),
                eq(mapOf(songPvs[0].id to thumbnailOk)),
                eq(mapOf("ああa1" to listOf(mappings[0]), "tag" to listOf(mappings[1]))),
                if (likelyEarliestWork) listOf(song.id) else emptyList(),
            )
        } returns responsePlaceholder

        assertThat(aggregatingService.getSongsWithPvsForTagging(request)).isEqualTo(responsePlaceholder)

        coVerifyAll {
            dbClient.getAllVocaDbTagMappings(any())
            dbClient.getSongs(any(), any(), any(), any())
            nndClient.getThumbInfo(any())
        }
        coVerifyCount {
            (if (taggedWithEarliestWork) 1 else 0) * { aggregatingService.likelyEarliestWork(any(), any()) }
        }
        verifyAll { songWithPvsMapper.map(any(), any(), any(), any()) }
    }

    companion object {
        private val nndVideoDataModel =
            Instancio.of(NndVideoData::class.java)
                .withNullable(field("userId"))
                .assign(
                    given(field("userId"), field("channelId"))
                        .generate(Predicate<Long?> { it == null }) { gen -> gen.longs() }
                        .elseSet(null)
                )
                .toModel()

        class GetVideosByNndTagsTestData : ArgumentsProvider {

            private fun createArgs(nndDescription: String?, hasEntry: Boolean): ArgumentSet {
                val tags =
                    Instancio.ofSet(String::class.java)
                        .size(2)
                        .generate(types().of(String::class.java)) { gen -> gen.string().lowerCase() }
                        .create()
                val request =
                    Instancio.of(VideosByNndTagsRequest::class.java)
                        .set(field("startOffset"), 0)
                        .set(field("tags"), tags)
                        .set(field("scope"), "tag1 OR Tag2 -tag3 OR Tag4 たぐ")
                        .create()
                val requestTagMappings =
                    request.tags.map { VocaDbTagMapping(it.lowercase(), Instancio.create(VocaDbTag::class.java)) }
                val tagMappings =
                    requestTagMappings +
                        listOf(
                            VocaDbTagMapping("tag4", Instancio.create(VocaDbTag::class.java)),
                            VocaDbTagMapping("tag6", Instancio.create(VocaDbTag::class.java)),
                            VocaDbTagMapping("たぐ1", Instancio.create(VocaDbTag::class.java)),
                        )
                val video =
                    Instancio.of(nndVideoDataModel)
                        .set(field("tags"), listOf("Tag1", "tag4", "tag5", "tag6", "タグ", "タグ1") + request.tags)
                        .set(field("description"), nndDescription)
                        .create()
                val song =
                    if (hasEntry)
                        Instancio.of(VocaDbSongEntryWithTags::class.java)
                            .set(field("tags"), tagMappings.drop(1).map { it.tag })
                            .create()
                    else null
                return argumentSet(
                    "[${VideosByTagsResponseForTagging::class.simpleName}] entry${if (hasEntry) "" else " not"} found in DB; video description in NND API response is${if (nndDescription == null) "" else " not"} null",
                    request,
                    video,
                    song,
                    tagMappings,
                    if (song == null) emptyList()
                    else
                        listOf(
                            VocaDbTagSelectable(requestTagMappings[0].tag, false),
                            VocaDbTagSelectable(requestTagMappings[1].tag, true),
                        ),
                    requestTagMappings.map { it.tag },
                )
            }

            override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
                return Stream.of(createArgs(Instancio.create(String::class.java), true), createArgs(null, false))
            }
        }

        class GetVideosByNndEventTagsTestData : ArgumentsProvider {

            private fun createArgs(nndDescription: String?, hasEntry: Boolean): ArgumentSet {
                val tags =
                    Instancio.ofSet(String::class.java)
                        .size(2)
                        .generate(types().of(String::class.java)) { gen -> gen.string().lowerCase() }
                        .create()
                val request =
                    Instancio.of(VideosByNndEventTagsRequest::class.java)
                        .set(field("tags"), tags)
                        .set(field("scope"), "tag1 OR Tag2 -tag3 OR Tag4 たぐ")
                        .create()
                val tagMappings =
                    request.tags.map { VocaDbTagMapping(it.lowercase(), Instancio.create(VocaDbTag::class.java)) } +
                        listOf(
                            VocaDbTagMapping("tag4", Instancio.create(VocaDbTag::class.java)),
                            VocaDbTagMapping("tag6", Instancio.create(VocaDbTag::class.java)),
                            VocaDbTagMapping("たぐ1", Instancio.create(VocaDbTag::class.java)),
                        )
                val video =
                    Instancio.of(nndVideoDataModel)
                        .set(field("tags"), listOf("Tag1", "tag4", "tag5", "tag6", "タグ", "タグ1") + request.tags)
                        .set(field("description"), nndDescription)
                        .create()
                val song = if (hasEntry) Instancio.create(VocaDbSongWithReleaseEvents::class.java) else null
                return argumentSet(
                    "[${VideosByNndTagsResponseForEvent::class.simpleName}] entry${if (hasEntry) "" else " not"} found in DB; NND description is${if (nndDescription == null) "" else " not"} null",
                    request,
                    video,
                    song,
                    tagMappings,
                )
            }

            override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
                return Stream.of(createArgs(Instancio.create(String::class.java), true), createArgs(null, false))
            }
        }

        class GetVideoByNndTagsFirstWorkTestData : ArgumentsProvider {

            data class TestVocaDbSongDataRecord(
                val resultingTagSet: List<VocaDbTagSelectable>,
                val entry: VocaDbSongEntryWithTags?,
                val priorSongsCheckResult: Boolean?,
                val caseDescription: String,
            )

            private val tagMappings =
                Instancio.createList(String::class.java).map {
                    VocaDbTagMapping(it, VocaDbTag(FIRST_WORK_TAG_ID, "first work"))
                }

            private fun createArgs(emptyTagSet: Boolean): List<ArgumentSet> {
                val testData =
                    jsonMapper
                        .readValue(
                            loadResource("responses/vocadb/first_work_tag_assignment_test_data.json"),
                            object : TypeReference<List<TestVocaDbSongDataRecord>>() {},
                        )
                        .filter { it.resultingTagSet.isEmpty() == emptyTagSet }
                val songs = testData.map { it.entry }
                val videos =
                    Instancio.ofList(nndVideoDataModel)
                        .size(songs.size)
                        .set(field(NndVideoData::class.java, "tags"), tagMappings.map { it.sourceTag })
                        .create()

                return generateSequence(0) { it + 1 }
                    .take(songs.size)
                    .map {
                        argumentSet(
                            testData[it].caseDescription,
                            songs[it],
                            videos[it],
                            tagMappings,
                            testData[it].priorSongsCheckResult,
                            testData[it].resultingTagSet,
                        )
                    }
                    .toList()
            }

            override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
                return Stream.concat(createArgs(true).stream(), createArgs(false).stream())
            }
        }
    }
}
