package niconicotagger.mapper

import java.util.stream.Stream
import niconicotagger.Utils.createSampleSongTypeStats
import niconicotagger.configuration.ClientSpecificDbTagProps
import niconicotagger.configuration.ClientSpecificDbTagProps.TagProps
import niconicotagger.dto.api.misc.AvailableNndVideo
import niconicotagger.dto.api.misc.NndTagData
import niconicotagger.dto.api.misc.NndTagType.MAPPED
import niconicotagger.dto.api.misc.NndTagType.NONE
import niconicotagger.dto.api.misc.PvWithSuggestedTags
import niconicotagger.dto.api.misc.ReleaseEvent
import niconicotagger.dto.api.misc.SongEntryWithPublishDateAndReleaseEventInfo
import niconicotagger.dto.api.misc.UnavailableNndVideo
import niconicotagger.dto.api.misc.VocaDbSongEntryWithPvs
import niconicotagger.dto.api.response.SongsWithPvsResponse
import niconicotagger.dto.inner.misc.PvService.NicoNicoDouga
import niconicotagger.dto.inner.misc.PvService.Youtube
import niconicotagger.dto.inner.misc.SongPv
import niconicotagger.dto.inner.misc.SongType.Unspecified
import niconicotagger.dto.inner.nnd.Error
import niconicotagger.dto.inner.nnd.NndEmbedOk
import niconicotagger.dto.inner.nnd.NndTag
import niconicotagger.dto.inner.nnd.NndThumbnailError
import niconicotagger.dto.inner.nnd.NndThumbnailOk
import niconicotagger.dto.inner.nnd.ThumbData
import niconicotagger.dto.inner.vocadb.VocaDbSongEntryWithNndPvsTagsAndReleaseEvents
import niconicotagger.dto.inner.vocadb.VocaDbTag
import niconicotagger.dto.inner.vocadb.VocaDbTagMapping
import niconicotagger.dto.inner.vocadb.VocaDbTagSelectable
import niconicotagger.dto.inner.vocadb.search.result.VocaDbSongEntryWithNndPvsTagsAndReleaseEventsSearchResult
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

class SongWithPvsMapperTest {
    private val mapper: SongWithPvsMapper = Mappers.getMapper(SongWithPvsMapper::class.java)

    @ParameterizedTest
    @ArgumentsSource(TestData::class)
    fun `SongsWithPvsResponse map test`(
        searchResult: VocaDbSongEntryWithNndPvsTagsAndReleaseEventsSearchResult,
        nonDisabledPvs: Map<String, NndThumbnailError>,
        tagMappings: Map<String, List<VocaDbTagMapping>>,
        likelyFirstWorks: List<Long>,
        regionBlocked: List<String>,
        expectedObject: SongsWithPvsResponse,
    ) {
        assertThat(mapper.map(searchResult, nonDisabledPvs, tagMappings, likelyFirstWorks, regionBlocked, tagProps))
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .isEqualTo(expectedObject)
    }

