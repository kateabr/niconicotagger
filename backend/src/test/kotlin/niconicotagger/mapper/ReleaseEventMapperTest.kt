package niconicotagger.mapper

import niconicotagger.dto.api.response.ReleaseEventPreviewResponse
import niconicotagger.dto.api.response.ReleaseEventWithVocaDbTagsResponse
import niconicotagger.dto.api.response.ReleaseEventWitnNndTagsResponse
import niconicotagger.dto.inner.misc.EventStatus.ENDED
import niconicotagger.dto.inner.misc.EventStatus.ONGOING
import niconicotagger.dto.inner.misc.EventStatus.UPCOMING
import niconicotagger.dto.inner.misc.ReleaseEventCategory
import niconicotagger.dto.inner.misc.ReleaseEventCategory.AlbumRelease
import niconicotagger.dto.inner.misc.ReleaseEventCategory.Club
import niconicotagger.dto.inner.misc.ReleaseEventCategory.Concert
import niconicotagger.dto.inner.misc.ReleaseEventCategory.Convention
import niconicotagger.dto.inner.misc.ReleaseEventCategory.Other
import niconicotagger.dto.inner.misc.ReleaseEventCategory.Unspecified
import niconicotagger.dto.inner.misc.WebLink
import niconicotagger.dto.inner.vocadb.VocaDbReleaseEvent
import niconicotagger.dto.inner.vocadb.VocaDbReleaseEventSeries
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.InstanceOfAssertFactories.list
import org.assertj.core.api.InstanceOfAssertFactories.type
import org.instancio.Assign.valueOf
import org.instancio.Instancio
import org.instancio.Select.all
import org.instancio.Select.field
import org.instancio.Select.types
import org.instancio.TypeToken
import org.instancio.junit.InstancioExtension
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.ArgumentSet
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import org.junit.jupiter.params.provider.FieldSource
import org.mapstruct.factory.Mappers
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset.UTC
import java.time.temporal.ChronoUnit.DAYS
import java.util.function.Function
import java.util.function.Supplier
import java.util.stream.Stream
import kotlin.math.absoluteValue

@ExtendWith(InstancioExtension::class)
class ReleaseEventMapperTest {
    private val mapper: ReleaseEventMapper = Mappers.getMapper(ReleaseEventMapper::class.java)

    @ParameterizedTest
    @ArgumentsSource(EventPreviewTestData::class)
    fun `map event previews test`(
        event: VocaDbReleaseEvent,
        eventScope: Duration,
        offlineEvents: Set<ReleaseEventCategory>,
        expectedObject: ReleaseEventPreviewResponse?
    ) {
        assertThat(mapper.mapForPreview(event, eventScope, offlineEvents))
            .usingRecursiveComparison()
            .ignoringFields("category") // checked separately
            .isEqualTo(expectedObject)
    }

    @ParameterizedTest
    @ArgumentsSource(EventPreviewCategoryTestData::class)
    fun `map event previews (event category) test`(
        event: VocaDbReleaseEvent,
        expectedCategory: ReleaseEventCategory
    ) {
        assertThat(mapper.mapForPreview(event, Duration.ofDays(14), emptySet()))
            .usingRecursiveComparison()
            .asInstanceOf(type(ReleaseEventPreviewResponse::class.java))
            .extracting { it.category }
            .isEqualTo(expectedCategory)
    }

    @ParameterizedTest
    @FieldSource("tagExtractionTestData")
    fun `NND tag extraction test`(link: WebLink, expectedTags: List<String>) {
        assertThat(
            mapper.mapWithLinks(
                Instancio.of(VocaDbReleaseEvent::class.java).set(field("webLinks"), listOf(link)).create(),
                null,
            )
        )
            .extracting { it.nndTags }
            .asInstanceOf(list(String::class.java))
            .containsExactlyInAnyOrderElementsOf(expectedTags)
    }

