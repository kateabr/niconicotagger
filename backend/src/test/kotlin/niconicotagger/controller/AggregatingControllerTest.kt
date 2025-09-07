package niconicotagger.controller

import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.equalToDateTime
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.not
import com.github.tomakehurst.wiremock.client.WireMock.ok
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathTemplate
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder
import java.time.LocalDate
import java.util.stream.Stream
import kotlinx.coroutines.runBlocking
import niconicotagger.Utils.jsonMapper
import niconicotagger.Utils.loadResource
import niconicotagger.constants.Constants.API_SEARCH_FIELDS
import niconicotagger.constants.Constants.DEFAULT_USER_AGENT
import niconicotagger.constants.Constants.GENRE_FILTER
import niconicotagger.dto.api.misc.ApiType
import niconicotagger.dto.api.misc.ApiType.ARTISTS
import niconicotagger.dto.api.misc.ApiType.SONGS
import niconicotagger.dto.api.misc.ClientType.VOCADB_BETA
import niconicotagger.dto.api.misc.NndSortOrder.PUBLISH_TIME
import niconicotagger.dto.api.misc.VocaDbSortOrder.AdditionDate
import niconicotagger.dto.api.misc.VocaDbSortOrder.PublishDate
import niconicotagger.dto.inner.misc.EntryField.ReleaseEvent
import niconicotagger.dto.inner.misc.PvService.NicoNicoDouga
import org.instancio.junit.InstancioExtension
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.ArgumentSet
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.HttpHeaders.USER_AGENT
import org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.MediaType.APPLICATION_XML_VALUE
import org.springframework.http.MediaType.TEXT_HTML_VALUE
import org.springframework.test.json.JsonCompareMode.STRICT
import org.springframework.test.web.servlet.post

@ExtendWith(InstancioExtension::class)
class AggregatingControllerTest : AbstractControllerTest() {

    @ParameterizedTest(name = "[{index}] apiType={0}")
    @ArgumentsSource(DataByCustomQuerySuccessTestData::class)
    fun `get data by custom query test`(apiType: ApiType, entryLookupResult: String, expectedResponse: String): Unit =
        runBlocking {
            wireMockExtension.stubFor(
                get(urlPathTemplate("/api/{apiType}"))
                    .withPathParam("apiType", equalTo(apiType.toString()))
                    .withQueryParams(
                        mapOf(
                            "fields" to equalTo("Tags"),
                            "getTotalCount" to equalTo("true"),
                            "start" to equalTo("0"),
                            "maxResults" to equalTo("10"),
                        )
                    )
                    .withHeader(CONTENT_TYPE, equalTo(APPLICATION_JSON_VALUE))
                    .withHeader(USER_AGENT, equalTo(DEFAULT_USER_AGENT))
                    .willReturn(okJson(entryLookupResult))
            )

            mockMvc
                .post("/api/get/data/by_custom_query") {
                    contentType = APPLICATION_JSON
                    content =
                        """
                {
                    "apiType": "$apiType",
                    "query": "start=0&maxResults=10",
                    "clientType": "$VOCADB_BETA"
                }
                 """
                            .trimIndent()
                }
                .asyncDispatch()
                .andExpect {
                    status { isOk() }
                    content { json(expectedResponse, STRICT) }
                }
        }

    @Test
    fun `get event schedule test`() {
        val basePath = "responses/integration/aggregate/event_schedule"
        wireMockExtension.stubFor(
            get(urlPathTemplate("/api/releaseEvents"))
                .withQueryParams(
                    mapOf(
                        "start" to equalTo("0"),
                        "maxResults" to equalTo("1000"),
                        "getTotalCount" to equalTo("true"),
                        "fields" to equalTo("Series,MainPicture"),
                        "sort" to equalTo("Date"),
                        "sortDirection" to equalTo("Ascending"),
                    )
                )
                .withHeader(CONTENT_TYPE, equalTo(APPLICATION_JSON_VALUE))
                .withHeader(USER_AGENT, equalTo(DEFAULT_USER_AGENT))
                .willReturn(okJson(loadResource("$basePath/endless_events_response.json").decodeToString()))
        )
        wireMockExtension.stubFor(
            get(urlPathTemplate("/api/releaseEvents"))
                .withQueryParams(
                    mapOf(
                        "afterDate" to
                            equalToDateTime(
                                LocalDate.now().minusYears(1).withMonth(12).withDayOfMonth(31).atStartOfDay()
                            ),
                        "beforeDate" to
                            equalToDateTime(
                                LocalDate.now().plusYears(1).withMonth(1).withDayOfMonth(31).atStartOfDay()
                            ),
                        "start" to equalTo("0"),
                        "maxResults" to equalTo("1000"),
                        "getTotalCount" to equalTo("true"),
                        "fields" to equalTo("Series,MainPicture"),
                        "sort" to equalTo("Date"),
                        "sortDirection" to equalTo("Ascending"),
                    )
                )
                .withHeader(CONTENT_TYPE, equalTo(APPLICATION_JSON_VALUE))
                .withHeader(USER_AGENT, equalTo(DEFAULT_USER_AGENT))
                .willReturn(okJson(loadResource("$basePath/all_events_for_year_response.json").decodeToString()))
        )
        wireMockExtension.stubFor(
            get(urlPathTemplate("/api/frontpage"))
                .withHeader(CONTENT_TYPE, equalTo(APPLICATION_JSON_VALUE))
                .withHeader(USER_AGENT, equalTo(DEFAULT_USER_AGENT))
                .willReturn(okJson(loadResource("$basePath/frontpage_response.json").decodeToString()))
        )

        mockMvc
            .post("/api/get/recent_events") {
                contentType = APPLICATION_JSON
                content =
                    """
                    {
                        "clientType": "$VOCADB_BETA",
                        "useCached": false
                    }
                     """
                        .trimIndent()
            }
            .asyncDispatch()
            .andExpect {
                status { isOk() }
                content { json(loadResource("$basePath/expected_response.json").decodeToString(), STRICT) }
            }
    }

