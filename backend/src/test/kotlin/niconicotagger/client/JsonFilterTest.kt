package niconicotagger.client

import niconicotagger.client.Utils.createNndFilters
import niconicotagger.constants.Constants.GENRE_FILTER
import niconicotagger.dto.api.misc.EventDateBounds
import niconicotagger.dto.api.request.VideosByNndEventTagsRequest
import niconicotagger.dto.api.request.VideosByNndTagsRequest
import niconicotagger.dto.api.request.VideosByNndTagsRequestBase
import niconicotagger.dto.inner.nnd.AndFilter
import niconicotagger.dto.inner.nnd.RangeFilter
import niconicotagger.dto.inner.nnd.SearchFilter
import org.assertj.core.api.Assertions.assertThat
import org.instancio.Instancio
import org.instancio.Select.field
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import java.time.temporal.ChronoUnit.DAYS
import java.util.stream.Stream

class JsonFilterTest {
    @ParameterizedTest
    @ArgumentsSource(TestData::class)
    fun `json filter creation test`(request: VideosByNndTagsRequestBase, expectedObject: SearchFilter) {
        assertThat(createNndFilters(request))
            .usingRecursiveComparison()
            .isEqualTo(expectedObject)
    }

    companion object {
        class TestData : ArgumentsProvider {
            override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
                return VideosByNndTagsRequestBase::class.sealedSubclasses.flatMap {
                    when (it) {
                        VideosByNndTagsRequest::class -> listOf(
                            argumentSet(
                                VideosByNndTagsRequest::class.simpleName,
                                Instancio.create(VideosByNndTagsRequest::class.java),
                                GENRE_FILTER
                            )
                        )

                        VideosByNndEventTagsRequest::class -> {
                            val dateRestrictedOneDay = Instancio.of(VideosByNndEventTagsRequest::class.java)
                                .set(field(EventDateBounds::class.java, "applyToSearch"), true)
                                .ignore(field(EventDateBounds::class.java, "to"))
                                .create()
                            val dateRestrictedSeveralDays = Instancio.of(VideosByNndEventTagsRequest::class.java)
                                .set(field(EventDateBounds::class.java, "applyToSearch"), true)
                                .create()
                            val noDateRestrictions = Instancio.of(VideosByNndEventTagsRequest::class.java)
                                .set(field(EventDateBounds::class.java, "applyToSearch"), false)
                                .create()
                            listOf(
                                argumentSet(
                                    VideosByNndEventTagsRequest::class.simpleName + ", date-restricted search, one-day event",
                                    dateRestrictedOneDay,
                                    AndFilter(
                                        listOf(
                                            GENRE_FILTER,
                                            RangeFilter(
                                                "startTime",
                                                dateRestrictedOneDay.dates.from.minus(
                                                    7,
                                                    DAYS
                                                ),
                                                dateRestrictedOneDay.dates.from.plus(7, DAYS),
                                                true,
                                                true
                                            )
                                        )
                                    )
                                ),
                                argumentSet(
                                    VideosByNndEventTagsRequest::class.simpleName + ", date-restricted search, several-day event",
                                    dateRestrictedSeveralDays,
                                    AndFilter(
                                        listOf(
                                            GENRE_FILTER,
                                            RangeFilter(
                                                "startTime",
                                                dateRestrictedSeveralDays.dates.from.minus(
                                                    1,
                                                    DAYS
                                                ),
                                                requireNotNull(dateRestrictedSeveralDays.dates.to).plus(1, DAYS),
                                                true,
                                                true
                                            )
                                        )
                                    )
                                ),
                                argumentSet(
                                    VideosByNndEventTagsRequest::class.simpleName + ", no date restrictions",
                                    noDateRestrictions,
                                    GENRE_FILTER
                                ),
                            )
                        }

                        else -> error("Unknown $it")
                    }
                }.stream()
            }

        }
    }
}