    @ParameterizedTest
    @FieldSource("linkAggregationTestData")
    fun `link aggregation test`(
        event: VocaDbReleaseEvent,
        series: VocaDbReleaseEventSeries?,
        expectedTags: List<String>,
    ) {
        assertThat(mapper.mapWithLinks(event, series))
            .extracting { it.nndTags }
            .asInstanceOf(list(String::class.java))
            .containsExactlyInAnyOrderElementsOf(expectedTags)
    }

    @ParameterizedTest
    @ArgumentsSource(ReleaseEventTestData::class)
    fun `map ReleaseEventWithVocaDbTagsResponse test`(
        event: VocaDbReleaseEvent,
        series: VocaDbReleaseEventSeries?,
        expectedCategory: ReleaseEventCategory,
    ) {
        assertThat(mapper.mapWithTags(event, series))
            .usingRecursiveComparison()
            .isEqualTo(
                ReleaseEventWithVocaDbTagsResponse(
                    event.id,
                    event.date,
                    event.endDate,
                    event.name,
                    expectedCategory,
                    event.tags,
                )
            )
    }

    @ParameterizedTest
    @ArgumentsSource(ReleaseEventTestData::class)
    fun `map ReleaseEventWitnNndTagsResponse test`(
        event: VocaDbReleaseEvent,
        series: VocaDbReleaseEventSeries?,
        expectedCategory: ReleaseEventCategory,
        expectedFilteringFlag: Boolean,
    ) {
        assertThat(mapper.mapWithLinks(event, series))
            .usingRecursiveComparison()
            .ignoringFields("nndTags") // checked in `NND tag extraction test`
            .isEqualTo(
                ReleaseEventWitnNndTagsResponse(
                    event.id,
                    event.date,
                    event.endDate,
                    event.name,
                    expectedCategory,
                    expectedFilteringFlag,
                    emptyList(), // checked in `NND tag extraction test`
                    event.seriesId,
                )
            )
    }