    @ParameterizedTest
    @ValueSource(longs = [0, 366])
    fun `get event schedule test (invalid request)`(eventScopeDays: Long) {
        testInvalidRequest(
            """
                {
                    "eventScopeDays": $eventScopeDays,
                    "clientType": "$VOCADB_BETA"
                }
                """
                .trimIndent(),
            """
                {
                  "type": "https://zalando.github.io/problem/constraint-violation",
                  "status": 400,
                  "violations": [
                    {
                      "field": "eventScopeDays",
                      "message": "must be between 1 and 365"
                    }
                  ],
                  "title": "Constraint Violation"
                }
                """
                .trimIndent(),
            "/get/recent_events",
        )
    }

    @Nested
    inner class GetReleaseEventTest {

        private fun testValidRequest(
            eventLookupResult: Map<String, String>,
            seriesLookupResult: Map<String, String>,
            expectedResponse: String,
            eventFields: String,
            seriesFields: String,
            uriPath: String,
        ) {
            eventLookupResult.forEach {
                wireMockExtension.stubFor(
                    get(urlPathEqualTo("/api/releaseEvents"))
                        .withQueryParams(
                            mapOf(
                                "query" to equalTo(it.key),
                                "lang" to equalTo("Default"),
                                "fields" to equalTo(eventFields),
                                "nameMatchMode" to equalTo("Exact"),
                                "maxResults" to equalTo("1"),
                                "getTotalCount" to equalTo("true"),
                            )
                        )
                        .withHeader(CONTENT_TYPE, equalTo(APPLICATION_JSON_VALUE))
                        .withHeader(USER_AGENT, equalTo(DEFAULT_USER_AGENT))
                        .willReturn(okJson(it.value))
                )
            }
            seriesLookupResult.forEach {
                wireMockExtension.stubFor(
                    get(urlPathTemplate("/api/releaseEventSeries/{id}"))
                        .withPathParam("id", equalTo(it.key))
                        .withQueryParam("fields", equalTo(seriesFields))
                        .withHeader(CONTENT_TYPE, equalTo(APPLICATION_JSON_VALUE))
                        .withHeader(USER_AGENT, equalTo(DEFAULT_USER_AGENT))
                        .willReturn(okJson(it.value))
                )
            }

            mockMvc
                .post("/api/get$uriPath") {
                    contentType = APPLICATION_JSON
                    content =
                        """
                {
                    "eventName": "${eventLookupResult.keys.first()}",
                    "clientType": "$VOCADB_BETA"
                }
                 """
                            .trimIndent()
                }
                .asyncDispatch()
                .andExpect {
                    status { isOk() }
                    content { json(expectedResponse, STRICT) }
                }
        }

        @ParameterizedTest
        @ArgumentsSource(ReleaseEventTestData::class)
        fun `get release event test (success)`(
            eventLookupResult: Map<String, String>,
            seriesLookupResult: Map<String, String>,
            expectedResponse: String,
        ) {
            testValidRequest(
                eventLookupResult,
                seriesLookupResult,
                expectedResponse,
                "WebLinks",
                "WebLinks",
                "/release_event",
            )
        }

        @ParameterizedTest
        @ArgumentsSource(ReleaseEventWithLinkedTagTestData::class)
        fun `get release event with linked tag test (success)`(
            eventLookupResult: Map<String, String>,
            seriesLookupResult: Map<String, String>,
            expectedResponse: String,
        ) {
            testValidRequest(
                eventLookupResult,
                seriesLookupResult,
                expectedResponse,
                "Tags",
                "None",
                "/release_event/with_linked_tags",
            )
        }

        @ParameterizedTest(name = "[{index}] {0}")
        @CsvSource(
            value = ["release event,/release_event", "release event with linked tag,/release_event/with_linked_tags"]
        )
        fun `get release event test (invalid request)`(ignored: String, uriPath: String) {
            testInvalidRequest(
                """
                {
                    "eventName": "",
                    "clientType": "$VOCADB_BETA"
                }
                """
                    .trimIndent(),
                """
                {
                  "type": "https://zalando.github.io/problem/constraint-violation",
                  "status": 400,
                  "violations": [
                    {
                      "field": "eventName",
                      "message": "must not be blank"
                    }
                  ],
                  "title": "Constraint Violation"
                }
                """
                    .trimIndent(),
                "/get$uriPath",
            )
        }
    }

