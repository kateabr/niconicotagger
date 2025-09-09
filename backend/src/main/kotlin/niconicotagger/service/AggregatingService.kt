package niconicotagger.service

import java.time.Duration
import java.time.ZoneOffset.UTC
import java.time.format.DateTimeFormatter.ISO_DATE_TIME
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import niconicotagger.client.DbClientHolder
import niconicotagger.client.NndClient
import niconicotagger.configuration.DbTagProps
import niconicotagger.dto.api.misc.ClientType
import niconicotagger.dto.api.misc.NndSortOrder
import niconicotagger.dto.api.misc.NndSortOrder.LIKE_COUNT
import niconicotagger.dto.api.misc.NndSortOrder.PUBLISH_TIME
import niconicotagger.dto.api.misc.NndSortOrder.VIEW_COUNT
import niconicotagger.dto.api.misc.NndTagType.NONE
import niconicotagger.dto.api.misc.NndVideoWithAssociatedVocaDbEntry
import niconicotagger.dto.api.misc.QueryConsoleData
import niconicotagger.dto.api.misc.SongEntryBase
import niconicotagger.dto.api.request.EventScheduleRequest
import niconicotagger.dto.api.request.GetReleaseEventRequest
import niconicotagger.dto.api.request.QueryConsoleRequest
import niconicotagger.dto.api.request.SongsWithPvsRequest
import niconicotagger.dto.api.request.VideosByNndEventTagsRequest
import niconicotagger.dto.api.request.VideosByNndTagsRequest
import niconicotagger.dto.api.request.VideosByVocaDbTagRequest
import niconicotagger.dto.api.response.EventScheduleResponse
import niconicotagger.dto.api.response.QueryConsoleResponse
import niconicotagger.dto.api.response.ReleaseEventWithVocaDbTagsResponse
import niconicotagger.dto.api.response.ReleaseEventWitnNndTagsResponse
import niconicotagger.dto.api.response.SongsWithPvsResponse
import niconicotagger.dto.api.response.VideosByNndTagsResponseForEvent
import niconicotagger.dto.api.response.VideosByNndTagsResponseForTagging
import niconicotagger.dto.api.response.VideosByVocaDbTagResponse
import niconicotagger.dto.inner.misc.ArtistRole.Composer
import niconicotagger.dto.inner.misc.ArtistRole.Default
import niconicotagger.dto.inner.misc.ArtistType.Producer
import niconicotagger.dto.inner.misc.EntryField.Artists
import niconicotagger.dto.inner.misc.EntryField.ReleaseEvent
import niconicotagger.dto.inner.misc.EntryField.Tags
import niconicotagger.dto.inner.misc.EntryField.WebLinks
import niconicotagger.dto.inner.misc.PvService.NicoNicoDouga
import niconicotagger.dto.inner.misc.ReleaseEventCategory
import niconicotagger.dto.inner.misc.TagTypeHolder
import niconicotagger.dto.inner.nnd.GenericNndOkVideoData
import niconicotagger.dto.inner.nnd.GenericNndVideoData
import niconicotagger.dto.inner.nnd.NndEmbed
import niconicotagger.dto.inner.nnd.NndEmbedError
import niconicotagger.dto.inner.nnd.NndEmbedOk
import niconicotagger.dto.inner.vocadb.VocaDbSongEntryWithTags
import niconicotagger.dto.inner.vocadb.VocaDbSongEntryWithTagsBase
import niconicotagger.dto.inner.vocadb.VocaDbSongWithReleaseEvents
import niconicotagger.dto.inner.vocadb.VocaDbTag
import niconicotagger.dto.inner.vocadb.VocaDbTagSelectable
import niconicotagger.mapper.NndVideoWithAssociatedVocaDbEntryMapper
import niconicotagger.mapper.QueryResponseMapper
import niconicotagger.mapper.ReleaseEventMapper
import niconicotagger.mapper.RequestMapper
import niconicotagger.mapper.SongWithPvsMapper
import niconicotagger.mapper.Utils.calculateSongStats
import niconicotagger.serde.Utils.kata2hiraAndLowercase
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class AggregatingService(
    private val dbClientHolder: DbClientHolder,
    private val nndClient: NndClient,
    private val eventMapper: ReleaseEventMapper,
    private val videoWithAssociatedEntryMapper: NndVideoWithAssociatedVocaDbEntryMapper,
    private val requestMapper: RequestMapper,
    private val queryResponseMapper: QueryResponseMapper,
    private val songWithPvsMapper: SongWithPvsMapper,
    private val publisherInfoService: PublisherInfoService,
    @Value("\${app.service.offline-events}") private val offlineEvents: Set<ReleaseEventCategory>,
    @Value("\${app.service.default-event-scope}") private val defaultEventScope: Duration,
    private val tagProps: DbTagProps,
) {
    private fun getClient(clientType: ClientType) = dbClientHolder.getClient(clientType)

    private fun getClientSpecificTagProps(clientType: ClientType) = tagProps.getClientSpecificProps(clientType)

    internal fun <T : NndVideoWithAssociatedVocaDbEntry<T1>, T1 : SongEntryBase> sortResults(
        result: MutableList<T>,
        nndSortOrder: NndSortOrder,
    ) {
        result.sortWith(
            compareBy(
                {
                    when (nndSortOrder) {
                        PUBLISH_TIME -> -it.video.publishedAt.toEpochMilli()
                        VIEW_COUNT -> -it.video.viewCounter
                        LIKE_COUNT -> -it.video.likeCounter
                    }
                },
                { it.video.id },
            )
        )
    }

    suspend fun getReleaseEventByName(request: GetReleaseEventRequest): ReleaseEventWitnNndTagsResponse {
        val event = getClient(request.clientType).getEventByName(request.eventName, WebLinks)
        val series = event.seriesId?.let { getClient(request.clientType).getEventSeriesById(event.seriesId, WebLinks) }
        val mappedEvent = eventMapper.mapWithLinks(event, series)
        require(mappedEvent.nndTags.isNotEmpty()) { "Event has no linked NND tags" }
        return mappedEvent
    }

    suspend fun getVideosByEventNndTags(request: VideosByNndEventTagsRequest): VideosByNndTagsResponseForEvent {
        val tagStyleHolder =
            TagTypeHolder()
                .storeRequestTags(request)
                .storeTagMappings(getClient(request.clientType).getAllVocaDbTagMappings(true))

        val videos = nndClient.getVideosByTags(request)
        val result =
            coroutineScope {
                    videos.data.map { video ->
                        async {
                            video.tags.forEach { tagStyleHolder.put(it, NONE) }
                            val videoTagsWithStyle = video.tags.associateWith { tagStyleHolder.get(it) }

                            val description = async {
                                video.description ?: extractDescription(nndClient.getEmbedInfo(video.id))
                            }
                            val songEntry = async {
                                getClient(request.clientType)
                                    .getSongByNndPv(
                                        VocaDbSongWithReleaseEvents::class.java,
                                        request.eventId,
                                        video.id,
                                        ReleaseEvent,
                                    )
                            }
                            val publisher = async {
                                if (songEntry.await() == null)
                                    publisherInfoService.getPublisher(video, request.clientType)
                                else null
                            }

                            videoWithAssociatedEntryMapper.mapForEvent(
                                video,
                                songEntry.await(),
                                minOf(songEntry.await()?.publishedAt ?: video.publishedAt, video.publishedAt),
                                request.dates,
                                videoTagsWithStyle,
                                description.await(),
                                publisher.await(),
                            )
                        }
                    }
                }
                .awaitAll()
                .toMutableList()

        sortResults(result, request.orderBy)

        return VideosByNndTagsResponseForEvent(result, videos.meta.totalCount, request.scope)
    }

    suspend fun getVideosByNndTags(
        request: VideosByNndTagsRequest,
        prefetchedTagId: Long? = null,
    ): VideosByNndTagsResponseForTagging {
        val tagStyleHolder = TagTypeHolder().storeRequestTags(request)

        val mappedTags = getClient(request.clientType).getAllVocaDbTagMappings(request.startOffset != 0L)
        val correspondingVocaDbTags =
            if (prefetchedTagId == null)
            // NND tag as a source, compare by tag name
            mappedTags.filter { request.tags.contains(kata2hiraAndLowercase(it.sourceTag)) }.map { it.tag }.toSet()
            else
            // DB tag as a source, compare by tag id
            mappedTags.filter { it.tag.id == prefetchedTagId }.map { it.tag }.toSet()
        if (correspondingVocaDbTags.isEmpty()) error("None of the tags in ${request.tags} is mapped")
        tagStyleHolder.storeTagMappings(mappedTags)

        val videos = nndClient.getVideosByTags(request)
        val result =
            coroutineScope {
                    videos.data.map { video ->
                        video.tags.forEach { tagStyleHolder.put(it, NONE) }
                        val videoTagsWithStyle = video.tags.associateWith { tagStyleHolder.get(it) }
                        async {
                            val songEntry =
                                getClient(request.clientType)
                                    .getSongByNndPv(VocaDbSongEntryWithTags::class.java, video.id, Tags, Artists)
                            val publisher =
                                if (songEntry == null) publisherInfoService.getPublisher(video, request.clientType)
                                else null
                            val description = video.description ?: extractDescription(nndClient.getEmbedInfo(video.id))
                            val resultingTagSet =
                                buildResultingTagSet(request.clientType, songEntry, correspondingVocaDbTags)

                            videoWithAssociatedEntryMapper.mapForTag(
                                video,
                                songEntry,
                                videoTagsWithStyle,
                                description,
                                resultingTagSet,
                                publisher,
                            )
                        }
                    }
                }
                .awaitAll()
                .toMutableList()

        sortResults(result, request.orderBy)

        return VideosByNndTagsResponseForTagging(
            result,
            videos.meta.totalCount,
            request.scope,
            calculateSongStats(result.mapNotNull { it.entry }),
            correspondingVocaDbTags,
        )
    }

    suspend fun getVideosByVocaDbTagMappings(request: VideosByVocaDbTagRequest): VideosByVocaDbTagResponse {
        return coroutineScope {
            val mappedTags = async { getClient(request.clientType).getAllVocaDbTagMappings(false) }
            val tag = async { getClient(request.clientType).getTagByName(request.tag) }.await()

            val tagMappings = mappedTags.await().filter { it.tag.id == tag.id }.map { it.sourceTag }.toSet()
            require(tagMappings.isNotEmpty()) { "Tag \"${request.tag}\" is not mapped" }

            val newRequest = requestMapper.map(request, tagMappings)

            val tempResult = getVideosByNndTags(newRequest, tag.id)

            VideosByVocaDbTagResponse(
                tempResult.items,
                tempResult.totalCount,
                tempResult.cleanScope,
                tempResult.songTypeStats,
                tag,
                tagMappings,
            )
        }
    }

    suspend fun getReleaseEventWithLinkedTags(request: GetReleaseEventRequest): ReleaseEventWithVocaDbTagsResponse {
        val event = getClient(request.clientType).getEventByName(request.eventName, Tags)
        val series =
            if (event.seriesId != null) getClient(request.clientType).getEventSeriesById(event.seriesId) else null
        return eventMapper.mapWithTags(event, series)
    }

    internal suspend fun likelyEarliestWork(clientType: ClientType, songEntry: VocaDbSongEntryWithTagsBase): Boolean {
        val effectiveCreators =
            songEntry.artists.filter {
                !it.isSupport &&
                    (it.artistEntryData?.artistType == Producer && it.effectiveRoles.contains(Default) ||
                        it.effectiveRoles.contains(Composer))
            }
        // trying to pinpoint a single person for whom this song is the first work
        // if cannot, remove the tag from suggestions
        return effectiveCreators.size == 1 &&
            songEntry.publishedAt != null &&
            !getClient(clientType)
                .artistHasSongsBeforeDate(
                    requireNotNull(effectiveCreators.first().artistEntryData).id,
                    requireNotNull(songEntry.publishedAt).atOffset(UTC).format(ISO_DATE_TIME),
                )
    }

    internal suspend fun buildResultingTagSet(
        clientType: ClientType,
        songEntry: VocaDbSongEntryWithTags?,
        correspondingVocaDbTags: Set<VocaDbTag>,
    ): List<VocaDbTagSelectable> {
        if (songEntry == null) return emptyList()

        val clientSpecificDbTagProps = getClientSpecificTagProps(clientType)
        val assignedTagIds = songEntry.tags.map { it.id }.toSet()
        if (
            correspondingVocaDbTags.any { it.id == clientSpecificDbTagProps.firstWork.id } &&
                !assignedTagIds.contains(clientSpecificDbTagProps.firstWork.id) &&
                !likelyEarliestWork(clientType, songEntry)
        ) {

            return correspondingVocaDbTags
                .filter { it.id != clientSpecificDbTagProps.firstWork.id }
                .map { VocaDbTagSelectable(it, assignedTagIds.contains(it.id)) }
        }

        return correspondingVocaDbTags.map { VocaDbTagSelectable(it, assignedTagIds.contains(it.id)) }
    }

    suspend fun getDataWithTagsByCustomQuery(request: QueryConsoleRequest): QueryConsoleResponse<out QueryConsoleData> {
        val response = getClient(request.clientType).getDataWithTagsByCustomQuery(request.apiType, request.query)
        return queryResponseMapper.map(response)
    }

    suspend fun getSongsWithPvsForTagging(request: SongsWithPvsRequest): SongsWithPvsResponse {
        val clientSpecificTagProps = getClientSpecificTagProps(request.clientType)
        val tagMappings =
            getClient(request.clientType).getAllVocaDbTagMappings(true).groupBy { kata2hiraAndLowercase(it.sourceTag) }
        val songEntries =
            dbClientHolder
                .getClient(request.clientType)
                .getSongs(
                    request.startOffset,
                    request.maxResults,
                    request.orderBy,
                    mapOf("pvServices" to NicoNicoDouga, "fields" to "PVs,Tags,Artists,ReleaseEvent"),
                )
        val pvs: MutableMap<String, GenericNndVideoData> =
            coroutineScope {
                    songEntries.items
                        .flatMap { it.pvs }
                        .filter { !it.disabled && it.service == NicoNicoDouga }
                        .map { it.id }
                        .map { async { it to nndClient.getEmbedInfo(it) } }
                }
                .awaitAll()
                .toMap()
                .toMutableMap()

        val regionBlocked =
            pvs.filter {
                    it.value is NndEmbedError &&
                        (it.value as NndEmbedError).errorParams.code ==
                            clientSpecificTagProps.regionBlocked.embedErrorCode
                }
                .keys
        val errorsToRecheck =
            coroutineScope {
                    pvs.filter { it.value is NndEmbedError }.map { async { it.key to nndClient.getThumbInfo(it.key) } }
                }
                .awaitAll()
                .toMap()
        errorsToRecheck.forEach { pvs[it.key] = it.value }

        val likelyFirstWorks =
            coroutineScope {
                    songEntries.items.map { item ->
                        async {
                            val firstWorkSuggested =
                                item.pvs
                                    .asSequence()
                                    .mapNotNull { pvs[it.id] }
                                    .filterIsInstance<GenericNndOkVideoData>()
                                    .flatMap { it.tags() }
                                    .mapNotNull { tagMappings[kata2hiraAndLowercase(it.name)] }
                                    .flatten()
                                    .any { it.tag.id == clientSpecificTagProps.firstWork.id }
                            if (!firstWorkSuggested || item.tags.any { it.id == clientSpecificTagProps.firstWork.id }) {
                                null
                            } else {
                                if (!likelyEarliestWork(request.clientType, item)) null else item.id
                            }
                        }
                    }
                }
                .awaitAll()
                .filterNotNull()

        return songWithPvsMapper.map(
            songEntries,
            pvs,
            tagMappings,
            likelyFirstWorks,
            regionBlocked,
            clientSpecificTagProps,
        )
    }

    suspend fun getRecentEvents(request: EventScheduleRequest): EventScheduleResponse {
        val endlessEventTagId = tagProps.getClientSpecificProps(request.clientType).endlessEvent.id
        val eventScope = request.eventScopeDays?.let(Duration::ofDays) ?: defaultEventScope
        val events =
            dbClientHolder.getClient(request.clientType).getReleaseEventSchedule(request.useCached, endlessEventTagId)
        val endlessEventIds = events.endlessEvents.map { it.id }.toSet()

        return events
            .toEventList()
            .distinctBy { it.id }
            .filterNot { it.date == null }
            .mapNotNull { eventMapper.mapForPreview(it, eventScope, offlineEvents, endlessEventIds.contains(it.id)) }
            .sortedWith(compareBy({ it.status.priority }, { it.date }))
            .let { EventScheduleResponse(it, eventScope.toDays()) }
    }

    private fun extractDescription(embed: NndEmbed) =
        when (embed) {
            is NndEmbedOk -> embed.description
            is NndEmbedError -> null
        }
}
