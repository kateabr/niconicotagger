package niconicotagger.client

import com.fasterxml.jackson.databind.json.JsonMapper
import com.github.benmanes.caffeine.cache.Caffeine
import com.sksamuel.aedile.core.asCache
import io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS
import io.netty.handler.logging.LogLevel.INFO
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import java.time.Duration
import java.util.concurrent.TimeUnit.HOURS
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import niconicotagger.constants.Constants.COOKIE_HEADER_KEY
import niconicotagger.constants.Constants.DEFAULT_USER_AGENT
import niconicotagger.dto.VocaDbLoginException
import niconicotagger.dto.api.misc.ApiType
import niconicotagger.dto.api.misc.ApiType.ARTISTS
import niconicotagger.dto.api.misc.ApiType.SONGS
import niconicotagger.dto.api.misc.VocaDbSortOrder
import niconicotagger.dto.inner.vocadb.DbLoginError
import niconicotagger.dto.inner.vocadb.DbLoginSuccess
import niconicotagger.dto.inner.vocadb.VocaDbArtist
import niconicotagger.dto.inner.vocadb.VocaDbCustomQueryArtistData
import niconicotagger.dto.inner.vocadb.VocaDbCustomQueryData
import niconicotagger.dto.inner.vocadb.VocaDbCustomQuerySongData
import niconicotagger.dto.inner.vocadb.VocaDbReleaseEvent
import niconicotagger.dto.inner.vocadb.VocaDbReleaseEventSeries
import niconicotagger.dto.inner.vocadb.VocaDbSongEntryBase
import niconicotagger.dto.inner.vocadb.VocaDbTag
import niconicotagger.dto.inner.vocadb.VocaDbTagMapping
import niconicotagger.dto.inner.vocadb.VocaDbTagMappings
import niconicotagger.dto.inner.vocadb.VocaDbTagSelectable
import niconicotagger.dto.inner.vocadb.VocaDbTagUsages
import niconicotagger.dto.inner.vocadb.VocaDbUser
import niconicotagger.dto.inner.vocadb.search.result.ArtistDuplicateSearchResult
import niconicotagger.dto.inner.vocadb.search.result.VocaDbArtistSearchResult
import niconicotagger.dto.inner.vocadb.search.result.VocaDbCustomQuerySearchResult
import niconicotagger.dto.inner.vocadb.search.result.VocaDbReleaseEventSearchResult
import niconicotagger.dto.inner.vocadb.search.result.VocaDbSongEntryWithNndPvsAndTagsSearchResult
import niconicotagger.dto.inner.vocadb.search.result.VocaDbTagSearchResult
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.HttpHeaders.USER_AGENT
import org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.util.MultiValueMap
import org.springframework.web.reactive.function.BodyInserters.fromFormData
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException.InternalServerError
import org.springframework.web.reactive.function.client.awaitBodilessEntity
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.awaitExchange
import org.springframework.web.reactive.function.client.toEntity
import reactor.netty.Connection
import reactor.netty.http.client.HttpClient
import reactor.netty.transport.logging.AdvancedByteBufFormat.TEXTUAL
import reactor.util.retry.Retry

/** Swagger: https://vocadb.net/swagger/index.html */
open class VocaDbClient(private val baseUrl: String, private val jsonMapper: JsonMapper) {
    private val timeoutSeconds: Int = 45

