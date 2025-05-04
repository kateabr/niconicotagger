package niconicotagger.service

import com.github.benmanes.caffeine.cache.Caffeine
import com.sksamuel.aedile.core.asCache
import java.time.ZoneOffset.UTC
import java.time.format.DateTimeFormatter.ISO_DATE_TIME
import java.util.concurrent.TimeUnit.HOURS
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import niconicotagger.client.DbClientHolder
import niconicotagger.client.NicologClient
import niconicotagger.client.NndClient
import niconicotagger.configuration.PublisherLinkConfig
import niconicotagger.constants.Constants.FIRST_WORK_TAG_ID
import niconicotagger.dto.api.misc.ClientType
import niconicotagger.dto.api.misc.NndSortOrder
import niconicotagger.dto.api.misc.NndSortOrder.LIKE_COUNT
import niconicotagger.dto.api.misc.NndSortOrder.PUBLISH_TIME
import niconicotagger.dto.api.misc.NndSortOrder.VIEW_COUNT
import niconicotagger.dto.api.misc.NndTagType.NONE
import niconicotagger.dto.api.misc.NndVideoWithAssociatedVocaDbEntry
import niconicotagger.dto.api.misc.QueryConsoleData
import niconicotagger.dto.api.misc.SongEntryBase
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
import niconicotagger.dto.api.response.VideosByVocaDbTagResponse
import niconicotagger.dto.inner.misc.ArtistRole.Composer
import niconicotagger.dto.inner.misc.ArtistRole.Default
import niconicotagger.dto.inner.misc.ArtistType.Producer
import niconicotagger.dto.inner.misc.PvService.NicoNicoDouga
import niconicotagger.dto.inner.misc.TagTypeHolder
import niconicotagger.dto.inner.nnd.NndThumbnailOk
import niconicotagger.dto.inner.nnd.NndVideoData
import niconicotagger.dto.inner.vocadb.PublisherInfo
import niconicotagger.dto.inner.vocadb.PublisherType
import niconicotagger.dto.inner.vocadb.PublisherType.DATABASE
import niconicotagger.dto.inner.vocadb.PublisherType.NND_CHANNEL
import niconicotagger.dto.inner.vocadb.PublisherType.NND_USER
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
import niconicotagger.serde.Utils.normalizeToken
import org.springframework.stereotype.Service