    @Nested
    inner class GetVideosByNndTagsTest {
        private fun setupStubs(
            tagLookupResult: Map<String, String>,
            tagMappings: String,
            tagQuery: String,
            nndVideoSearchResult: String,
            dbSongLookupResult: Map<String, String?>,
            dbPublisherLookupResultUser: Map<String, String>,
            dbPublisherLookupResultChannel: Map<String, String>,
            thumbnails: Map<String, String>,
            embeds: Map<String, String>,
            songFields: String,
        ) {
            tagLookupResult.forEach {
                wireMockExtension.stubFor(
                    get(urlPathEqualTo("/api/tags"))
                        .withQueryParams(
                            mapOf(
                                "query" to equalTo(it.key),
                                "maxResults" to equalTo("1"),
                                "getTotalCount" to equalTo("true"),
                            )
                        )
                        .withHeader(CONTENT_TYPE, equalTo(APPLICATION_JSON_VALUE))
                        .withHeader(USER_AGENT, equalTo(DEFAULT_USER_AGENT))
                        .willReturn(okJson(it.value))
                )
            }
            wireMockExtension.stubFor(
                get(urlPathEqualTo("/api/tags/mappings"))
                    .withQueryParams(
                        mapOf(
                            "start" to equalTo("0"),
                            "maxEntries" to equalTo("10000"),
                            "getTotalCount" to equalTo("true"),
                        )
                    )
                    .withHeader(CONTENT_TYPE, equalTo(APPLICATION_JSON_VALUE))
                    .withHeader(USER_AGENT, equalTo(DEFAULT_USER_AGENT))
                    .willReturn(okJson(tagMappings))
            )
            wireMockExtension.stubFor(
                get(urlPathEqualTo("/api/v2/snapshot/video/contents/search"))
                    .withQueryParams(
                        mapOf(
                            "q" to equalTo(tagQuery),
                            "_offset" to equalTo("0"),
                            "_limit" to equalTo("10"),
                            "_sort" to equalTo("viewCounter"),
                            "targets" to equalTo("tagsExact"),
                            "fields" to equalTo(API_SEARCH_FIELDS),
                            "jsonFilter" to equalToJson(jsonMapper.writeValueAsString(GENRE_FILTER)),
                        )
                    )
                    .withHeader(CONTENT_TYPE, equalTo(APPLICATION_JSON_VALUE))
                    .withHeader(USER_AGENT, equalTo(DEFAULT_USER_AGENT))
                    .willReturn(okJson(nndVideoSearchResult))
            )
            dbSongLookupResult.forEach {
                wireMockExtension.stubFor(
                    get(urlPathEqualTo("/api/songs/byPv"))
                        .withQueryParams(
                            mapOf(
                                "pvId" to equalTo(it.key),
                                "pvService" to equalTo(NicoNicoDouga.toString()),
                                "fields" to equalTo(songFields),
                            )
                        )
                        .withHeader(CONTENT_TYPE, equalTo(APPLICATION_JSON_VALUE))
                        .withHeader(USER_AGENT, equalTo(DEFAULT_USER_AGENT))
                        .willReturn(okJson(it.value))
                )
            }
            thumbnails.forEach {
                wireMockExtension.stubFor(
                    get(urlPathTemplate("/api/getthumbinfo/{id}"))
                        .withPathParam("id", equalTo(it.key))
                        .withHeader(CONTENT_TYPE, equalTo(APPLICATION_XML_VALUE))
                        .withHeader(USER_AGENT, equalTo(DEFAULT_USER_AGENT))
                        .willReturn(okJson(it.value))
                )
            }
            dbPublisherLookupResultUser.forEach {
                wireMockExtension.stubFor(
                    get(urlPathEqualTo("/api/artists"))
                        .withQueryParams(mapOf("query" to equalTo(it.key)))
                        .withHeader(CONTENT_TYPE, equalTo(APPLICATION_JSON_VALUE))
                        .withHeader(USER_AGENT, equalTo(DEFAULT_USER_AGENT))
                        .willReturn(okJson(it.value))
                )
            }
            // channel links in entries
            dbPublisherLookupResultChannel.forEach {
                wireMockExtension.stubFor(
                    post(urlPathEqualTo("/Artist/FindDuplicate"))
                        .withFormParam("term1", equalTo(""))
                        .withFormParam("term2", equalTo(""))
                        .withFormParam("term3", equalTo(""))
                        .withFormParam("linkUrl", equalTo(it.key))
                        .withHeader(CONTENT_TYPE, equalTo(APPLICATION_FORM_URLENCODED_VALUE))
                        .withHeader(USER_AGENT, equalTo(DEFAULT_USER_AGENT))
                        .willReturn(okJson(it.value))
                )
            }
            // shortened channel links in entries
            dbPublisherLookupResultChannel.forEach {
                wireMockExtension.stubFor(
                    post(urlPathEqualTo("/Artist/FindDuplicate"))
                        .withFormParam("term1", equalTo(""))
                        .withFormParam("term2", equalTo(""))
                        .withFormParam("term3", equalTo(""))
                        .withFormParam("linkUrl", not(equalTo(it.key)))
                        .withHeader(CONTENT_TYPE, equalTo(APPLICATION_FORM_URLENCODED_VALUE))
                        .withHeader(USER_AGENT, equalTo(DEFAULT_USER_AGENT))
                        .willReturn(okJson(it.value))
                )
            }
            embeds.forEach {
                wireMockExtension.stubFor(
                    get(urlPathTemplate("/watch/{id}"))
                        .withPathParam("id", equalTo(it.key))
                        .withHeader(USER_AGENT, equalTo(DEFAULT_USER_AGENT))
                        .withHeader(CONTENT_TYPE, equalTo(TEXT_HTML_VALUE))
                        .willReturn(ok(it.value))
                )
            }
        }

        private fun testValidRequest(
            request: String,
            tagLookupResult: Map<String, String>,
            tagMappings: String,
            tagQuery: String,
            nndVideoSearchResult: String,
            dbSongLookupResult: Map<String, String?>,
            dbPublisherLookupResultUser: Map<String, String>,
            dbPublisherLookupResultChannel: Map<String, String>,
            thumbnails: Map<String, String>,
            embeds: Map<String, String>,
            expectedResponse: String,
            uriPath: String,
            songFields: String,
        ) {
            setupStubs(
                tagLookupResult,
                tagMappings,
                tagQuery,
                nndVideoSearchResult,
                dbSongLookupResult,
                dbPublisherLookupResultUser,
                dbPublisherLookupResultChannel,
                thumbnails,
                embeds,
                songFields,
            )

            mockMvc
                .post("/api/get/videos$uriPath") {
                    contentType = APPLICATION_JSON
                    content = request
                }
                .asyncDispatch()
                .andExpect {
                    status { isOk() }
                    content { json(expectedResponse.replace("https://beta.vocadb.net", testDbHost), STRICT) }
                }
        }

        @ParameterizedTest
        @ArgumentsSource(GetVideosByNndTagsTestData::class)
        fun `get videos by NND tags test (success)`(
            request: String,
            tagLookupResult: Map<String, String>,
            tagMappings: String,
            tagQuery: String,
            nndVideoSearchResult: String,
            dbSongLookupResult: Map<String, String?>,
            dbPublisherLookupResultUser: Map<String, String>,
            dbPublisherLookupResultChannel: Map<String, String>,
            thumbnails: Map<String, String>,
            embeds: Map<String, String>,
            expectedResponse: String,
        ) {
            testValidRequest(
                request,
                tagLookupResult,
                tagMappings,
                tagQuery,
                nndVideoSearchResult,
                dbSongLookupResult,
                dbPublisherLookupResultUser,
                dbPublisherLookupResultChannel,
                thumbnails,
                embeds,
                expectedResponse,
                "/by_nnd_tags/for_tagging",
                "Tags,Artists",
            )
        }

        @ParameterizedTest
        @ArgumentsSource(GetVideosByVocaDbTagMappingsTestData::class)
        fun `get videos by VocaDB tag mappings test (success)`(
            request: String,
            tagLookupResult: Map<String, String>,
            tagMappings: String,
            tagQuery: String,
            nndVideoSearchResult: String,
            dbSongLookupResult: Map<String, String?>,
            dbPublisherLookupResultUser: Map<String, String>,
            dbPublisherLookupResultChannel: Map<String, String>,
            thumbnails: Map<String, String>,
            embeds: Map<String, String>,
            expectedResponse: String,
        ) {
            testValidRequest(
                request,
                tagLookupResult,
                tagMappings,
                tagQuery,
                nndVideoSearchResult,
                dbSongLookupResult,
                dbPublisherLookupResultUser,
                dbPublisherLookupResultChannel,
                thumbnails,
                embeds,
                expectedResponse,
                "/by_vocadb_tag",
                "Tags,Artists",
            )
        }

        @ParameterizedTest
        @ArgumentsSource(GetVideosByEventNndTagsTestData::class)
        fun `get videos by VocaDB event NND tags test (success)`(
            request: String,
            tagLookupResult: Map<String, String>,
            tagMappings: String,
            tagQuery: String,
            nndVideoSearchResult: String,
            dbSongLookupResult: Map<String, String?>,
            dbPublisherLookupResultUser: Map<String, String>,
            dbPublisherLookupResultChannel: Map<String, String>,
            thumbnails: Map<String, String>,
            embeds: Map<String, String>,
            expectedResponse: String,
        ) {
            testValidRequest(
                request,
                tagLookupResult,
                tagMappings,
                tagQuery,
                nndVideoSearchResult,
                dbSongLookupResult,
                dbPublisherLookupResultUser,
                dbPublisherLookupResultChannel,
                thumbnails,
                embeds,
                expectedResponse,
                "/by_nnd_tags/for_event",
                "ReleaseEvent",
            )
        }

        @ParameterizedTest
        @ArgumentsSource(GetVideosByEventNndTagsCacheTestData::class)
        fun `get videos by VocaDB event NND tags from cache test`(
            request: String,
            tagQuery: String,
            nndVideoSearchResult: String,
            dbSongLookupResult: Map<String, String?>,
            thumbnails: Map<String, String>,
            dbSongByPvLookupCount: Int,
        ) {
            setupStubs(
                emptyMap(),
                """
            {
              "items": [],
              "totalCount": 0
            }
            """
                    .trimIndent(),
                tagQuery,
                nndVideoSearchResult,
                dbSongLookupResult,
                mapOf("user/102050" to "null"),
                emptyMap(),
                thumbnails,
                emptyMap(),
                ReleaseEvent.toString(),
            )
            repeat(2) {
                mockMvc
                    .post("/api/get/videos/by_nnd_tags/for_event") {
                        contentType = APPLICATION_JSON
                        content = request
                    }
                    .asyncDispatch()
                    .andExpect { status { isOk() } }
            }

            wireMockExtension.verify(
                dbSongByPvLookupCount,
                RequestPatternBuilder.newRequestPattern()
                    .withUrl("/api/songs/byPv?pvId=sm33123155&pvService=$NicoNicoDouga&fields=$ReleaseEvent"),
            )
        }

        @ParameterizedTest
        @ValueSource(ints = [9, 101])
        fun `get videos by NND tags test (invalid request)`(maxResults: Int) {
            testInvalidRequest(
                """
                {
                    "tags": [],
                    "scope": "",
                    "startOffset": -1,
                    "maxResults": $maxResults,
                    "orderBy": "$PUBLISH_TIME",
                    "clientType": "$VOCADB_BETA"
                }
                """
                    .trimIndent(),
                """
                {
                  "type": "https://zalando.github.io/problem/constraint-violation",
                  "status": 400,
                  "violations": [
                    {
                      "field": "maxResults",
                      "message": "must be between 10 and 100"
                    },
                    {
                      "field": "startOffset",
                      "message": "must be greater than or equal to 0"
                    },
                    {
                      "field": "tags",
                      "message": "must not be empty"
                    }
                  ],
                  "title": "Constraint Violation"
                }
                """
                    .trimIndent(),
                "/get/videos/by_nnd_tags/for_tagging",
            )
        }

        @ParameterizedTest
        @ValueSource(ints = [9, 101])
        fun `get videos by VocaDB tag mappings test (invalid request)`(maxResults: Int) {
            testInvalidRequest(
                """
                {
                    "tag": " ",
                    "scope": "",
                    "startOffset": -1,
                    "maxResults": $maxResults,
                    "orderBy": "$PUBLISH_TIME",
                    "clientType": "$VOCADB_BETA"
                }
                """
                    .trimIndent(),
                """
                {
                  "type": "https://zalando.github.io/problem/constraint-violation",
                  "status": 400,
                  "violations": [
                    {
                      "field": "maxResults",
                      "message": "must be between 10 and 100"
                    },
                    {
                      "field": "startOffset",
                      "message": "must be greater than or equal to 0"
                    },
                    {
                      "field": "tag",
                      "message": "must not be blank"
                    }
                  ],
                  "title": "Constraint Violation"
                }
                """
                    .trimIndent(),
                "/get/videos/by_vocadb_tag",
            )
        }

        @ParameterizedTest
        @ValueSource(ints = [9, 101])
        fun `get videos by VocaDB event NND tags test (invalid request)`(maxResults: Int) {
            testInvalidRequest(
                """
                {
                    "tags": [],
                    "scope": "",
                    "startOffset": -1,
                    "maxResults": $maxResults,
                    "orderBy": "$PUBLISH_TIME",
                    "dates": {
                        "from": "2025-04-13",
                        "to": null,
                        "applyToSearch": false
                    },
                    "clientType": "$VOCADB_BETA"
                }
                """
                    .trimIndent(),
                """
                {
                  "type": "https://zalando.github.io/problem/constraint-violation",
                  "status": 400,
                  "violations": [
                    {
                      "field": "maxResults",
                      "message": "must be between 10 and 100"
                    },
                    {
                      "field": "startOffset",
                      "message": "must be greater than or equal to 0"
                    },
                    {
                      "field": "tags",
                      "message": "must not be empty"
                    }
                  ],
                  "title": "Constraint Violation"
                }
                """
                    .trimIndent(),
                "/get/videos/by_nnd_tags/for_event",
            )
        }
    }

