package niconicotagger.service

import com.fasterxml.jackson.core.type.TypeReference
import io.mockk.coEvery
import io.mockk.coVerifyAll
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verifyAll
import java.util.stream.Stream
import kotlinx.coroutines.runBlocking
import net.javacrumbs.jsonunit.assertj.assertThatJson
import niconicotagger.Utils.jsonMapper
import niconicotagger.Utils.loadResource
import niconicotagger.client.DbClientHolder
import niconicotagger.client.VocaDbClient
import niconicotagger.dto.api.misc.ApiType
import niconicotagger.dto.api.misc.ApiType.SONGS
import niconicotagger.dto.api.misc.ClientType
import niconicotagger.dto.api.misc.ClientType.VOCADB_BETA
import niconicotagger.dto.api.misc.PvToDisable
import niconicotagger.dto.api.misc.ReleaseEvent
import niconicotagger.dto.api.request.AddReleaseEventRequest
import niconicotagger.dto.api.request.DeleteTagsRequest
import niconicotagger.dto.api.request.SongTagsAndPvsUpdateRequest
import niconicotagger.dto.inner.vocadb.VocaDbTag
import niconicotagger.dto.inner.vocadb.VocaDbTagSelectable
import niconicotagger.dto.inner.vocadb.VocaDbTagUsage
import niconicotagger.dto.inner.vocadb.VocaDbTagUsages
import niconicotagger.mapper.RequestMapper
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.instancio.Instancio
import org.instancio.Select.field
import org.instancio.junit.Given
import org.instancio.junit.InstancioExtension
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import org.junit.jupiter.params.provider.EnumSource

@ExtendWith(InstancioExtension::class)
class UpdatingServiceTest {

    private val client = mockk<VocaDbClient>()
    private val clientHolder = mockk<DbClientHolder>()
    private val requestMapper = mockk<RequestMapper>()
    private val service = spyk(UpdatingService(clientHolder, requestMapper))

    @BeforeEach
    fun setup() {
        every { clientHolder.getClient(any()) } returns client
    }

    @AfterEach
    fun confirm() {
        verifyAll { clientHolder.getClient(any()) }
    }

    @Test
    fun `disable song PVs test`(@Given clientType: ClientType, @Given cookie: String): Unit = runBlocking {
        val updatedSongData = slot<MutableMap<String, Any>>()
        coEvery { client.getSongForEdit(eq(657_775), eq(cookie)) } returns
            jsonMapper.readValue(
                loadResource("responses/vocadb/song_for_edit.json"),
                object : TypeReference<MutableMap<String, Any>>() {},
            )
        coEvery { client.saveSong(eq(657_775), capture(updatedSongData), eq(cookie)) } returns Unit

        service.disablePvs(
            SongTagsAndPvsUpdateRequest(
                657_775,
                emptyList(),
                setOf(PvToDisable("sm43909677", "DELETED"), PvToDisable("sm43909678", "DELETED")),
            ),
            clientType,
            cookie,
        )

        assertThatJson(jsonMapper.writeValueAsString(updatedSongData.captured))
            .isEqualTo(loadResource("responses/vocadb/song_with_disabled_pvs.json").decodeToString())

        coVerifyAll {
            client.getSongForEdit(any(), any())
            client.saveSong(any(), any(), any())
        }
    }

    @Test
    fun `assign tags test`(@Given clientType: ClientType, @Given cookie: String): Unit = runBlocking {
        val tags = slot<List<VocaDbTag>>()
        coEvery { client.getSongTags(eq(657_775), eq(cookie)) } returns
            jsonMapper.readValue(
                loadResource("responses/vocadb/song_tags_lookup_result.json"),
                object : TypeReference<List<VocaDbTagSelectable>>() {},
            )
        coEvery { client.assignSongTags(eq(657_775), capture(tags), eq(cookie)) } returns Unit

        service.assignSongTags(SongTagsAndPvsUpdateRequest(657_775, listOf(158), emptySet()), clientType, cookie)

        assertThat(tags.captured)
            .containsExactlyInAnyOrder(VocaDbTag(158, ""), VocaDbTag(8087, "karaoke available (DAM&JOY)"))

        coVerifyAll {
            client.getSongTags(any(), any())
            client.assignSongTags(any(), any(), any())
        }
    }

    @Test
    fun `add release event test`(@Given clientType: ClientType, @Given cookie: String): Unit = runBlocking {
        val updatedSongData = slot<MutableMap<String, Any>>()
        coEvery { client.getSongForEdit(eq(657_775), eq(cookie)) } returns
            jsonMapper.readValue(
                loadResource("responses/vocadb/song_for_edit.json"),
                object : TypeReference<MutableMap<String, Any>>() {},
            )
        coEvery { client.saveSong(eq(657_775), capture(updatedSongData), eq(cookie)) } returns Unit

        service.addReleaseEvent(
            AddReleaseEventRequest(657_775, ReleaseEvent(6645, "0Mix vol.10", null)),
            clientType,
            cookie,
        )

        assertThatJson(jsonMapper.writeValueAsString(updatedSongData.captured))
            .isEqualTo(loadResource("responses/vocadb/song_with_added_event.json").decodeToString())

        coVerifyAll {
            client.getSongForEdit(any(), any())
            client.saveSong(any(), any(), any())
        }
    }