    companion object {
        val tagExtractionTestData =
            listOf(
                argumentSet(
                    "no schema, host: nicovideo.jp",
                    WebLink("nicovideo.jp/tag/%E9%87%8D%E9%9F%B3%E3%83%86%E3%83%88%E8%AA%95%E7%94%9F%E7%A5%AD"),
                    listOf("重音テト誕生祭"),
                ),
                argumentSet(
                    "no schema, host: www.nicovideo.jp",
                    WebLink("www.nicovideo.jp/tag/%E9%87%8D%E9%9F%B3%E3%83%86%E3%83%88%E8%AA%95%E7%94%9F%E7%A5%AD"),
                    listOf("重音テト誕生祭"),
                ),
                argumentSet(
                    "schema: http",
                    WebLink(
                        "http://www.nicovideo.jp/tag/%E9%87%8D%E9%9F%B3%E3%83%86%E3%83%88%E8%AA%95%E7%94%9F%E7%A5%AD"
                    ),
                    listOf("重音テト誕生祭"),
                ),
                argumentSet(
                    "schema: https",
                    WebLink(
                        "https://www.nicovideo.jp/tag/%E9%87%8D%E9%9F%B3%E3%83%86%E3%83%88%E8%AA%95%E7%94%9F%E7%A5%AD"
                    ),
                    listOf("重音テト誕生祭"),
                ),
                argumentSet(
                    "random params in the end",
                    WebLink(
                        "https://www.nicovideo.jp/tag/%E9%87%8D%E9%9F%B3%E3%83%86%E3%83%88%E8%AA%95%E7%94%9F%E7%A5%AD?rf=nvpc&rp=watch&ra=content_tree&rd=content_tree_children"
                    ),
                    listOf("重音テト誕生祭"),
                ),
                argumentSet(
                    "name starts with a hashtag (#)",
                    WebLink("https://www.nicovideo.jp/tag/%23some_event"),
                    listOf("#some_event"),
                ),
                argumentSet("not a tag link", WebLink("https://www.nicovideo.jp/watch/sm12345"), emptyList<String>()),
                argumentSet("not an NND link", WebLink("https://piapro.jp"), emptyList<String>()),
            )

        val linkAggregationTestData =
            listOf(
                argumentSet(
                    "suitable links in both event and series",
                    Instancio.of(VocaDbReleaseEvent::class.java)
                        .set(
                            field("webLinks"),
                            listOf(
                                WebLink("https://www.nicovideo.jp/tag/event_specific_tag"),
                                WebLink("https://piapro.jp"),
                            ),
                        )
                        .create(),
                    Instancio.of(VocaDbReleaseEventSeries::class.java)
                        .set(
                            field("webLinks"),
                            listOf(
                                WebLink("https://www.nicovideo.jp/tag/general_series_tag"),
                                WebLink("https://x.com")
                            ),
                        )
                        .create(),
                    listOf("event_specific_tag", "general_series_tag"),
                ),
                argumentSet(
                    "suitable links in event only",
                    Instancio.of(VocaDbReleaseEvent::class.java)
                        .set(
                            field("webLinks"),
                            listOf(
                                WebLink("https://www.nicovideo.jp/tag/event_specific_tag"),
                                WebLink("https://piapro.jp"),
                            ),
                        )
                        .create(),
                    Instancio.of(VocaDbReleaseEventSeries::class.java)
                        .set(field("webLinks"), listOf(WebLink("https://x.com")))
                        .create(),
                    listOf("event_specific_tag"),
                ),
                argumentSet(
                    "suitable links in event only, standalone event (no series)",
                    Instancio.of(VocaDbReleaseEvent::class.java)
                        .set(
                            field("webLinks"),
                            listOf(
                                WebLink("https://www.nicovideo.jp/tag/event_specific_tag"),
                                WebLink("https://piapro.jp"),
                            ),
                        )
                        .create(),
                    null,
                    listOf("event_specific_tag"),
                ),
                argumentSet(
                    "suitable links in series only",
                    Instancio.of(VocaDbReleaseEvent::class.java)
                        .set(field("webLinks"), listOf(WebLink("https://piapro.jp")))
                        .create(),
                    Instancio.of(VocaDbReleaseEventSeries::class.java)
                        .set(
                            field("webLinks"),
                            listOf(
                                WebLink("https://www.nicovideo.jp/tag/general_series_tag"),
                                WebLink("https://x.com")
                            ),
                        )
                        .create(),
                    listOf("general_series_tag"),
                ),
                argumentSet(
                    "no suitable links at all",
                    Instancio.of(VocaDbReleaseEvent::class.java)
                        .set(field("webLinks"), listOf(WebLink("https://piapro.jp")))
                        .create(),
                    Instancio.of(VocaDbReleaseEventSeries::class.java)
                        .set(field("webLinks"), listOf(WebLink("https://x.com")))
                        .create(),
                    listOf<String>(),
                ),
                argumentSet(
                    "no suitable links, standalone event (no series)",
                    Instancio.of(VocaDbReleaseEvent::class.java)
                        .set(field("webLinks"), listOf(WebLink("https://piapro.jp")))
                        .create(),
                    null,
                    listOf<String>(),
                ),
            )

        class ReleaseEventTestData : ArgumentsProvider {
            private fun inheritFromSeries(): ArgumentSet {
                val series =
                    Instancio.of(VocaDbReleaseEventSeries::class.java)
                        .set(field("webLinks"), listOf(WebLink("https://www.nicovideo.jp/tag/tag")))
                        .create()
                return argumentSet(
                    "event category is \"Unspecified\" => inherit from series; series has NND tags => suggest filtering",
                    Instancio.of(VocaDbReleaseEvent::class.java).set(field("category"), Unspecified).create(),
                    series,
                    series.category,
                    true,
                )
            }

            private fun doNotInheritFromSeries(): ArgumentSet {
                val event =
                    Instancio.of(VocaDbReleaseEvent::class.java)
                        .generate(field("category")) { gen ->
                            gen.enumOf(ReleaseEventCategory::class.java).excluding(Unspecified)
                        }
                        .setBlank(field("webLinks"))
                        .create()
                return argumentSet(
                    "category in event is not \"Unspecified\" => keep the event category; no NND tags in series => do not suggest filtering",
                    event,
                    Instancio.create(VocaDbReleaseEventSeries::class.java),
                    event.category,
                    false,
                )
            }

            private fun standaloneEvent(): ArgumentSet {
                val event = Instancio.create(VocaDbReleaseEvent::class.java)
                return argumentSet(
                    "not a series event => keep the event category; do not suggest filtering",
                    event,
                    null,
                    event.category,
                    false,
                )
            }

            override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
                return Stream.of(inheritFromSeries(), doNotInheritFromSeries(), standaloneEvent())
            }
        }

