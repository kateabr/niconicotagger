package niconicotagger.mapper

import Utils.createSampleSongTypeStats
import niconicotagger.constants.Constants.FIRST_WORK_TAG_ID
import niconicotagger.dto.api.misc.AvailableNndVideo
import niconicotagger.dto.api.misc.NndTagData
import niconicotagger.dto.api.misc.NndTagType.MAPPED
import niconicotagger.dto.api.misc.NndTagType.NONE
import niconicotagger.dto.api.misc.PvWithSuggestedTags
import niconicotagger.dto.api.misc.SongEntry
import niconicotagger.dto.api.misc.UnavailableNndVideo
import niconicotagger.dto.api.misc.VocaDbSongEntryWithPvs
import niconicotagger.dto.api.response.SongsWithPvsResponse
import niconicotagger.dto.inner.misc.PvService.NicoNicoDouga
import niconicotagger.dto.inner.misc.PvService.Youtube
import niconicotagger.dto.inner.misc.SongPv
import niconicotagger.dto.inner.misc.SongType.Unspecified
import niconicotagger.dto.inner.nnd.Error
import niconicotagger.dto.inner.nnd.NndTag
import niconicotagger.dto.inner.nnd.NndThumbnailError
import niconicotagger.dto.inner.nnd.NndThumbnailOk
import niconicotagger.dto.inner.nnd.ThumbData
import niconicotagger.dto.inner.vocadb.VocaDbSongEntryWithNndPvsAndTags
import niconicotagger.dto.inner.vocadb.VocaDbTag
import niconicotagger.dto.inner.vocadb.VocaDbTagMapping
import niconicotagger.dto.inner.vocadb.VocaDbTagSelectable
import niconicotagger.dto.inner.vocadb.search.result.VocaDbSongEntryWithNndPvsAndTagsSearchResult
import niconicotagger.serde.Utils.kata2hiraAndLowercase
import org.assertj.core.api.Assertions.assertThat
import org.instancio.Instancio
import org.instancio.Select.field
import org.instancio.TypeToken
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.ArgumentSet
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import org.mapstruct.factory.Mappers
import java.util.stream.Stream

class SongWithPvsMapperTest {
    private val mapper: SongWithPvsMapper = Mappers.getMapper(SongWithPvsMapper::class.java)

    @ParameterizedTest
    @ArgumentsSource(TestData::class)
    fun `SongsWithPvsResponse map test`(
        searchResult: VocaDbSongEntryWithNndPvsAndTagsSearchResult,
        nonDisabledPvs: Map<String, NndThumbnailError>,
        tagMappings: Map<String, List<VocaDbTagMapping>>,
        likelyFirstWorks: List<Long>,
        expectedObject: SongsWithPvsResponse
    ) {
        assertThat(mapper.map(searchResult, nonDisabledPvs, tagMappings, likelyFirstWorks))
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .isEqualTo(expectedObject)
    }

