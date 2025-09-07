package niconicotagger.client

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.json.JsonMapper
import com.sksamuel.aedile.core.Cache
import java.time.temporal.ChronoUnit.DAYS
import kotlinx.coroutines.reactive.awaitFirstOrNull
import niconicotagger.constants.Constants.GENRE_FILTER
import niconicotagger.dto.api.request.VideosByNndEventTagsRequest
import niconicotagger.dto.api.request.VideosByNndTagsRequestBase
import niconicotagger.dto.inner.nnd.AndFilter
import niconicotagger.dto.inner.nnd.RangeFilter
import niconicotagger.dto.inner.nnd.SearchFilter
import org.springframework.web.reactive.function.client.WebClient

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

    suspend inline fun <reified T> performLargeGet(response: WebClient.ResponseSpec, jsonMapper: JsonMapper) =
        response.bodyToFlux(ByteArray::class.java).reduce(ByteArray::plus).awaitFirstOrNull()?.let {
            jsonMapper.readValue(it, object : TypeReference<T>() {})
        }

    suspend fun <K, V> Cache<K, V>.useCachedOrForceUpdate(useCached: Boolean, key: K, compute: suspend (K) -> V): V {
        if (useCached) {
            val cachedValue = getIfPresent(key)
            if (cachedValue != null) return cachedValue
        }
        return compute.invoke(key).also { put(key, it) }
    }
}
