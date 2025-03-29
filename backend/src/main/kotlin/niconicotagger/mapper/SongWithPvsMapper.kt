package niconicotagger.mapper

import niconicotagger.constants.Constants.FIRST_WORK_TAG_ID
import niconicotagger.dto.api.misc.AvailableNndVideo
import niconicotagger.dto.api.misc.EventDateBounds
import niconicotagger.dto.api.misc.NndTagData
import niconicotagger.dto.api.misc.NndTagType.MAPPED
import niconicotagger.dto.api.misc.NndTagType.NONE
import niconicotagger.dto.api.misc.PvWithSuggestedTags
import niconicotagger.dto.api.misc.SongEntry
import niconicotagger.dto.api.misc.SongEntryByVocaDbTagForEvent
import niconicotagger.dto.api.misc.UnavailableNndVideo
import niconicotagger.dto.api.misc.VocaDbSongEntryWithPvs
import niconicotagger.dto.api.response.SongsWithPvsResponse
import niconicotagger.dto.inner.nnd.NndThumbnail
import niconicotagger.dto.inner.nnd.NndThumbnailError
import niconicotagger.dto.inner.nnd.NndThumbnailOk
import niconicotagger.dto.inner.vocadb.VocaDbSongEntryWithNndPvsAndTags
import niconicotagger.dto.inner.vocadb.VocaDbSongWithReleaseEvents
import niconicotagger.dto.inner.vocadb.VocaDbTagMapping
import niconicotagger.dto.inner.vocadb.VocaDbTagSelectable
import niconicotagger.dto.inner.vocadb.search.result.SearchResult
import niconicotagger.mapper.Utils.Companion.calculateSongStats
import niconicotagger.serde.Utils.kata2hiraAndLowercase
import org.mapstruct.Context
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingConstants.ComponentModel.SPRING

@Mapper(componentModel = SPRING)
abstract class SongWithPvsMapper {
    fun map(
        searchResult: SearchResult<VocaDbSongEntryWithNndPvsAndTags>,
        nonDisabledPvs: Map<String, NndThumbnail>,
        tagMappings: Map<String, List<VocaDbTagMapping>>,
        likelyFirstWorks: List<Long>
    ): SongsWithPvsResponse {
        val items = mapItems(searchResult.items, nonDisabledPvs, tagMappings, likelyFirstWorks)
        return SongsWithPvsResponse(
            items,
            calculateSongStats(items.map { it.entry }),
            searchResult.totalCount
        )
    }

    @Mapping(
        target = "disposition",
        expression = "java(Utils.calculateDisposition(entry.getPublishedAt(), eventDates))"
    )
    protected abstract fun mapVocaDbSongWithReleaseEvents(
        entry: VocaDbSongWithReleaseEvents,
        @Context eventDates: EventDateBounds
    ): SongEntryByVocaDbTagForEvent

    private fun mapItems(
        songEntries: List<VocaDbSongEntryWithNndPvsAndTags>,
        nonDisabledPvs: Map<String, NndThumbnail>,
        tagMappings: Map<String, List<VocaDbTagMapping>>,
        likelyFirstWorks: List<Long>
    ): List<VocaDbSongEntryWithPvs> {
        return songEntries.map { entry ->
            VocaDbSongEntryWithPvs(
                mapEntry(entry),
                mapAvailablePvs(entry, nonDisabledPvs, tagMappings, likelyFirstWorks.contains(entry.id)),
                mapUnavailablePvs(entry, nonDisabledPvs),
            )
        }
    }

    protected abstract fun mapEntry(songEntry: VocaDbSongEntryWithNndPvsAndTags): SongEntry

    private fun mapAvailablePvs(
        songEntry: VocaDbSongEntryWithNndPvsAndTags,
        nonDisabledPvs: Map<String, NndThumbnail>,
        tagMappings: Map<String, List<VocaDbTagMapping>>,
        likelyFirstWork: Boolean
    ): List<PvWithSuggestedTags> {
        val entryTagIds = songEntry.tags.map { it.id }
        return songEntry.pvs.mapNotNull { pv ->
            val thumbnail = nonDisabledPvs[pv.id] ?: return@mapNotNull null
            when (thumbnail) {
                is NndThumbnailError -> null

                is NndThumbnailOk -> {
                    val tags = thumbnail.data.tags.asSequence()
                        .mapNotNull { tagMappings[kata2hiraAndLowercase(it.name)] }
                        .flatten()
                        .map { VocaDbTagSelectable(it.tag, entryTagIds.contains(it.tag.id)) }
                        .distinctBy { it.tag.id }
                        .filter {
                            songEntry.type.tagIsApplicable(it.tag)
                                    && (likelyFirstWork || it.tag.id != FIRST_WORK_TAG_ID)
                        }
                        .toList()
                    PvWithSuggestedTags(
                        AvailableNndVideo(
                            pv.id,
                            pv.name,
                            thumbnail.data.description,
                            thumbnail.data.tags.map {
                                NndTagData(
                                    it.name,
                                    if (tagMappings.containsKey(kata2hiraAndLowercase(it.name))) MAPPED else NONE,
                                    it.locked
                                )
                            }
                        ),
                        tags
                    )
                }
            }
        }
    }

    private fun mapUnavailablePvs(
        songEntry: VocaDbSongEntryWithNndPvsAndTags,
        nonDisabledPvs: Map<String, NndThumbnail>
    ): List<UnavailableNndVideo> {
        return songEntry.pvs.mapNotNull { pv ->
            val thumbnail = nonDisabledPvs[pv.id] ?: return@mapNotNull null
            when (thumbnail) {
                is NndThumbnailError -> UnavailableNndVideo(
                    pv.id,
                    pv.name,
                    thumbnail.error.code
                )

                is NndThumbnailOk -> null
            }
        }
    }
}