@Service
class AggregatingService(
    private val dbClientHolder: DbClientHolder,
    private val nndClient: NndClient,
    private val nicologClient: NicologClient,
    private val eventMapper: ReleaseEventMapper,
    private val videoWithAssociatedEntryMapper: NndVideoWithAssociatedVocaDbEntryMapper,
    private val requestMapper: RequestMapper,
    private val queryResponseMapper: QueryResponseMapper,
    private val songWithPvsMapper: SongWithPvsMapper,
    private val publisherLinkConfig: PublisherLinkConfig,
) {
    private val publisherCache =
        Caffeine.newBuilder().expireAfterWrite(1, HOURS).maximumSize(100).asCache<String, PublisherInfo>()

    private fun getClient(clientType: ClientType) = dbClientHolder.getClient(clientType)

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
        result
    }

    suspend fun getReleaseEventByName(request: GetReleaseEventRequest): ReleaseEventWitnNndTagsResponse {
        val event = getClient(request.clientType).getEventByName(request.eventName, "WebLinks")
        val series =
            event.seriesId?.let { getClient(request.clientType).getEventSeriesById(event.seriesId, "WebLinks") }
        return eventMapper.mapWithLinks(event, series)
    }

    suspend fun getVideosByEventNndTags(request: VideosByNndEventTagsRequest): VideosByNndTagsResponseForEvent {
        val tagStyleHolder =
            TagTypeHolder()
                .storeRequestTags(request)
                .storeTagMappings(getClient(request.clientType).getAllVocaDbTagMappings(true))

        val videos = nndClient.getVideosByTags(request)
        val result =
            coroutineScope {
                    nndClient.getVideosByTags(request).data.map { video ->
                        async {
                            video.tags.forEach { tagStyleHolder.put(it, NONE) }
                            val videoTagsWithStyle = video.tags.associateWith { tagStyleHolder.get(it) }

                            val description = async { video.description ?: nndClient.getFormattedDescription(video.id) }
                            val songEntry = async {
                                getClient(request.clientType)
                                    .getSongByNndPv(video.id, "ReleaseEvent", VocaDbSongWithReleaseEvents::class.java)
                            }
                            val publisher = async {
                                if (songEntry.await() == null) getPublisher(video, request.clientType) else null
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

    suspend fun getVideosByNndTags(request: VideosByNndTagsRequest): VideosByNndTagsResponseForTagging {
        val tagStyleHolder = TagTypeHolder().storeRequestTags(request)

        val mappedTags = getClient(request.clientType).getAllVocaDbTagMappings(request.startOffset != 0L)
        val correspondingVocaDbTags = mappedTags.filter { request.tags.contains(it.sourceTag) }.map { it.tag }.toSet()
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
                                    .getSongByNndPv(video.id, "Tags,Artists", VocaDbSongEntryWithTags::class.java)
                            val publisher = if (songEntry == null) getPublisher(video, request.clientType) else null
                            val description = video.description ?: nndClient.getFormattedDescription(video.id)

                            videoWithAssociatedEntryMapper.mapForTag(
                                video,
                                songEntry,
                                videoTagsWithStyle,
                                description,
                                buildResultingTagSet(request.clientType, songEntry, correspondingVocaDbTags),
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

            val tagMappings =
                mappedTags
                    .await()
                    .filter { it.tag.id == tag.id }
                    .map { it.sourceTag }
                    .map { normalizeToken(it).trim() }
                    .toSet()
            require(tagMappings.isNotEmpty()) { "Tag \"${request.tag}\" is not mapped" }

            val newRequest = requestMapper.map(request, tagMappings)

            val tempResult = getVideosByNndTags(newRequest)

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
        val event = getClient(request.clientType).getEventByName(request.eventName, "Tags")
        val series =
            if (event.seriesId != null) getClient(request.clientType).getEventSeriesById(event.seriesId) else null
        return eventMapper.mapWithTags(event, series)
    }

    private fun createPublisher(id: Long, name: String?, publisherType: PublisherType, clientType: ClientType? = null) =
        PublisherInfo(
            publisherLinkConfig.getFullLink(id, publisherType, clientType),
            publisherLinkConfig.getLinkPath(id, publisherType, clientType),
            name,
            publisherType,
        )

    internal suspend fun getPublisher(video: NndVideoData, clientType: ClientType): PublisherInfo? {
        if (video.userId != null) {
            publisherCache
                .getOrNull("${video.userId}_${DATABASE}") {
                    getClient(clientType)
                        .getArtistByQuery(publisherLinkConfig.getLinkPath(video.userId, NND_USER))
                        ?.let { createPublisher(it.id, it.name, DATABASE, clientType) }
                }
                ?.let {
                    return it
                }
        }
        if (video.channelId != null) {
            publisherCache
                .getOrNull("ch${video.channelId}_${DATABASE}") {
                    getClient(clientType)
                        .findArtistDuplicate(publisherLinkConfig.getFullLink(video.channelId, NND_CHANNEL))
                        ?.let { createPublisher(it.id, it.name, DATABASE, clientType) }
                }
                ?.let {
                    return it
                }
        }
        nndClient
            .getThumbInfo(video.id)
            .let { thumbnail ->
                when (thumbnail) {
                    is NndThumbnailOk -> {
                        thumbnail.data.userId?.let { userId ->
                            publisherCache.getOrNull("${userId}_${NND_USER}") {
                                createPublisher(userId, thumbnail.data.publisherName, NND_USER)
                            }
                        }
                            ?: thumbnail.data.channelId?.let { channelId ->
                                publisherCache.getOrNull("ch${channelId}_${NND_USER}") {
                                    createPublisher(channelId, thumbnail.data.publisherName, NND_CHANNEL)
                                }
                            }
                    }

                    else -> null
                }
            }
            ?.let {
                return it
            }

        // if everything else fails, try to extract publisher's name from nicolog
        if (video.userId != null) {
            publisherCache
                .getOrNull("${video.userId}_${NND_USER}") {
                    nicologClient.getUserName(video.userId)?.let { createPublisher(video.userId, it, NND_USER) }
                }
                ?.let {
                    return it
                }
        }
        if (video.channelId != null) {
            publisherCache
                .getOrNull("ch${video.channelId}_${NND_CHANNEL}") {
                    nicologClient.getChannelName(video.channelId)?.let {
                        createPublisher(video.channelId, it, NND_CHANNEL)
                    }
                }
                ?.let {
                    return it
                }
        }

        // if that failed too
        if (video.userId != null) {
            return createPublisher(video.userId, null, NND_USER)
        }
        if (video.channelId != null) {
            return createPublisher(video.channelId, null, NND_CHANNEL)
        }

        // normally should not get here
        return null
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

    private suspend fun buildResultingTagSet(
        clientType: ClientType,
        songEntry: VocaDbSongEntryWithTags?,
        correspondingVocaDbTags: Set<VocaDbTag>,
    ): List<VocaDbTagSelectable> {
        if (songEntry == null) return emptyList()

        val assignedTagIds = songEntry.tags.map { it.id }.toSet()
        if (
            correspondingVocaDbTags.any { it.id == FIRST_WORK_TAG_ID } &&
                !assignedTagIds.contains(FIRST_WORK_TAG_ID) &&
                !likelyEarliestWork(clientType, songEntry)
        ) {

            return correspondingVocaDbTags
                .filter { it.id != FIRST_WORK_TAG_ID }
                .map { VocaDbTagSelectable(it, assignedTagIds.contains(it.id)) }
        }

        return correspondingVocaDbTags.map { VocaDbTagSelectable(it, assignedTagIds.contains(it.id)) }
    }

    suspend fun getDataWithTagsByCustomQuery(request: QueryConsoleRequest): QueryConsoleResponse<out QueryConsoleData> {
        val response = getClient(request.clientType).getDataWithTagsByCustomQuery(request.apiType, request.query)
        return queryResponseMapper.map(response)
    }

    suspend fun getSongsWithPvsForTagging(request: SongsWithPvsRequest): SongsWithPvsResponse {
        val tagMappings =
            getClient(request.clientType).getAllVocaDbTagMappings(true).groupBy { kata2hiraAndLowercase(it.sourceTag) }
        val songEntries =
            dbClientHolder
                .getClient(request.clientType)
                .getSongs(
                    request.startOffset,
                    request.maxResults,
                    request.orderBy,
                    mapOf("pvServices" to NicoNicoDouga, "fields" to "PVs,Tags,Artists"),
                )
        val pvs =
            coroutineScope {
                    songEntries.items
                        .flatMap { it.pvs }
                        .filter { !it.disabled && it.service == NicoNicoDouga }
                        .map { it.id }
                        .map { async { it to nndClient.getThumbInfo(it) } }
                }
                .awaitAll()
                .toMap()

        val likelyFirstWorks =
            coroutineScope {
                    songEntries.items.map { item ->
                        async {
                            val firstWorkSuggested =
                                item.pvs
                                    .asSequence()
                                    .mapNotNull { pvs[it.id] }
                                    .filterIsInstance<NndThumbnailOk>()
                                    .flatMap { it.data.tags }
                                    .map { it.name }
                                    .mapNotNull { tagMappings[kata2hiraAndLowercase(it)] }
                                    .flatten()
                                    .any { it.tag.id == FIRST_WORK_TAG_ID }
                            if (!firstWorkSuggested || item.tags.any { it.id == FIRST_WORK_TAG_ID }) {
                                null
                            } else {
                                if (!likelyEarliestWork(request.clientType, item)) null else item.id
                            }
                        }
                    }
                }
                .awaitAll()
                .filterNotNull()

        return songWithPvsMapper.map(songEntries, pvs, tagMappings, likelyFirstWorks)
    }
}