    @Nested
    inner class GetSongEntriesTest {
        private val dataFolder = "responses/integration/aggregate/songs"

        @Test
        fun `get VocaDB song entries for tagging test (success)`() {
            wireMockExtension.stubFor(
                get(urlPathEqualTo("/api/tags/mappings"))
                    .withQueryParams(
                        mapOf(
                            "start" to equalTo("0"),
                            "maxEntries" to equalTo("10000"),
                            "getTotalCount" to equalTo("true"),
                        )
                    )
                    .withHeader(CONTENT_TYPE, equalTo(APPLICATION_JSON_VALUE))
                    .withHeader(USER_AGENT, equalTo(DEFAULT_USER_AGENT))
                    .willReturn(okJson(loadResource("$dataFolder/tag_mappings.json").decodeToString()))
            )
            wireMockExtension.stubFor(
                get(urlPathEqualTo("/api/songs"))
                    .withQueryParams(
                        mapOf(
                            "start" to equalTo("0"),
                            "maxResults" to equalTo("10"),
                            "sort" to equalTo(PublishDate.toString()),
                            "getTotalCount" to equalTo("true"),
                            "pvServices" to equalTo(NicoNicoDouga.toString()),
                            "fields" to equalTo("PVs,Tags,Artists,ReleaseEvent"),
                        )
                    )
                    .withHeader(CONTENT_TYPE, equalTo(APPLICATION_JSON_VALUE))
                    .withHeader(USER_AGENT, equalTo(DEFAULT_USER_AGENT))
                    .willReturn(okJson(loadResource("$dataFolder/song_lookup_result.json").decodeToString()))
            )
            mapOf(
                    "sm44594436" to loadResource("$dataFolder/video_embed_result_sm44594436.html"),
                    "sm44594435" to loadResource("$dataFolder/video_embed_result_not_found.html"),
                    "sm44594434" to loadResource("$dataFolder/video_embed_result_sm44594434.html"),
                )
                .forEach {
                    wireMockExtension.stubFor(
                        get(urlPathTemplate("/watch/{id}"))
                            .withPathParam("id", equalTo(it.key))
                            .withHeader(CONTENT_TYPE, equalTo(TEXT_HTML_VALUE))
                            .withHeader(USER_AGENT, equalTo(DEFAULT_USER_AGENT))
                            .willReturn(ok().withBody(it.value).withHeader(CONTENT_TYPE, TEXT_HTML_VALUE))
                    )
                }
            mapOf(
                    "sm44594435" to loadResource("$dataFolder/video_lookup_result_not_found.xml"),
                    "sm44594434" to loadResource("$dataFolder/video_lookup_result_sm44594434.xml"),
                )
                .forEach {
                    wireMockExtension.stubFor(
                        get(urlPathTemplate("/api/getthumbinfo/{id}"))
                            .withPathParam("id", equalTo(it.key))
                            .withHeader(CONTENT_TYPE, equalTo(APPLICATION_XML_VALUE))
                            .withHeader(USER_AGENT, equalTo(DEFAULT_USER_AGENT))
                            .willReturn(ok().withBody(it.value).withHeader(CONTENT_TYPE, APPLICATION_XML_VALUE))
                    )
                }

            mockMvc
                .post("/api/get/songs") {
                    contentType = APPLICATION_JSON
                    content =
                        """
                {
                    "startOffset": 0,
                    "maxResults": 10,
                    "orderBy": "$PublishDate",
                    "clientType": "$VOCADB_BETA"
                }
                 """
                            .trimIndent()
                }
                .asyncDispatch()
                .andExpect {
                    status { isOk() }
                    content { json(loadResource("$dataFolder/expected_response.json").decodeToString(), STRICT) }
                }
        }

        @ParameterizedTest
        @ValueSource(ints = [9, 101])
        fun `get VocaDB song entries for tagging test (invalid request)`(maxResults: Int) {
            mockMvc
                .post("/api/get/songs") {
                    contentType = APPLICATION_JSON
                    content =
                        """
                {
                    "startOffset": -1,
                    "maxResults": $maxResults,
                    "orderBy": "$AdditionDate",
                    "clientType": "$VOCADB_BETA"
                }
                """
                            .trimIndent()
                }
                .andExpect {
                    status { is4xxClientError() }
                    content {
                        json(
                            """
                        {
                          "type": "https://zalando.github.io/problem/constraint-violation",
                          "status": 400,
                          "violations": [
                            {
                              "field": "maxResults",
                              "message": "must be between 10 and 100"
                            },
                            {
                              "field": "startOffset",
                              "message": "must be greater than or equal to 0"
                            }
                          ],
                          "title": "Constraint Violation"
                        }
                        """
                                .trimIndent(),
                            STRICT,
                        )
                    }
                }
        }
    }

