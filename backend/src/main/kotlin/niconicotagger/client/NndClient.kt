package niconicotagger.client

import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.github.benmanes.caffeine.cache.Caffeine
import com.sksamuel.aedile.core.asCache
import io.netty.handler.logging.LogLevel.DEBUG
import java.util.concurrent.TimeUnit.HOURS
import kotlinx.coroutines.reactor.awaitSingle
import niconicotagger.client.Utils.createNndFilters
import niconicotagger.client.Utils.performLargeGet
import niconicotagger.constants.Constants.API_SEARCH_FIELDS
import niconicotagger.constants.Constants.DEFAULT_USER_AGENT
import niconicotagger.dto.api.request.VideosByNndTagsRequestBase
import niconicotagger.dto.inner.nnd.NndApiSearchResult
import niconicotagger.dto.inner.nnd.NndEmbed
import niconicotagger.dto.inner.nnd.NndThumbnail
import org.jsoup.Jsoup
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.HttpHeaders.USER_AGENT
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.MediaType.APPLICATION_XML_VALUE
import org.springframework.http.MediaType.TEXT_HTML_VALUE
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.http.codec.xml.Jaxb2XmlDecoder
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.util.MimeTypeUtils.APPLICATION_XML
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.util.UriComponentsBuilder
import reactor.netty.http.client.HttpClient
import reactor.netty.transport.logging.AdvancedByteBufFormat.TEXTUAL

/** Api docs: https://site.nicovideo.jp/search-api-docs/snapshot */
class NndClient(
    private val channelBaseHost: String,
    private val thumbBaseUrl: String,
    private val embedBaseUrl: String,
    private val apiBaseUrl: String,
    private val jsonMapper: JsonMapper,
    private val xmlMapper: XmlMapper,
    webClientBuilder: WebClient.Builder,
) {
    private val client: WebClient =
        webClientBuilder
            .defaultHeader(USER_AGENT, DEFAULT_USER_AGENT)
            .clientConnector(
                ReactorClientHttpConnector(
                    HttpClient.create()
                        .wiretap(this::class.java.getCanonicalName(), DEBUG, TEXTUAL)
                        .followRedirect(true)
                )
            )
            .exchangeStrategies(
                ExchangeStrategies.builder()
                    .codecs { configurer -> configurer.defaultCodecs().jaxb2Decoder(Jaxb2XmlDecoder(APPLICATION_XML)) }
                    .build()
            )
            .build()
    private val thumbCache =
        Caffeine.newBuilder().expireAfterAccess(1, HOURS).maximumSize(10_000).asCache<String, NndThumbnail>()
    private val embedCache =
        Caffeine.newBuilder().expireAfterAccess(1, HOURS).maximumSize(10_000).asCache<String, NndEmbed>()
    private val videosByTagsCache =
        Caffeine.newBuilder().expireAfterAccess(1, HOURS).maximumSize(1_000).asCache<Int, NndApiSearchResult>()

    suspend fun getThumbInfo(id: String): NndThumbnail {
        return thumbCache.get(id) {
            xmlMapper.readValue(
                client
                    .get()
                    .uri("$thumbBaseUrl/api/getthumbinfo/{id}", id)
                    .header(CONTENT_TYPE, APPLICATION_XML_VALUE)
                    .retrieve()
                    .awaitBody<ByteArray>(),
                NndThumbnail::class.java,
            ) ?: error("Failed to retrieve thumbnail for id $id")
        }
    }

    suspend fun getEmbedInfo(id: String): NndEmbed {
        val cachedValue = embedCache.getIfPresent(id)
        if (cachedValue != null) return cachedValue

        val html =
            client
                .get()
                .uri("$embedBaseUrl/watch/{id}", id)
                .header(CONTENT_TYPE, TEXT_HTML_VALUE)
                .exchangeToMono { it.bodyToMono(String::class.java) }
                .awaitSingle()

        val dataProps = Jsoup.parse(html).body().getElementById("ext-player")?.attr("data-props")
        if (dataProps.isNullOrEmpty()) error("Failed to extract embed data for $id")

        val parsedData = jsonMapper.readValue(dataProps, NndEmbed::class.java)
        embedCache.put(id, parsedData)

        return parsedData
    }

    suspend fun <T : VideosByNndTagsRequestBase> getVideosByTags(request: T): NndApiSearchResult =
        videosByTagsCache.getOrNull(request.hashCode()) {
            client.get().uri(buildApiRequestUri(request)).header(CONTENT_TYPE, APPLICATION_JSON_VALUE).retrieve().let {
                performLargeGet<NndApiSearchResult>(it, jsonMapper)
            }
        } ?: error("Failed to load videos for tag \"${request.tags}\"")

    @Suppress("SwallowedException")
    suspend fun getChannelHandle(channelId: Long) =
        try {
            client
                .get()
                .uri("$channelBaseHost/ch$channelId")
                .retrieve()
                .toBodilessEntity()
                .awaitSingle()
                ?.headers
                ?.get("location")
                ?.first()
                ?.substring(1)
        } catch (e: WebClientResponseException.NotFound) {
            null
        }

    @Scheduled(cron = "0 0 22 * * *")
    fun invalidateVideosByTagsCache() {
        videosByTagsCache.invalidateAll()
    }

    private fun <T : VideosByNndTagsRequestBase> buildApiRequestUri(request: T) =
        UriComponentsBuilder.fromUriString(apiBaseUrl)
            .path("/api/v2/snapshot/video/contents/search")
            .queryParam("q", request.joinTags())
            .queryParam("_offset", request.startOffset)
            .queryParam("_limit", request.maxResults)
            .queryParam("_sort", request.orderBy)
            .queryParam("targets", "tagsExact")
            .queryParam("fields", API_SEARCH_FIELDS)
            .queryParam("jsonFilter", jsonMapper.writeValueAsString(createNndFilters(request)))
            .build()
            .encode()
            .toUri()
}
