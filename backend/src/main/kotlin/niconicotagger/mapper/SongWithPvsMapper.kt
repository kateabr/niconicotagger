package niconicotagger.mapper

import niconicotagger.configuration.ClientSpecificDbTagProps
import niconicotagger.dto.api.misc.AvailableNndVideo
import niconicotagger.dto.api.misc.EventDateBounds
import niconicotagger.dto.api.misc.NndTagData
import niconicotagger.dto.api.misc.NndTagType.MAPPED
import niconicotagger.dto.api.misc.NndTagType.NONE
import niconicotagger.dto.api.misc.PvWithSuggestedTags
import niconicotagger.dto.api.misc.SongEntryByVocaDbTagForEvent
import niconicotagger.dto.api.misc.SongEntryWithPublishDateAndReleaseEventInfo
import niconicotagger.dto.api.misc.UnavailableNndVideo
import niconicotagger.dto.api.misc.VocaDbSongEntryWithPvs
import niconicotagger.dto.api.response.SongsWithPvsResponse
import niconicotagger.dto.inner.nnd.GenericNndErrorVideoData
import niconicotagger.dto.inner.nnd.GenericNndOkVideoData
import niconicotagger.dto.inner.nnd.GenericNndVideoData
import niconicotagger.dto.inner.vocadb.VocaDbSongEntryWithNndPvsTagsAndReleaseEvents
import niconicotagger.dto.inner.vocadb.VocaDbSongWithReleaseEvents
import niconicotagger.dto.inner.vocadb.VocaDbTag
import niconicotagger.dto.inner.vocadb.VocaDbTagMapping
import niconicotagger.dto.inner.vocadb.VocaDbTagSelectable
import niconicotagger.dto.inner.vocadb.search.result.SearchResult
import niconicotagger.mapper.Utils.calculateSongStats
import niconicotagger.serde.Utils.kata2hiraAndLowercase
import org.mapstruct.Context
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingConstants.ComponentModel.SPRING

@Mapper(componentModel = SPRING)
abstract class SongWithPvsMapper {
    fun map(
        searchResult: SearchResult<VocaDbSongEntryWithNndPvsTagsAndReleaseEvents>,
        nonDisabledPvs: Map<String, GenericNndVideoData>,
        tagMappings: Map<String, List<VocaDbTagMapping>>,
        likelyFirstWorks: List<Long>,
        regionBlocked: Collection<String>,
        tagProps: ClientSpecificDbTagProps,
    ): SongsWithPvsResponse {
        val items = mapItems(searchResult.items, nonDisabledPvs, tagMappings, likelyFirstWorks, regionBlocked, tagProps)
        return SongsWithPvsResponse(items, calculateSongStats(items.map { it.entry }), searchResult.totalCount)
    }

    @Mapping(
        target = "disposition",
        expression = "java(Utils.calculateDisposition(entry.getPublishedAt(), eventDates))",
    )
    protected abstract fun mapVocaDbSongWithReleaseEvents(
        entry: VocaDbSongWithReleaseEvents,
        @Context eventDates: EventDateBounds,
    ): SongEntryByVocaDbTagForEvent

    private fun mapItems(
        songEntries: List<VocaDbSongEntryWithNndPvsTagsAndReleaseEvents>,
        nonDisabledPvs: Map<String, GenericNndVideoData>,
        tagMappings: Map<String, List<VocaDbTagMapping>>,
        likelyFirstWorks: List<Long>,
        regionBlocked: Collection<String>,
        tagProps: ClientSpecificDbTagProps,
    ): List<VocaDbSongEntryWithPvs> {
        return songEntries.map { entry ->
            VocaDbSongEntryWithPvs(
                mapEntry(entry),
                mapAvailablePvs(
                    entry,
                    nonDisabledPvs,
                    tagMappings,
                    likelyFirstWorks.contains(entry.id),
                    regionBlocked,
                    tagProps,
                ),
                mapUnavailablePvs(entry, nonDisabledPvs, tagProps),
            )
        }
    }

    protected abstract fun mapEntry(
        songEntry: VocaDbSongEntryWithNndPvsTagsAndReleaseEvents
    ): SongEntryWithPublishDateAndReleaseEventInfo

    private fun mapAvailablePvs(
        songEntry: VocaDbSongEntryWithNndPvsTagsAndReleaseEvents,
        nonDisabledPvs: Map<String, GenericNndVideoData>,
        tagMappings: Map<String, List<VocaDbTagMapping>>,
        likelyFirstWork: Boolean,
        regionBlocked: Collection<String>,
        tagProps: ClientSpecificDbTagProps,
    ): List<PvWithSuggestedTags> {
        val entryTagIds = songEntry.tags.map { it.id }
        return songEntry.pvs.mapNotNull { pv ->
            val pvData = nonDisabledPvs[pv.id] ?: return@mapNotNull null
            when (pvData) {
                is GenericNndErrorVideoData -> null

                is GenericNndOkVideoData -> {
                    val tags =
                        pvData
                            .tags()
                            .asSequence()
                            .mapNotNull { tagMappings[kata2hiraAndLowercase(it.name)] }
                            .flatten()
                            .map { VocaDbTagSelectable(it.tag, entryTagIds.contains(it.tag.id)) }
                            .plus(
                                if (regionBlocked.contains(pv.id))
                                    listOf(
                                        VocaDbTagSelectable(
                                            tagProps.regionBlocked.let { VocaDbTag(it.id, it.name!!) },
                                            entryTagIds.contains(tagProps.regionBlocked.id),
                                        )
                                    )
                                else emptyList()
                            )
                            .distinctBy { it.tag.id }
                            .filter {
                                songEntry.type.tagIsApplicable(it.tag) &&
                                    (likelyFirstWork || it.tag.id != tagProps.firstWork.id)
                            }
                            .toList()
                    PvWithSuggestedTags(
                        AvailableNndVideo(
                            pv.id,
                            pv.name,
                            pvData.description(),
                            pvData.tags().map {
                                NndTagData(
                                    it.name,
                                    if (tagMappings.containsKey(kata2hiraAndLowercase(it.name))) MAPPED else NONE,
                                    it.locked,
                                )
                            },
                        ),
                        tags,
                    )
                }
            }
        }
    }

    private fun mapUnavailablePvs(
        songEntry: VocaDbSongEntryWithNndPvsTagsAndReleaseEvents,
        nonDisabledPvs: Map<String, GenericNndVideoData>,
        tagProps: ClientSpecificDbTagProps,
    ): List<UnavailableNndVideo> {
        return songEntry.pvs.mapNotNull { pv ->
            val pvData = nonDisabledPvs[pv.id] ?: return@mapNotNull null
            when (pvData) {
                is GenericNndErrorVideoData ->
                    if (pvData.code() != tagProps.regionBlocked.embedErrorCode)
                        UnavailableNndVideo(pv.id, pv.name, pvData.code())
                    else null

                is GenericNndOkVideoData -> null
            }
        }
    }
}