    companion object {
        private val tagProps =
            ClientSpecificDbTagProps(TagProps(null, -1, null), TagProps("region_blocked", -2, "region blocked"))

        class TestData : ArgumentsProvider {
            private fun pvShouldBeDisabled(): ArgumentSet {
                val songPvs = listOf(SongPv("id", "name", false, NicoNicoDouga), SongPv("2", "2", false, Youtube))
                val tagMappings =
                    Instancio.ofMap(object : TypeToken<String> {}, object : TypeToken<List<VocaDbTagMapping>> {})
                        .create()
                val searchResult =
                    VocaDbSongEntryWithNndPvsTagsAndReleaseEventsSearchResult(
                        Instancio.of(VocaDbSongEntryWithNndPvsTagsAndReleaseEvents::class.java)
                            .set(field("pvs"), songPvs)
                            .`as` { listOf(it) },
                        Instancio.create(Long::class.java),
                    )
                val nonDisabledPvs = mapOf(songPvs[0].id to NndThumbnailError(Instancio.create(Error::class.java)))
                val expectedObject =
                    searchResult.items[0].let {
                        SongsWithPvsResponse(
                            listOf(
                                VocaDbSongEntryWithPvs(
                                    SongEntryWithPublishDateAndReleaseEventInfo(
                                        it.id,
                                        it.name,
                                        it.type,
                                        it.artistString,
                                        it.publishedAt,
                                        it.events.map { event -> ReleaseEvent(event.id, event.name, event.seriesId) },
                                    ),
                                    listOf(),
                                    listOf(
                                        UnavailableNndVideo(
                                            songPvs[0].id,
                                            songPvs[0].name,
                                            requireNotNull(nonDisabledPvs[songPvs[0].id]).error.code,
                                        )
                                    ),
                                )
                            ),
                            createSampleSongTypeStats(it.type),
                            searchResult.totalCount,
                        )
                    }

                return argumentSet(
                    "one NND PV that should be disabled, no tags to assign",
                    searchResult,
                    nonDisabledPvs,
                    tagMappings,
                    emptyList<Long>(),
                    emptyList<String>(),
                    expectedObject,
                )
            }

            private fun regionBlockedPv(hasTag: Boolean): ArgumentSet {
                val regionBlockedTag = VocaDbTag(tagProps.regionBlocked.id, tagProps.regionBlocked.name!!)

                val searchResult =
                    VocaDbSongEntryWithNndPvsTagsAndReleaseEventsSearchResult(
                        Instancio.of(VocaDbSongEntryWithNndPvsTagsAndReleaseEvents::class.java)
                            .generate(field("pvs")) { gen -> gen.collection<SongPv>().size(1) }
                            .set(field(SongPv::class.java, "service"), NicoNicoDouga)
                            .set(
                                field("tags"),
                                if (hasTag) {
                                    listOf(regionBlockedTag)
                                } else emptyList(),
                            )
                            .`as` { listOf(it) },
                        Instancio.create(Long::class.java),
                    )
                val nonDisabledPvs =
                    mapOf(searchResult.items[0].pvs[0].id to Instancio.create(NndThumbnailOk::class.java))

                val expectedObject =
                    searchResult.items[0].let { item ->
                        SongsWithPvsResponse(
                            listOf(
                                VocaDbSongEntryWithPvs(
                                    SongEntryWithPublishDateAndReleaseEventInfo(
                                        item.id,
                                        item.name,
                                        item.type,
                                        item.artistString,
                                        item.publishedAt,
                                        item.events.map { event -> ReleaseEvent(event.id, event.name, event.seriesId) },
                                    ),
                                    listOf(
                                        requireNotNull(nonDisabledPvs[searchResult.items[0].pvs[0].id]).data.let {
                                            PvWithSuggestedTags(
                                                AvailableNndVideo(
                                                    searchResult.items[0].pvs[0].id,
                                                    searchResult.items[0].pvs[0].name,
                                                    it.description,
                                                    it.tags.map { NndTagData(it.name, NONE, it.locked) },
                                                ),
                                                listOf(VocaDbTagSelectable(regionBlockedTag, hasTag)),
                                            )
                                        }
                                    ),
                                    emptyList(),
                                )
                            ),
                            createSampleSongTypeStats(item.type),
                            searchResult.totalCount,
                        )
                    }

                return argumentSet(
                    "one region-blocked NND PV; entry is ${if (hasTag) "already" else "not"} tagged as such",
                    searchResult,
                    nonDisabledPvs,
                    emptyMap<String, List<VocaDbTagMapping>>(),
                    emptyList<String>(),
                    listOf(searchResult.items[0].pvs[0].id),
                    expectedObject,
                )
            }

            private fun oneNndPvWithoutMappedTags(): ArgumentSet {
                val searchResult =
                    VocaDbSongEntryWithNndPvsTagsAndReleaseEventsSearchResult(
                        Instancio.of(VocaDbSongEntryWithNndPvsTagsAndReleaseEvents::class.java)
                            .generate(field("pvs")) { gen -> gen.collection<SongPv>().size(1) }
                            .set(field(SongPv::class.java, "service"), NicoNicoDouga)
                            .`as` { listOf(it) },
                        Instancio.create(Long::class.java),
                    )
                val nndTags = Instancio.createList(String::class.java)
                val nonDisabledPvs =
                    mapOf(
                        searchResult.items[0].pvs[0].id to
                            Instancio.of(NndThumbnailOk::class.java)
                                .set(field(ThumbData::class.java, "tags"), nndTags.map { NndTag(it, false) })
                                .create()
                    )
                val tagMappings =
                    mapOf(
                        kata2hiraAndLowercase(nndTags[0]) to
                            listOf(VocaDbTagMapping(nndTags[0], VocaDbTag(tagProps.firstWork.id, "first work")))
                    ) +
                        Instancio.ofMap(object : TypeToken<String> {}, object : TypeToken<List<VocaDbTagMapping>> {})
                            .filter<String>(field(VocaDbTagMapping::class.java, "sourceTag")) { !nndTags.contains(it) }
                            .create()
                val expectedObject =
                    searchResult.items[0].let { item ->
                        SongsWithPvsResponse(
                            listOf(
                                VocaDbSongEntryWithPvs(
                                    SongEntryWithPublishDateAndReleaseEventInfo(
                                        item.id,
                                        item.name,
                                        item.type,
                                        item.artistString,
                                        item.publishedAt,
                                        item.events.map { event -> ReleaseEvent(event.id, event.name, event.seriesId) },
                                    ),
                                    listOf(
                                        requireNotNull(nonDisabledPvs[searchResult.items[0].pvs[0].id]).data.let {
                                            PvWithSuggestedTags(
                                                AvailableNndVideo(
                                                    searchResult.items[0].pvs[0].id,
                                                    searchResult.items[0].pvs[0].name,
                                                    it.description,
                                                    listOf(NndTagData(it.tags[0].name, MAPPED, false)) +
                                                        it.tags.drop(1).map { NndTagData(it.name, NONE, it.locked) },
                                                ),
                                                emptyList(),
                                            )
                                        }
                                    ),
                                    emptyList(),
                                )
                            ),
                            createSampleSongTypeStats(item.type),
                            searchResult.totalCount,
                        )
                    }

                return argumentSet(
                    "one NND PV that is only tagged with \"first work\" but in reality it is not the first one (no tags suggested)",
                    searchResult,
                    nonDisabledPvs,
                    tagMappings,
                    emptyList<Long>(),
                    emptyList<String>(),
                    expectedObject,
                )
            }

            private fun twoNndPvsWithDifferentTags(): ArgumentSet {
                val tagMappings =
                    mapOf(
                        "1" to
                            listOf(
                                VocaDbTagMapping("1", VocaDbTag(1, "1")),
                                VocaDbTagMapping("1", VocaDbTag(11, "11")),
                            ),
                        "2" to
                            listOf(
                                VocaDbTagMapping("2", VocaDbTag(2, "2")),
                                VocaDbTagMapping("2", VocaDbTag(22, "22")),
                            ),
                        "3" to
                            listOf(
                                VocaDbTagMapping("3", VocaDbTag(3, "3")),
                                VocaDbTagMapping("3", VocaDbTag(33, "33")),
                            ),
                        "4" to
                            listOf(
                                VocaDbTagMapping("4", VocaDbTag(4, "4")),
                                VocaDbTagMapping("4", VocaDbTag(44, "44")),
                            ),
                        "first_work" to
                            listOf(VocaDbTagMapping("first_work", VocaDbTag(tagProps.firstWork.id, "first work"))),
                    )
                val searchResult =
                    VocaDbSongEntryWithNndPvsTagsAndReleaseEventsSearchResult(
                        Instancio.of(VocaDbSongEntryWithNndPvsTagsAndReleaseEvents::class.java)
                            .set(field("type"), Unspecified) // does not have ignored tags, easier to check this way
                            .generate(field("pvs")) { gen -> gen.collection<SongPv>().size(2) }
                            .set(field(SongPv::class.java, "service"), NicoNicoDouga)
                            .set(
                                field("tags"),
                                listOf(VocaDbTag(1, "1"), VocaDbTag(2, "2"), VocaDbTag(3, "3"), VocaDbTag(4, "4")),
                            )
                            .withUnique(field(SongPv::class.java, "id"))
                            .`as` { listOf(it) },
                        Instancio.create(Long::class.java),
                    )
                val nonDisabledPvs =
                    mapOf(
                        searchResult.items[0].pvs[0].id to
                            Instancio.of(NndEmbedOk::class.java)
                                .set(field("tags"), listOf("1", "2", "first_work"))
                                .create(),
                        searchResult.items[0].pvs[1].id to
                            Instancio.of(NndThumbnailOk::class.java)
                                .set(
                                    field(ThumbData::class.java, "tags"),
                                    listOf(NndTag("3", true), NndTag("4", false)),
                                )
                                .create(),
                    )
                val expectedObject =
                    SongsWithPvsResponse(
                        listOf(
                            VocaDbSongEntryWithPvs(
                                SongEntryWithPublishDateAndReleaseEventInfo(
                                    searchResult.items[0].id,
                                    searchResult.items[0].name,
                                    searchResult.items[0].type,
                                    searchResult.items[0].artistString,
                                    searchResult.items[0].publishedAt,
                                    searchResult.items[0].events.map { event ->
                                        ReleaseEvent(event.id, event.name, event.seriesId)
                                    },
                                ),
                                listOf(
                                    PvWithSuggestedTags(
                                        AvailableNndVideo(
                                            searchResult.items[0].pvs[0].id,
                                            searchResult.items[0].pvs[0].name,
                                            requireNotNull(nonDisabledPvs[searchResult.items[0].pvs[0].id])
                                                .description(),
                                            listOf(
                                                NndTagData("1", MAPPED, false),
                                                NndTagData("2", MAPPED, false),
                                                NndTagData("first_work", MAPPED, false),
                                            ),
                                        ),
                                        listOf(
                                            VocaDbTagSelectable(VocaDbTag(1, "1"), true),
                                            VocaDbTagSelectable(VocaDbTag(11, "11"), false),
                                            VocaDbTagSelectable(VocaDbTag(2, "2"), true),
                                            VocaDbTagSelectable(VocaDbTag(22, "22"), false),
                                            VocaDbTagSelectable(VocaDbTag(tagProps.firstWork.id, "first work"), false),
                                        ),
                                    ),
                                    PvWithSuggestedTags(
                                        AvailableNndVideo(
                                            searchResult.items[0].pvs[1].id,
                                            searchResult.items[0].pvs[1].name,
                                            requireNotNull(nonDisabledPvs[searchResult.items[0].pvs[1].id])
                                                .description(),
                                            listOf(NndTagData("3", MAPPED, true), NndTagData("4", MAPPED, false)),
                                        ),
                                        listOf(
                                            VocaDbTagSelectable(VocaDbTag(3, "3"), true),
                                            VocaDbTagSelectable(VocaDbTag(33, "33"), false),
                                            VocaDbTagSelectable(VocaDbTag(4, "4"), true),
                                            VocaDbTagSelectable(VocaDbTag(44, "44"), false),
                                        ),
                                    ),
                                ),
                                emptyList(),
                            )
                        ),
                        createSampleSongTypeStats(searchResult.items[0].type),
                        searchResult.totalCount,
                    )

                return argumentSet(
                    "two NND PVs with tags that have 2 mappings each, one of the mapped tags already present on the entry; one is tagged with \"first work\" and it likely is",
                    searchResult,
                    nonDisabledPvs,
                    tagMappings,
                    listOf(searchResult.items[0].id),
                    emptyList<String>(),
                    expectedObject,
                )
            }

            override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
                return Stream.of(
                    pvShouldBeDisabled(),
                    regionBlockedPv(true),
                    regionBlockedPv(false),
                    oneNndPvWithoutMappedTags(),
                    twoNndPvsWithDifferentTags(),
                )
            }
        }
    }
}
