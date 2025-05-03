package niconicotagger.mapper

import java.util.stream.Stream
import niconicotagger.dto.api.response.ReleaseEventWithVocaDbTagsResponse
import niconicotagger.dto.api.response.ReleaseEventWitnNndTagsResponse
import niconicotagger.dto.inner.misc.ReleaseEventCategory
import niconicotagger.dto.inner.misc.ReleaseEventCategory.Unspecified
import niconicotagger.dto.inner.misc.WebLink
import niconicotagger.dto.inner.vocadb.VocaDbReleaseEvent
import niconicotagger.dto.inner.vocadb.VocaDbReleaseEventSeries
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.InstanceOfAssertFactories.list
import org.instancio.Instancio
import org.instancio.Select.field
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

@ExtendWith(InstancioExtension::class)
class ReleaseEventWitnNndTagsResponseMapperTest {
    private val mapper: ReleaseEventMapper = Mappers.getMapper(ReleaseEventMapper::class.java)

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
                            listOf(WebLink("https://www.nicovideo.jp/tag/general_series_tag"), WebLink("https://x.com")),
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
                            listOf(WebLink("https://www.nicovideo.jp/tag/general_series_tag"), WebLink("https://x.com")),
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
    }
}