    companion object {
        class TestData : ArgumentsProvider {
            private fun pvShouldBeDisabled(): ArgumentSet {
                val songPvs = listOf(SongPv("id", "name", false, NicoNicoDouga), SongPv("2", "2", false, Youtube))
                val tagMappings =
                    Instancio.ofMap(object : TypeToken<String> {}, object : TypeToken<List<VocaDbTagMapping>> {})
                        .create()
                val searchResult = VocaDbSongEntryWithNndPvsAndTagsSearchResult(
                    Instancio.of(VocaDbSongEntryWithNndPvsAndTags::class.java)
                        .set(field("pvs"), songPvs)
                        .`as` { listOf(it) },
                    Instancio.create(Long::class.java)
                )
                val nonDisabledPvs = mapOf(songPvs[0].id to NndThumbnailError(Instancio.create(Error::class.java)))
                val expectedObject = searchResult.items[0].let {
                    SongsWithPvsResponse(
                        listOf(
                            VocaDbSongEntryWithPvs(

                                SongEntry(
                                    it.id,
                                    it.name,
                                    it.type,
                                    it.artistString,
                                    it.publishedAt
                                ),
                                listOf(),
                                listOf(
                                    UnavailableNndVideo(
                                        songPvs[0].id,
                                        songPvs[0].name,
                                        requireNotNull(nonDisabledPvs[songPvs[0].id]).error.code
                                    )
                                )
                            )
                        ),
                        createSampleSongTypeStats(it.type),
                        searchResult.totalCount
                    )
                }

                return argumentSet(
                    "one NND PV that should be disabled, no tags to assign",
                    searchResult,
                    nonDisabledPvs,
                    tagMappings,
                    emptyList<Long>(),
                    expectedObject
                )
            }

            private fun oneNndPvWithoutMappedTags(): ArgumentSet {
                val searchResult = VocaDbSongEntryWithNndPvsAndTagsSearchResult(
                    Instancio.of(VocaDbSongEntryWithNndPvsAndTags::class.java)
                        .generate(field("pvs")) { gen -> gen.collection<SongPv>().size(1) }
                        .set(field(SongPv::class.java, "service"), NicoNicoDouga)
                        .`as` { listOf(it) },
                    Instancio.create(Long::class.java)
                )
                val nndTags = Instancio.createList(String::class.java)
                val nonDisabledPvs = mapOf(
                    searchResult.items[0].pvs[0].id to Instancio.of(NndThumbnailOk::class.java)
                        .set(field(ThumbData::class.java, "tags"), nndTags.map { NndTag(it, false) })
                        .create()
                )
                val tagMappings = mapOf(
                    kata2hiraAndLowercase(nndTags[0]) to listOf(
                        VocaDbTagMapping(
                            nndTags[0],
                            VocaDbTag(FIRST_WORK_TAG_ID, "first work")
                        )
                    )
                ) + Instancio.ofMap(object : TypeToken<String> {}, object : TypeToken<List<VocaDbTagMapping>> {})
                    .filter<String>(field(VocaDbTagMapping::class.java, "sourceTag")) { !nndTags.contains(it) }
                    .create()
                val expectedObject = searchResult.items[0].let { item ->
                    SongsWithPvsResponse(
                        listOf(
                            VocaDbSongEntryWithPvs(
                                SongEntry(
                                    item.id,
                                    item.name,
                                    item.type,
                                    item.artistString,
                                    item.publishedAt
                                ),
                                listOf(
                                    requireNotNull(nonDisabledPvs[searchResult.items[0].pvs[0].id]).data.let {
                                        PvWithSuggestedTags(
                                            AvailableNndVideo(
                                                searchResult.items[0].pvs[0].id,
                                                searchResult.items[0].pvs[0].name,
                                                it.description,
                                                listOf(
                                                    NndTagData(
                                                        it.tags[0].name,
                                                        MAPPED,
                                                        false
                                                    )
                                                ) + it.tags.drop(1).map { NndTagData(it.name, NONE, it.locked) }
                                            ),
                                            emptyList()
                                        )
                                    }
                                ),
                                emptyList()
                            )
                        ),
                        createSampleSongTypeStats(item.type),
                        searchResult.totalCount
                    )
                }

                return argumentSet(
                    "one NND PV that is only tagged with \"first work\" but in reality it is not the first one (no tags suggested)",
                    searchResult,
                    nonDisabledPvs,
                    tagMappings,
                    emptyList<Long>(),
                    expectedObject
                )
            }

            private fun twoNndPvsWithDifferentTags(): ArgumentSet {
                val tagMappings = mapOf(
                    "1" to listOf(VocaDbTagMapping("1", VocaDbTag(1, "1")), VocaDbTagMapping("1", VocaDbTag(11, "11"))),
                    "2" to listOf(VocaDbTagMapping("2", VocaDbTag(2, "2")), VocaDbTagMapping("2", VocaDbTag(22, "22"))),
                    "3" to listOf(VocaDbTagMapping("3", VocaDbTag(3, "3")), VocaDbTagMapping("3", VocaDbTag(33, "33"))),
                    "4" to listOf(VocaDbTagMapping("4", VocaDbTag(4, "4")), VocaDbTagMapping("4", VocaDbTag(44, "44"))),
                    "first_work" to listOf(VocaDbTagMapping("first_work", VocaDbTag(FIRST_WORK_TAG_ID, "first work")))
                )
                val searchResult = VocaDbSongEntryWithNndPvsAndTagsSearchResult(
                    Instancio.of(VocaDbSongEntryWithNndPvsAndTags::class.java)
                        .set(field("type"), Unspecified) // does not have ignored tags, easier to check this way
                        .generate(field("pvs")) { gen -> gen.collection<SongPv>().size(2) }
                        .set(field(SongPv::class.java, "service"), NicoNicoDouga)
                        .set(
                            field("tags"),
                            listOf(VocaDbTag(1, "1"), VocaDbTag(2, "2"), VocaDbTag(3, "3"), VocaDbTag(4, "4"))
                        )
                        .withUnique(field(SongPv::class.java, "id"))
                        .`as` { listOf(it) },
                    Instancio.create(Long::class.java)
                )
                val nonDisabledPvs = mapOf(
                    searchResult.items[0].pvs[0].id to Instancio.of(NndThumbnailOk::class.java)
                        .set(
                            field(ThumbData::class.java, "tags"),
                            listOf(NndTag("1", false), NndTag("2", true), NndTag("first_work", true))
                        )
                        .create(),
                    searchResult.items[0].pvs[1].id to Instancio.of(NndThumbnailOk::class.java)
                        .set(field(ThumbData::class.java, "tags"), listOf(NndTag("3", true), NndTag("4", false)))
                        .create()
                )
                val expectedObject = SongsWithPvsResponse(
                    listOf(
                        VocaDbSongEntryWithPvs(
                            SongEntry(
                                searchResult.items[0].id,
                                searchResult.items[0].name,
                                searchResult.items[0].type,
                                searchResult.items[0].artistString,
                                searchResult.items[0].publishedAt
                            ),
                            listOf(
                                PvWithSuggestedTags(
                                    AvailableNndVideo(
                                        searchResult.items[0].pvs[0].id,
                                        searchResult.items[0].pvs[0].name,
                                        requireNotNull(nonDisabledPvs[searchResult.items[0].pvs[0].id]).data.description,
                                        listOf(
                                            NndTagData("1", MAPPED, false),
                                            NndTagData("2", MAPPED, true),
                                            NndTagData("first_work", MAPPED, true)
                                        ),
                                    ),
                                    listOf(
                                        VocaDbTagSelectable(VocaDbTag(1, "1"), true),
                                        VocaDbTagSelectable(VocaDbTag(11, "11"), false),
                                        VocaDbTagSelectable(VocaDbTag(2, "2"), true),
                                        VocaDbTagSelectable(VocaDbTag(22, "22"), false),
                                        VocaDbTagSelectable(VocaDbTag(FIRST_WORK_TAG_ID, "first work"), false)
                                    )
                                ),
                                PvWithSuggestedTags(
                                    AvailableNndVideo(
                                        searchResult.items[0].pvs[1].id,
                                        searchResult.items[0].pvs[1].name,
                                        requireNotNull(nonDisabledPvs[searchResult.items[0].pvs[1].id]).data.description,
                                        listOf(NndTagData("3", MAPPED, true), NndTagData("4", MAPPED, false)),
                                    ),
                                    listOf(
                                        VocaDbTagSelectable(VocaDbTag(3, "3"), true),
                                        VocaDbTagSelectable(VocaDbTag(33, "33"), false),
                                        VocaDbTagSelectable(VocaDbTag(4, "4"), true),
                                        VocaDbTagSelectable(VocaDbTag(44, "44"), false)
                                    )
                                )
                            ),
                            emptyList()
                        )
                    ),
                    createSampleSongTypeStats(searchResult.items[0].type),
                    searchResult.totalCount
                )

                return argumentSet(
                    "two NND PVs with tags that have 2 mappings each, one of the mapped tags already present on the entry; one is tagged with \"first work\" and it likely is",
                    searchResult,
                    nonDisabledPvs,
                    tagMappings,
                    listOf(searchResult.items[0].id),
                    expectedObject
                )
            }

            override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
                return Stream.of(
                    pvShouldBeDisabled(),
                    oneNndPvWithoutMappedTags(),
                    twoNndPvsWithDifferentTags()
                )
            }

        }
    }
}