        class EventPreviewTestData : ArgumentsProvider {
            private val offlineEvents = setOf(AlbumRelease, Club, Concert, Convention, Other)
            private val eventScopeSpecs = Instancio.gen().longs().range(2, 14)

            private fun createEvent(isOffline: Boolean, startDate: LocalDate, endDate: LocalDate?): VocaDbReleaseEvent {
                return Instancio.of(VocaDbReleaseEvent::class.java)
                    .generate(types().of(ReleaseEventCategory::class.java)) { gen ->
                        if (isOffline) gen.oneOf(offlineEvents)
                        else gen.enumOf(ReleaseEventCategory::class.java).excluding(*offlineEvents.toTypedArray())
                    }
                    .set(field("date"), startDate.atStartOfDay().toInstant(UTC))
                    .set(field("endDate"), endDate?.atStartOfDay()?.toInstant(UTC))
                    .create()
            }

            private fun outOfRecentScope(offByDays: Long, hasEndDate: Boolean): ArgumentSet {
                val eventScope = eventScopeSpecs.get().let(Duration::ofDays)
                val startDate = if (offByDays > 0)
                    LocalDate.now().plusDays(eventScope.toDays()).plusDays(offByDays)
                else
                    if (!hasEndDate)
                        LocalDate.now().minusDays(eventScope.toDays()).minusDays(-offByDays)
                    else
                        LocalDate.now().minusDays(eventScope.toDays()).minusDays(-offByDays * 2)
                val endDate = if (!hasEndDate) null else startDate.plusDays(offByDays)

                return argumentSet(
                    "event out of recent scope (${offByDays.absoluteValue} days ${if (offByDays > 0) "late" else "early"}, ${if (hasEndDate) "several-day" else "one-day"} event)",
                    createEvent(Instancio.gen().booleans().get(), startDate, endDate),
                    eventScope,
                    offlineEvents,
                    null
                )
            }

            private fun recentlyEnded(hasEndDate: Boolean, isOffline: Boolean): ArgumentSet {
                val eventScope = eventScopeSpecs.get().let(Duration::ofDays)
                val startDate = LocalDate.now().minusDays(eventScope.toDays() - 1)
                val endDate = if (!hasEndDate) null else startDate.plusDays(1)
                val event = createEvent(isOffline, startDate, endDate)

                return argumentSet(
                    "event recently ended (${if (hasEndDate) "several-day" else "one-day"} event, ${if (isOffline) "offline" else "online"})",
                    event,
                    eventScope,
                    offlineEvents,
                    ReleaseEventPreviewResponse(
                        event.id,
                        event.date,
                        event.endDate,
                        event.name,
                        Unspecified,
                        ENDED,
                        event.mainPicture?.urlOriginal,
                        isOffline
                    )
                )
            }

