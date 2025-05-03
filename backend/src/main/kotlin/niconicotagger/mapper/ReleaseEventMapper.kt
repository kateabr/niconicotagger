package niconicotagger.mapper

import java.net.URLDecoder
import niconicotagger.dto.api.response.ReleaseEventWithVocaDbTagsResponse
import niconicotagger.dto.api.response.ReleaseEventWitnNndTagsResponse
import niconicotagger.dto.inner.misc.ReleaseEventCategory
import niconicotagger.dto.inner.misc.ReleaseEventCategory.Unspecified
import niconicotagger.dto.inner.vocadb.VocaDbReleaseEvent
import niconicotagger.dto.inner.vocadb.VocaDbReleaseEventSeries
import org.mapstruct.Context
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingConstants.ComponentModel.SPRING

@Mapper(componentModel = SPRING)
abstract class ReleaseEventMapper {

    @Mapping(target = "nndTags", expression = "java(mapNndTags(event, series))")
    @Mapping(target = "category", expression = "java(mapCategory(event, series))")
    @Mapping(target = "suggestFiltering", expression = "java(mapFilteringSuggestionFlag(series))")
    abstract fun mapWithLinks(
        event: VocaDbReleaseEvent,
        @Context series: VocaDbReleaseEventSeries?,
    ): ReleaseEventWitnNndTagsResponse

    @Mapping(target = "category", expression = "java(mapCategory(event, series))")
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
            .map { URLDecoder.decode(it, "UTF-8") }

    protected fun mapCategory(event: VocaDbReleaseEvent, series: VocaDbReleaseEventSeries?): ReleaseEventCategory =
        if (event.category == Unspecified && series != null) series.category else event.category

    protected fun mapFilteringSuggestionFlag(eventSeries: VocaDbReleaseEventSeries?): Boolean =
        (eventSeries?.webLinks?.mapNotNull { nndTagRegex.find(it.url)?.groupValues }?.size ?: 0) > 0

    companion object {
        private val nndTagRegex = Regex("(?:https?://)?(?:www.)?nicovideo.jp/tag/([^?]+)(?:\\?.*)?")
    }
}
