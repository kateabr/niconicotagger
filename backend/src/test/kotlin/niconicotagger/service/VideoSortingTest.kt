package niconicotagger.service

import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import java.util.Comparator.comparing
import java.util.stream.Stream
import niconicotagger.client.DbClientHolder
import niconicotagger.client.NicologClient
import niconicotagger.client.NndClient
import niconicotagger.configuration.PublisherLinkConfig
import niconicotagger.dto.api.misc.NndSortOrder
import niconicotagger.dto.api.misc.NndSortOrder.LIKE_COUNT
import niconicotagger.dto.api.misc.NndSortOrder.PUBLISH_TIME
import niconicotagger.dto.api.misc.NndSortOrder.VIEW_COUNT
import niconicotagger.dto.api.misc.NndVideoWithAssociatedVocaDbEntry
import niconicotagger.dto.api.misc.NndVideoWithAssociatedVocaDbEntryForEvent
import niconicotagger.dto.api.misc.SongEntryBase
import niconicotagger.dto.inner.misc.ReleaseEventCategory
import niconicotagger.mapper.NndVideoWithAssociatedVocaDbEntryMapper
import niconicotagger.mapper.QueryResponseMapper
import niconicotagger.mapper.ReleaseEventMapper
import niconicotagger.mapper.RequestMapper
import niconicotagger.mapper.SongWithPvsMapper
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.InstanceOfAssertFactories.LIST
import org.instancio.Instancio
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import java.time.Duration

class VideoSortingTest: AggregatingServiceTest() {

    @ParameterizedTest
    @ArgumentsSource(SortingTestData::class)
    fun `results sorting test`(
        results: MutableList<NndVideoWithAssociatedVocaDbEntry<SongEntryBase>>,
        sortOrder: NndSortOrder,
        comparator: Comparator<in Any>,
    ) {
        aggregatingService.sortResults(results, sortOrder)

        assertThat(results).asInstanceOf(LIST).isSortedAccordingTo(comparator)
    }

    companion object {
        class SortingTestData : ArgumentsProvider {
            override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
                return NndSortOrder.entries
                    .map {
                        when (it) {
                            VIEW_COUNT ->
                                argumentSet(
                                    "sort by view count (most viewed first)",
                                    Instancio.createList(NndVideoWithAssociatedVocaDbEntryForEvent::class.java),
                                    it,
                                    comparing { item1: NndVideoWithAssociatedVocaDbEntryForEvent ->
                                            item1.video.viewCounter
                                        }
                                        .reversed(),
                                )

                            PUBLISH_TIME ->
                                argumentSet(
                                    "sort by publish time (newest first)",
                                    Instancio.createList(NndVideoWithAssociatedVocaDbEntryForEvent::class.java),
                                    it,
                                    comparing { item1: NndVideoWithAssociatedVocaDbEntryForEvent ->
                                            item1.video.publishedAt
                                        }
                                        .reversed(),
                                )

                            LIKE_COUNT ->
                                argumentSet(
                                    "sort by view count (most liked first)",
                                    Instancio.createList(NndVideoWithAssociatedVocaDbEntryForEvent::class.java),
                                    it,
                                    comparing { item1: NndVideoWithAssociatedVocaDbEntryForEvent ->
                                            item1.video.likeCounter
                                        }
                                        .reversed(),
                                )
                        }
                    }
                    .stream()
            }
        }
    }
}