            private fun ongoing(hasEndDate: Boolean, isOffline: Boolean): ArgumentSet {
                val eventScope = eventScopeSpecs.get().let(Duration::ofDays)
                val startDate = LocalDate.now()
                val endDate = if (!hasEndDate) null else startDate.plusDays(1)
                val event = createEvent(isOffline, startDate, endDate)

                return argumentSet(
                    "event currently happening (${if (hasEndDate) "several-day" else "one-day"} event, ${if (isOffline) "offline" else "online"})",
                    event,
                    eventScope,
                    offlineEvents,
                    ReleaseEventPreviewResponse(
                        event.id,
                        event.date,
                        event.endDate,
                        event.name,
                        Unspecified,
                        ONGOING,
                        event.mainPicture?.urlOriginal,
                        isOffline
                    )
                )
            }

            private fun upcoming(hasEndDate: Boolean, isOffline: Boolean): ArgumentSet {
                val eventScope = eventScopeSpecs.get().let(Duration::ofDays)
                val startDate = LocalDate.now().plusDays(1)
                val endDate = if (!hasEndDate) null else startDate.plusDays(eventScope.toDays())
                val event = createEvent(isOffline, startDate, endDate)

                return argumentSet(
                    "upcoming event (${if (hasEndDate) "several-day" else "one-day"} event, ${if (isOffline) "offline" else "online"})",
                    event,
                    eventScope,
                    offlineEvents,
                    ReleaseEventPreviewResponse(
                        event.id,
                        event.date,
                        event.endDate,
                        event.name,
                        Unspecified,
                        UPCOMING,
                        event.mainPicture?.urlOriginal,
                        isOffline
                    )
                )
            }

            override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
                return listOf(true, false).flatMap { hasEndDate ->
                    listOf(-2L, 2L).map { offByDays ->
                        outOfRecentScope(offByDays, hasEndDate)
                    } + listOf(true, false).flatMap { isOffline ->
                        listOf(
                            recentlyEnded(hasEndDate, isOffline),
                            ongoing(hasEndDate, isOffline),
                            upcoming(hasEndDate, isOffline)
                        )
                    }
                }
                    .stream()
            }

        }

        class EventPreviewCategoryTestData : ArgumentsProvider {
            private fun createEvent(hasSeries: Boolean, hasEndDate: Boolean, hasCategory: Boolean): ArgumentSet {
                var eventSpec = Instancio.of(VocaDbReleaseEvent::class.java)
                    .lenient()
                    .generate(field("category")) { gen ->
                        if (!hasCategory) gen.oneOf(Unspecified) else gen.enumOf(
                            ReleaseEventCategory::class.java
                        ).excluding(Unspecified)
                    }
                    .assign(valueOf(field("date")).supply(Supplier { Instant.now() }))
                    .assign(
                        valueOf(field("date")).to(field("endDate"))
                            .`as`<Instant, Instant?>(Function { it.plus(1, DAYS) }).`when`<Instant> { hasEndDate })
                if (!hasSeries) eventSpec = eventSpec.ignore(all(field("seriesId"), field("series")))
                if (!hasEndDate) eventSpec = eventSpec.ignore(field("endDate"))
                val event = eventSpec.create()
                val expectedCategory =
                    if (event.category == Unspecified && event.seriesId != null) event?.series?.category else event.category

                return argumentSet(
                    "${if (hasEndDate) "several-day" else "one-day"} ${if (hasSeries) "series" else "standalone"} event of category ${event.category} => $expectedCategory",
                    event,
                    expectedCategory
                )
            }

            override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
                return Instancio.ofCartesianProduct(object : TypeToken<Triple<Boolean, Boolean, Boolean>> {})
                    .with(field(Triple::class.java, "first"), true, false)
                    .with(field(Triple::class.java, "second"), true, false)
                    .with(field(Triple::class.java, "third"), true, false)
                    .create()
                    .map { createEvent(it.first, it.second, it.third) }
                    .stream()
            }

        }
    }
}