    @Test
    fun `delete tags test (success)`(
        @Given entryId: Long,
        @Given event: ReleaseEvent,
        @Given tagUsage: VocaDbTagUsage,
        @Given clientType: ClientType,
        @Given cookie: String,
    ): Unit = runBlocking {
        val request = DeleteTagsRequest(SONGS, entryId, listOf(tagUsage.tag))
        coEvery { client.getTagUsages(eq(SONGS), eq(entryId), eq(cookie)) } returns
            VocaDbTagUsages(true, listOf(tagUsage))
        coEvery { client.deleteTagUsage(eq(SONGS), eq(tagUsage.id), eq(cookie)) } returns Unit

        service.deleteTags(request, clientType, cookie)

        coVerifyAll {
            service.deleteTags(any(), any(), any())
            client.getTagUsages(any(), any(), any())
            client.deleteTagUsage(any(), any(), any())
        }
    }

    @ParameterizedTest
    @ArgumentsSource(DeleteTagsTestData::class)
    fun `delete tags test (error)`(
        message: String,
        request: DeleteTagsRequest,
        tagUsageResponse: VocaDbTagUsages,
        @Given clientType: ClientType,
        @Given cookie: String,
    ) {
        coEvery { client.getTagUsages(eq(request.apiType), eq(request.entryId), eq(cookie)) } returns tagUsageResponse

        assertThatExceptionOfType(IllegalStateException::class.java)
            .isThrownBy { runBlocking { service.deleteTags(request, clientType, cookie) } }
            .withMessage(message)
    }

    @EnumSource(ApiType::class)
    @ParameterizedTest
    fun `delete multiple tags test`(
        apiType: ApiType,
        @Given entryId: Long,
        @Given tags: List<VocaDbTag>,
        @Given cookie: String,
    ): Unit = runBlocking {
        val vocaDbTagUsages = VocaDbTagUsages(true, tags.map { VocaDbTagUsage(Instancio.create(Long::class.java), it) })
        coEvery { client.getTagUsages(eq(apiType), eq(entryId), eq(cookie)) } returns vocaDbTagUsages
        coEvery {
            client.deleteTagUsage(
                eq(apiType),
                match { vocaDbTagUsages.tagUsages.any { usage -> usage.id == it } },
                eq(cookie),
            )
        } returns Unit

        service.deleteTags(DeleteTagsRequest(apiType, entryId, tags), VOCADB_BETA, cookie)

        coEvery {
            client.getTagUsages(any(), any(), any())
            client.deleteTagUsage(any(), any(), any())
        }
    }

    companion object {
        class DeleteTagsTestData : ArgumentsProvider {
            private fun cannotRemoveTagUsages(apiType: ApiType): Arguments {
                val request = Instancio.of(DeleteTagsRequest::class.java).set(field("apiType"), apiType).create()
                val tagUsagesResponse = VocaDbTagUsages(false, Instancio.createList(VocaDbTagUsage::class.java))
                return argumentSet(
                    "apiType=$apiType, user not allowed to remove tags",
                    "User lacks permission to remove tags",
                    request,
                    tagUsagesResponse,
                )
            }

            private fun notTaggedWithTargetTag(apiType: ApiType): Arguments {
                val tagUsagesResponse = VocaDbTagUsages(true, Instancio.createList(VocaDbTagUsage::class.java))
                val request =
                    Instancio.of(DeleteTagsRequest::class.java)
                        .set(field("apiType"), apiType)
                        .generate(field("tags")) { gen -> gen.collection<VocaDbTag>().size(2) }
                        .filter<Long>(field(VocaDbTag::class.java, "id").within(field("tags").toScope())) { tagId ->
                            !tagUsagesResponse.tagUsages.any { tagUsage -> tagUsage.tag.id == tagId }
                        }
                        .create()
                return argumentSet(
                    "apiType=$apiType, tags not present on the entry",
                    "Following tags were not found on the entry: T/${request.tags[0].id} \"${request.tags[0].name}\", T/${request.tags[1].id} \"${request.tags[1].name}\"",
                    request,
                    tagUsagesResponse,
                )
            }

            override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
                return Stream.of(*ApiType.entries.toTypedArray()).flatMap {
                    Stream.of(cannotRemoveTagUsages(it), notTaggedWithTargetTag(it))
                }
            }
        }
    }
}