    companion object {
        abstract class ReleaseEventTestDataBase(private val dataFolder: String) : ArgumentsProvider {
            private fun series(): ArgumentSet {
                return argumentSet(
                    "series event",
                    mapOf(
                        "11月1日はネル誕生祭 2008" to
                            loadResource("responses/integration/aggregate/$dataFolder/series/event_lookup_result.json")
                                .decodeToString()
                    ),
                    mapOf(
                        "176" to
                            loadResource("responses/integration/aggregate/$dataFolder/series/series_lookup_result.json")
                                .decodeToString()
                    ),
                    loadResource("responses/integration/aggregate/$dataFolder/series/expected_response.json")
                        .decodeToString(),
                )
            }

            private fun standalone(): ArgumentSet {
                return argumentSet(
                    "standalone event",
                    mapOf(
                        "『在りし日の歌』祭り" to
                            loadResource(
                                    "responses/integration/aggregate/$dataFolder/standalone/event_lookup_result.json"
                                )
                                .decodeToString()
                    ),
                    emptyMap<String, String>(),
                    loadResource("responses/integration/aggregate/$dataFolder/standalone/expected_response.json")
                        .decodeToString(),
                )
            }

            override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
                return Stream.of(standalone(), series())
            }
        }

        abstract class GetVideosByVocaDbTagMappingsTestDataBase(private val dataFolder: String) : ArgumentsProvider {
            protected fun loadTestData(
                caseType: String,
                tagQuery: String,
                tagLookupRequired: Boolean = false,
            ): ArgumentSet {
                val request =
                    loadResource("responses/integration/aggregate/videos_by_tags/$dataFolder/$caseType/request.json")
                        .decodeToString()
                val tagLookupResult =
                    if (tagLookupRequired)
                        mapOf(
                            "シューゲイザー" to
                                loadResource(
                                        "responses/integration/aggregate/videos_by_tags/$dataFolder/$caseType/tag_lookup_result.json"
                                    )
                                    .decodeToString()
                        )
                    else emptyMap()
                val tagMappings =
                    loadResource(
                            "responses/integration/aggregate/videos_by_tags/$dataFolder/$caseType/tag_mappings.json"
                        )
                        .decodeToString()
                val nndVideoSearchResult =
                    loadResource(
                            "responses/integration/aggregate/videos_by_tags/$dataFolder/$caseType/nnd_video_search_result.json"
                        )
                        .decodeToString()
                val dbSongLookupResult =
                    mapOf(
                        // not added, publisher in the db
                        "sm35039682" to "null",
                        // added, published from a channel
                        "sm33123156" to
                            loadResource(
                                    "responses/integration/aggregate/videos_by_tags/$dataFolder/$caseType/song_by_pv_lookup_result_sm33123156.json"
                                )
                                .decodeToString(),
                        // added, published from a user account
                        "sm32396434" to
                            loadResource(
                                    "responses/integration/aggregate/videos_by_tags/$dataFolder/$caseType/song_by_pv_lookup_result_sm32396434.json"
                                )
                                .decodeToString(),
                        // not added, published from a user account
                        "sm33123155" to "null",
                        // not added, published from a channel
                        "sm32396435" to "null",
                    )
                val dbPublisherLookupResultUser =
                    mapOf(
                        "user/87192516" to
                            loadResource(
                                    "responses/integration/aggregate/videos_by_tags/$dataFolder/$caseType/publisher_lookup_result_87192516.json"
                                )
                                .decodeToString(),
                        "user/102050" to
                            loadResource(
                                    "responses/integration/aggregate/videos_by_tags/$dataFolder/$caseType/publisher_lookup_result_empty.json"
                                )
                                .decodeToString(),
                    )
                val dbPublisherLookupResultChannel = mapOf("https://ch.nicovideo.jp/channel/ch5981761" to "[]")
                val thumbnails =
                    listOf("sm32396435", "sm33123155").associateWith {
                        loadResource(
                                "responses/integration/aggregate/videos_by_tags/$dataFolder/$caseType/thumbnail_lookup_result_$it.xml"
                            )
                            .decodeToString()
                    }
                val embeds =
                    mapOf(
                        "sm35039682" to
                            loadResource(
                                    "responses/integration/aggregate/videos_by_tags/$dataFolder/$caseType/embed_sm35039682.html"
                                )
                                .decodeToString()
                    )
                val expectedResponse =
                    loadResource(
                            "responses/integration/aggregate/videos_by_tags/$dataFolder/$caseType/expected_response.json"
                        )
                        .decodeToString()

                return argumentSet(
                    caseType,
                    request,
                    tagLookupResult,
                    tagMappings,
                    tagQuery,
                    nndVideoSearchResult,
                    dbSongLookupResult,
                    dbPublisherLookupResultUser,
                    dbPublisherLookupResultChannel,
                    thumbnails,
                    embeds,
                    expectedResponse,
                )
            }
        }