    private val client: WebClient =
        WebClient.builder()
            .clientConnector(
                ReactorClientHttpConnector(
                    HttpClient.create()
                        .wiretap(this::class.java.getCanonicalName(), INFO, TEXTUAL)
                        .option(CONNECT_TIMEOUT_MILLIS, timeoutSeconds * 1000)
                        .doOnConnected { conn: Connection ->
                            conn
                                .addHandlerFirst(ReadTimeoutHandler(timeoutSeconds))
                                .addHandlerFirst(WriteTimeoutHandler(timeoutSeconds))
                        }
                )
            )
            .baseUrl(baseUrl)
            .defaultHeader(USER_AGENT, DEFAULT_USER_AGENT)
            .defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
            .codecs { configurer -> configurer.defaultCodecs().maxInMemorySize(500 * 1024) }
            .filter { request, next ->
                next
                    .exchange(request)
                    .retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(2)).filter { it is InternalServerError })
            }
            .build()
    private var maxTagMappingsToLoad = 10_000
    private val tagMappingsCache =
        Caffeine.newBuilder().expireAfterWrite(1, HOURS).asCache<String, List<VocaDbTagMapping>>()

    suspend fun login(username: String, password: String): MultiValueMap<String, String> {
        val loginPayload = mapOf("keepLoggedIn" to true, "userName" to username, "password" to password)

        val loginResponse =
            client.post().uri("/api/users/login").bodyValue(loginPayload).awaitExchange {
                if (it.statusCode().is2xxSuccessful) DbLoginSuccess(it.cookies())
                else if (it.statusCode().is4xxClientError) it.awaitBody(DbLoginError::class)
                else throw it.createException().awaitSingle()
            }

        val cookies =
            when (loginResponse) {
                is DbLoginSuccess -> loginResponse.cookies
                is DbLoginError -> throw VocaDbLoginException(loginResponse)
            }

        if (!cookies.containsKey(COOKIE_HEADER_KEY)) error("Required cookies are not found")

        val clientCookies =
            MultiValueMap.fromMultiValue(
                cookies.entries.associate { entry -> entry.key to entry.value.map { it.value } }
            )
        checkUser(clientCookies)
        return clientCookies
    }

    private suspend fun checkUser(cookies: MultiValueMap<String, String>) {
        client
            .get()
            .uri("/api/users/current")
            .cookies { it.addAll(cookies) }
            .retrieve()
            .toEntity(VocaDbUser::class.java)
            .awaitSingle()
            ?.body
            ?.checkUserPermissions() ?: error("Could not get user information")
    }

    suspend fun getTagByName(name: String): VocaDbTag {
        val searchResult =
            client
                .get()
                .uri(
                    "/api/tags?query={query}&maxResults={maxResults}&getTotalCount=true",
                    mapOf("query" to name, "maxResults" to 1),
                )
                .retrieve()
                .toEntity(VocaDbTagSearchResult::class.java)
                .awaitSingle()
                .body ?: error("Tag \"$name\" not found")

        if (searchResult.totalCount != 1L)
            error(
                "Received incorrect number of tags: expected 1, but found ${searchResult.totalCount} (ids=${searchResult.items.map { it.id }})"
            )

        return searchResult.items[0]
    }

    suspend fun getTagUsages(apiType: ApiType, id: Long, cookie: String): VocaDbTagUsages {
        return client
            .get()
            .uri("/api/{apiType}/{id}/tagUsages", mapOf("apiType" to apiType, "id" to id))
            .cookies { it.add(COOKIE_HEADER_KEY, cookie) }
            .retrieve()
            .toEntity(VocaDbTagUsages::class.java)
            .awaitSingle()
            ?.body ?: error("Could not get tag usages for S/$id")
    }

    suspend fun deleteTagUsage(apiType: ApiType, tagUsageId: Long, cookie: String) {
        client
            .delete()
            .uri(
                "/api/users/current/{tagType}/{tagUsageId}",
                mapOf("tagType" to apiType.tagType, "tagUsageId" to tagUsageId),
            )
            .cookies { it.add(COOKIE_HEADER_KEY, cookie) }
            .exchangeToMono { if (it.statusCode().is2xxSuccessful) it.releaseBody() else it.createError() }
            .awaitSingleOrNull()
    }

    suspend fun getEventByName(name: String, fields: String): VocaDbReleaseEvent {
        return client
            .get()
            .uri(
                "/api/releaseEvents?query={query}&lang=Default&fields=$fields&nameMatchMode=Exact&maxResults=1&getTotalCount=true",
                mapOf("query" to name),
            )
            .retrieve()
            .toEntity(VocaDbReleaseEventSearchResult::class.java)
            .awaitSingle()
            .body
            ?.also {
                if (it.totalCount != 1L)
                    error(
                        "Received incorrect number of events: expected 1, but found ${it.totalCount} (ids=${it.items.map { item -> item.id }})"
                    )
            }
            ?.items
            ?.first() ?: error("Event \"$name\" not found")
    }

    suspend fun getEventSeriesById(id: Long, fields: String = "None"): VocaDbReleaseEventSeries {
        return client
            .get()
            .uri("/api/releaseEventSeries/{id}?fields={fields}", mapOf("id" to id, "fields" to fields))
            .retrieve()
            .toEntity(VocaDbReleaseEventSeries::class.java)
            .awaitSingle()
            .body ?: error("Event series id=$id not found")
    }

    suspend fun getSongForEdit(id: Long, cookie: String): MutableMap<String, Any> {
        return client
            .get()
            .uri("/api/songs/{id}/for-edit", id)
            .cookies { it.add(COOKIE_HEADER_KEY, cookie) }
            .retrieve()
            .toEntity(object : ParameterizedTypeReference<MutableMap<String, Any>>() {})
            .awaitSingle()
            .body ?: error("Failed to load song S/$id for editing")
    }

    suspend fun saveSong(id: Long, songData: Map<String, Any>, cookie: String) {
        client
            .post()
            .uri("/api/songs/{id}", id)
            .contentType(APPLICATION_FORM_URLENCODED)
            .cookies { it.add(COOKIE_HEADER_KEY, cookie) }
            .body(fromFormData("contract", jsonMapper.writeValueAsString(songData)))
            .retrieve()
            .awaitBodilessEntity()
    }

    open suspend fun getAllVocaDbTagMappings(useCachedMappings: Boolean): List<VocaDbTagMapping> {
        if (useCachedMappings) {
            val cached = tagMappingsCache.getIfPresent("mappings")
            if (cached != null) return cached
        }
        while (true) {
            val response =
                client
                    .get()
                    .uri(
                        "/api/tags/mappings?start=0&maxEntries={maxEntries}&getTotalCount=true",
                        mapOf("maxEntries" to maxTagMappingsToLoad),
                    )
                    .retrieve()
                    .toEntity<VocaDbTagMappings>()
                    .awaitSingle()
                    .body ?: error("Failed to load tag mappings")

            if (response.totalCount > response.items.size) {
                maxTagMappingsToLoad += 1000
                continue
            }

            tagMappingsCache.put("mappings", response.items)
            return response.items
        }
    }

    suspend fun <T : VocaDbSongEntryBase> getSongByNndPv(pvId: String, fields: String, responseClass: Class<T>): T? {
        return client
            .get()
            .uri("/api/songs/byPv?pvId=$pvId&pvService=NicoNicoDouga&fields=$fields")
            .retrieve()
            .toEntity(responseClass)
            .awaitSingle()
            .body
    }

    suspend fun getArtistByQuery(query: String): VocaDbArtist? {
        return client
            .get()
            .uri("/api/artists?query={query}", mapOf("query" to query))
            .retrieve()
            .toEntity(VocaDbArtistSearchResult::class.java)
            .awaitSingle()
            .body
            ?.items
            ?.firstOrNull()
    }

    suspend fun findArtistDuplicate(linkUrl: String): VocaDbArtist? {
        return (client
                .post()
                .uri("/Artist/FindDuplicate")
                .contentType(APPLICATION_FORM_URLENCODED)
                .body(
                    fromFormData(
                        MultiValueMap.fromSingleValue(
                            mapOf("term1" to "", "term2" to "", "term3" to "", "linkUrl" to linkUrl)
                        )
                    )
                )
                .retrieve()
                .toEntity(artistDuplicateResponseTypeReference)
                .awaitSingle()
                .body ?: error("Could not check if \"$linkUrl\" is already associated with any artist"))
            .let { if (it.isEmpty()) null else VocaDbArtist(it[0].entry.id, it[0].entry.name.displayName) }
    }

    suspend fun artistHasSongsBeforeDate(artistId: Long, timestamp: String): Boolean {
        return client
            .get()
            .uri(
                "/api/songs?artistId[]={artistId}&beforeDate={beforeDate}&maxResults=1&getTotalCount=true",
                mapOf("artistId" to artistId, "beforeDate" to timestamp),
            )
            .retrieve()
            .toEntity(SimpleSearchResult::class.java)
            .awaitSingle()
            ?.body
            ?.let { it.totalCount > 0 }
            ?: error("Could not check if artist id=$artistId has published any songs prior to $timestamp")
    }

    suspend fun getSongTags(songId: Long, cookie: String): List<VocaDbTagSelectable> {
        return client
            .get()
            .uri("/api/users/current/songTags/{songId}", mapOf("songId" to songId))
            .cookies { it.add(COOKIE_HEADER_KEY, cookie) }
            .retrieve()
            .toEntityList(VocaDbTagSelectable::class.java)
            .awaitSingle()
            ?.body ?: error("Could not load currently selected tags for entry S/$songId")
    }

    suspend fun assignSongTags(songId: Long, tags: List<VocaDbTag>, cookie: String) {
        client
            .put()
            .uri("/api/users/current/songTags/{songId}", mapOf("songId" to songId))
            .cookies { it.add(COOKIE_HEADER_KEY, cookie) }
            .bodyValue(tags)
            .retrieve()
            .toBodilessEntity()
            .retryWhen(Retry.backoff(5, Duration.ofSeconds(1)).filter { it is InternalServerError })
            .awaitSingleOrNull() ?: error("Could not assign tags $tags to entry S/$songId")
    }

    suspend fun getDataWithTagsByCustomQuery(
        apiType: ApiType,
        query: String,
    ): VocaDbCustomQuerySearchResult<out VocaDbCustomQueryData> {
        return client
            .get()
            .uri { uri ->
                uri.path("/api")
                    .path("/$apiType")
                    .queryParam("fields", "Tags")
                    .queryParam("getTotalCount", "true")
                    .query(query)
                    .build()
            }
            .retrieve()
            .toEntity(
                when (apiType) {
                    ARTISTS -> queryConsoleArtistResponseTypeReference
                    SONGS -> queryConsoleSongResponseTypeReference
                }
            )
            .awaitSingle()
            ?.body ?: error("Could not load data")
    }

    suspend fun getSongs(
        startOffset: Long,
        maxResults: Long,
        orderBy: VocaDbSortOrder,
        additionalParams: Map<String, Any>,
    ): VocaDbSongEntryWithNndPvsAndTagsSearchResult {
        return (client
            .get()
            .uri { uri ->
                val builder =
                    uri.path("/api/songs")
                        .queryParam("start", startOffset)
                        .queryParam("maxResults", maxResults)
                        .queryParam("sort", orderBy)
                        .queryParam("getTotalCount", true)
                additionalParams.forEach { (param, value) -> builder.queryParam(param, value) }
                builder.build()
            }
            .retrieve()
            .toEntity(VocaDbSongEntryWithNndPvsAndTagsSearchResult::class.java)
            .awaitSingle()
            ?.body ?: error("Could not load songs"))
    }

    companion object {
        private val queryConsoleArtistResponseTypeReference =
            object : ParameterizedTypeReference<VocaDbCustomQuerySearchResult<VocaDbCustomQueryArtistData>>() {}
        private val queryConsoleSongResponseTypeReference =
            object : ParameterizedTypeReference<VocaDbCustomQuerySearchResult<VocaDbCustomQuerySongData>>() {}

        val artistDuplicateResponseTypeReference =
            object : ParameterizedTypeReference<List<ArtistDuplicateSearchResult>>() {}

        private data class SimpleSearchResult(val totalCount: Long)
    }
}
