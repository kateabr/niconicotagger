package niconicotagger.client

import java.time.temporal.ChronoUnit.DAYS
import niconicotagger.constants.Constants.GENRE_FILTER
import niconicotagger.dto.api.request.VideosByNndEventTagsRequest
import niconicotagger.dto.api.request.VideosByNndTagsRequestBase
import niconicotagger.dto.inner.nnd.AndFilter
import niconicotagger.dto.inner.nnd.RangeFilter
import niconicotagger.dto.inner.nnd.SearchFilter

object Utils {
    fun <T : VideosByNndTagsRequestBase> createNndFilters(request: T): SearchFilter {
        return when (request) {
            is VideosByNndEventTagsRequest -> {
                if (!request.dates.applyToSearch) {
                    GENRE_FILTER
                } else {
                    val dateFilter =
                        if (request.dates.to == null) {
                            RangeFilter(
                                "startTime",
                                request.dates.from.minus(7, DAYS),
                                request.dates.from.plus(7, DAYS),
                                true,
                                true,
                            )
                        } else {
                            RangeFilter(
                                "startTime",
                                request.dates.from.minus(1, DAYS),
                                request.dates.to.plus(1, DAYS),
                                true,
                                true,
                            )
                        }
                    AndFilter(listOf(GENRE_FILTER, dateFilter))
                }
            }

            else -> GENRE_FILTER
        }
    }
}