        class DataByCustomQuerySuccessTestData : ArgumentsProvider {
            private val dataFolder = "responses/integration/aggregate/custom_query"

            private fun artists(): Arguments {
                return arguments(
                    ARTISTS,
                    loadResource("$dataFolder/$ARTISTS/entry_lookup_result.json").decodeToString(),
                    loadResource("$dataFolder/$ARTISTS/expected_response.json").decodeToString(),
                )
            }

            private fun songs(): Arguments {
                return arguments(
                    SONGS,
                    loadResource("$dataFolder/$SONGS/entry_lookup_result.json").decodeToString(),
                    loadResource("$dataFolder/$SONGS/expected_response.json").decodeToString(),
                )
            }

            override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
                return ApiType.entries
                    .map {
                        when (it) {
                            ARTISTS -> artists()
                            SONGS -> songs()
                        }
                    }
                    .stream()
            }
        }

        class ReleaseEventTestData : ReleaseEventTestDataBase("event")

        class ReleaseEventWithLinkedTagTestData : ReleaseEventTestDataBase("event_with_linked_tag")

        class GetVideosByNndTagsTestData : GetVideosByVocaDbTagMappingsTestDataBase("nnd_tags") {
            override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
                return Stream.of(
                    loadTestData("empty_scope", "shoegazer "),
                    loadTestData("non_empty_scope", "shoegazer VOCALOID"),
                )
            }
        }

        class GetVideosByVocaDbTagMappingsTestData : GetVideosByVocaDbTagMappingsTestDataBase("vocadb_tag_mappings") {
            override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
                return Stream.of(
                    loadTestData("empty_scope", "shoegazer OR シューゲイザー ", true),
                    loadTestData("non_empty_scope", "shoegazer OR シューゲイザー VOCALOID", true),
                )
            }
        }

        class GetVideosByEventNndTagsTestData : GetVideosByVocaDbTagMappingsTestDataBase("event_nnd_tags") {
            override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
                return Stream.of(
                    loadTestData("empty_scope", "冬のシューゲイザー祭2017 "),
                    loadTestData("non_empty_scope", "冬のシューゲイザー祭2017 VOCALOID"),
                )
            }
        }

        class GetVideosByEventNndTagsCacheTestData : ArgumentsProvider {
            private val dataFolder = "event_nnd_tags_cache"
            private val tagQuery = "冬のシューゲイザー祭2017 "

            private fun buildArgs(caseType: String, dbSongByPvLookupCount: Int): ArgumentSet {
                val request =
                    loadResource("responses/integration/aggregate/videos_by_tags/$dataFolder/$caseType/request.json")
                        .decodeToString()
                val nndVideoSearchResult =
                    loadResource(
                            "responses/integration/aggregate/videos_by_tags/$dataFolder/$caseType/nnd_video_search_result.json"
                        )
                        .decodeToString()
                val dbSongLookupResult =
                    mapOf(
                        "sm33123155" to
                            loadResource(
                                    "responses/integration/aggregate/videos_by_tags/$dataFolder/$caseType/song_by_pv_lookup_result_sm33123155.json"
                                )
                                .decodeToString()
                    )
                val thumbnails =
                    mapOf(
                        "sm33123155" to
                            loadResource(
                                    "responses/integration/aggregate/videos_by_tags/$dataFolder/$caseType/thumbnail_lookup_result_sm33123155.xml"
                                )
                                .decodeToString()
                    )

                return argumentSet(
                    caseType,
                    request,
                    tagQuery,
                    nndVideoSearchResult,
                    dbSongLookupResult,
                    thumbnails,
                    dbSongByPvLookupCount,
                )
            }

            override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
                return Stream.of(
                    buildArgs("no_song_entry", 2),
                    buildArgs("song_entry_without_events", 2),
                    buildArgs("song_entry_with_non_target_event", 2),
                    buildArgs("song_entry_with_target_event", 1),
                )
            }
        }
    }
}
