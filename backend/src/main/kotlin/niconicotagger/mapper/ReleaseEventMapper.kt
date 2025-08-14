package niconicotagger.mapper

import java.net.URLDecoder
import java.nio.charset.Charset
import java.time.Clock
import java.time.Duration
import niconicotagger.dto.api.response.ReleaseEventPreview
import niconicotagger.dto.api.response.ReleaseEventWithVocaDbTagsResponse
import niconicotagger.dto.api.response.ReleaseEventWitnNndTagsResponse
import niconicotagger.dto.inner.misc.EventStatus
import niconicotagger.dto.inner.misc.EventStatus.OUT_OF_RECENT_SCOPE
import niconicotagger.dto.inner.misc.ReleaseEventCategory
import niconicotagger.dto.inner.vocadb.VocaDbReleaseEvent
import niconicotagger.dto.inner.vocadb.VocaDbReleaseEventSeries
import org.mapstruct.Context
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingConstants.ComponentModel.SPRING

@Mapper(componentModel = SPRING, imports = [Utils::class])
abstract class ReleaseEventMapper {

    internal var clock: Clock = Clock.systemUTC()

    fun mapForPreview(
        event: VocaDbReleaseEvent,
        eventScope: Duration,
        offlineEvents: Set<ReleaseEventCategory>,
    ): ReleaseEventPreview? {
        val eventStatus = Utils.getEventStatus(event, eventScope, clock)
        return if (eventStatus == OUT_OF_RECENT_SCOPE) null else mapEventPreview(event, eventStatus, offlineEvents)
    }

    @Mapping(target = "category", expression = "java(Utils.mapCategory(event, event.getSeries()))")
    @Mapping(target = "isOffline", expression = "java(offlineEvents.contains(category))", dependsOn = ["category"])
    @Mapping(target = "status", expression = "java(status)")
    @Mapping(target = "pictureUrl", source = "event.mainPicture.urlOriginal")
    abstract fun mapEventPreview(
        event: VocaDbReleaseEvent,
        @Context status: EventStatus,
        @Context offlineEvents: Set<ReleaseEventCategory>,
    ): ReleaseEventPreview

    @Mapping(target = "nndTags", expression = "java(mapNndTags(event, series))")
    @Mapping(target = "category", expression = "java(Utils.mapCategory(event, series))")
    @Mapping(target = "suggestFiltering", expression = "java(mapFilteringSuggestionFlag(series))")
    abstract fun mapWithLinks(
        event: VocaDbReleaseEvent,
        @Context series: VocaDbReleaseEventSeries?,
    ): ReleaseEventWitnNndTagsResponse

    @Mapping(target = "category", expression = "java(Utils.mapCategory(event, series))")
    @Mapping(target = "vocaDbTags", source = "tags")
    abstract fun mapWithTags(
        event: VocaDbReleaseEvent,
        @Context series: VocaDbReleaseEventSeries?,
    ): ReleaseEventWithVocaDbTagsResponse

    protected fun mapNndTags(event: VocaDbReleaseEvent, series: VocaDbReleaseEventSeries?): List<String> =
        (event.webLinks + (series?.webLinks ?: emptyList()))
            .map { it.url }
            .mapNotNull { nndTagRegex.find(it) }
            .map { it.groupValues[1] }
            .map { URLDecoder.decode(it, Charset.forName("UTF-8")) }

    protected fun mapFilteringSuggestionFlag(eventSeries: VocaDbReleaseEventSeries?): Boolean =
        (eventSeries?.webLinks?.mapNotNull { nndTagRegex.find(it.url)?.groupValues }?.size ?: 0) > 0

    companion object {
        private val nndTagRegex = Regex("(?:https?://)?(?:www.)?nicovideo.jp/tag/([^?]+)(?:\\?.*)?")
    }
}
