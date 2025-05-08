package niconicotagger.mapper

import java.net.URLDecoder
import java.nio.charset.Charset
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneOffset.UTC
import java.util.Objects.requireNonNullElse
import niconicotagger.dto.api.response.ReleaseEventPreviewResponse
import niconicotagger.dto.api.response.ReleaseEventWithVocaDbTagsResponse
import niconicotagger.dto.api.response.ReleaseEventWitnNndTagsResponse
import niconicotagger.dto.inner.misc.EventStatus
import niconicotagger.dto.inner.misc.EventStatus.ENDED
import niconicotagger.dto.inner.misc.EventStatus.ONGOING
import niconicotagger.dto.inner.misc.EventStatus.OUT_OF_RECENT_SCOPE
import niconicotagger.dto.inner.misc.EventStatus.UPCOMING
import niconicotagger.dto.inner.vocadb.VocaDbReleaseEvent
import niconicotagger.dto.inner.vocadb.VocaDbReleaseEventSeries
import org.mapstruct.Context
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingConstants.ComponentModel.SPRING

@Mapper(componentModel = SPRING, imports = [Utils::class])
abstract class ReleaseEventMapper {

    fun mapForPreview(event: VocaDbReleaseEvent): ReleaseEventPreviewResponse? {
        val eventStatus = getEventStatus(event)
        return if (eventStatus == OUT_OF_RECENT_SCOPE) null else mapEventPreview(event, eventStatus)
    }

    @Mapping(target = "category", expression = "java(Utils.mapCategory(event, event.getSeries()))")
    @Mapping(target = "status", expression = "java(status)")
    @Mapping(target = "pictureUrl", source = "event.mainPicture.urlOriginal")
    abstract fun mapEventPreview(event: VocaDbReleaseEvent, @Context status: EventStatus): ReleaseEventPreviewResponse

    internal fun getEventStatus(event: VocaDbReleaseEvent): EventStatus {
        val today = LocalDate.now().atStartOfDay().toInstant(UTC)
        if (!today.isBefore(event.date) && !today.isAfter(requireNonNullElse(event.endDate, event.date))) {
            return ONGOING
        }
        if (Duration.between(requireNonNullElse(event.endDate, event.date), today).toDays() in 0..14) {
            return ENDED
        }
        if (Duration.between(today, event.date).toDays() in 0..14) {
            return UPCOMING
        }
        return OUT_OF_RECENT_SCOPE
    }

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
