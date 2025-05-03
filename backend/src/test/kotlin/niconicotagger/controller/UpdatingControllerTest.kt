package niconicotagger.controller

import com.github.tomakehurst.wiremock.client.WireMock.delete
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.ok
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.put
import com.github.tomakehurst.wiremock.client.WireMock.urlPathTemplate
import jakarta.servlet.http.Cookie
import java.util.stream.Stream
import niconicotagger.Utils.loadResource
import niconicotagger.constants.Constants.COOKIE_HEADER_KEY
import niconicotagger.constants.Constants.DEFAULT_USER_AGENT
import niconicotagger.dto.api.misc.ApiType
import niconicotagger.dto.api.misc.ApiType.SONGS
import niconicotagger.dto.api.misc.ClientType.VOCADB_BETA
import niconicotagger.dto.inner.vocadb.VocaDbTagUsage
import org.instancio.junit.Given
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.ArgumentSet
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.HttpHeaders.USER_AGENT
import org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.test.web.servlet.post

class UpdatingControllerTest : AbstractControllerTest() {

    @Nested
    inner class AddReleaseEvent {
        private val dataFolder = "responses/integration/update/add_release_event"

        @ParameterizedTest
        @ValueSource(strings = ["no_release_events", "has_release_events"])
        fun `add release event test (success)`(case: String, @Given cookie: String) {
            wireMockExtension.stubFor(
                get(urlPathTemplate("/api/songs/{id}/for-edit"))
                    .withPathParam("id", equalTo("743185"))
                    .withHeader(CONTENT_TYPE, equalTo(APPLICATION_JSON_VALUE))
                    .withHeader(USER_AGENT, equalTo(DEFAULT_USER_AGENT))
                    .withCookie(COOKIE_HEADER_KEY, equalTo(cookie))
                    .willReturn(okJson(loadResource("$dataFolder/$case/song_for_edit_response.json").decodeToString()))
            )
            wireMockExtension.stubFor(
                post(urlPathTemplate("/api/songs/{id}"))
                    .withPathParam("id", equalTo("743185"))
                    .withFormParam(
                        "contract",
                        equalToJson(loadResource("$dataFolder/$case/updated_song_data.json").decodeToString()),
                    )
                    .withHeader(CONTENT_TYPE, equalTo(APPLICATION_FORM_URLENCODED_VALUE))
                    .withHeader(USER_AGENT, equalTo(DEFAULT_USER_AGENT))
                    .withCookie(COOKIE_HEADER_KEY, equalTo(cookie))
                    .willReturn(ok())
            )

            mockMvc
                .post("/api/update/songs/add_release_event") {
                    contentType = APPLICATION_JSON
                    content =
                        """
                {
                    "subRequests": [
                        {
                            "entryId": 743185,
                            "event": {
                                "id": 8006,
                                "name": "プロセカNEXT 28"
                            }
                        }
                    ],
                    "clientType": "$VOCADB_BETA"
                }
                """
                            .trimIndent()
                    cookie(Cookie(COOKIE_HEADER_KEY, cookie))
                }
                .asyncDispatch()
                .andExpect { status { isOk() } }
        }

        @Test
        fun `add release event test (invalid request)`(@Given cookie: String) {
            testInvalidRequest(
                """
                {
                    "subRequests": [
                        {
                            "entryId": 743185,
                            "event": {
                                "id": 8006,
                                "name": " "
                            }
                        }
                    ],
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
                      "field": "subRequests[0].event.name",
                      "message": "must not be blank"
                    }
                  ],
                  "title": "Constraint Violation"
                }
                """
                    .trimIndent(),
                "/update/songs/add_release_event",
                Cookie(COOKIE_HEADER_KEY, cookie),
            )
        }
    }

    @Nested
    inner class DeleteTagsTest {
        @ParameterizedTest
        @EnumSource(ApiType::class)
        fun `delete tags test`(
            apiType: ApiType,
            @Given entryId: Long,
            @Given tagUsage1: VocaDbTagUsage,
            @Given tagUsage2: VocaDbTagUsage,
            @Given cookie: String,
        ) {
            wireMockExtension.stubFor(
                get(urlPathTemplate("/api/{apiType}/{id}/tagUsages"))
                    .withPathParam("apiType", equalTo(apiType.toString()))
                    .withPathParam("id", equalTo(entryId.toString()))
                    .withCookie(COOKIE_HEADER_KEY, equalTo(cookie))
                    .withHeader(CONTENT_TYPE, equalTo(APPLICATION_JSON_VALUE))
                    .withHeader(USER_AGENT, equalTo(DEFAULT_USER_AGENT))
                    .willReturn(
                        okJson(
                            """
                        {
                          "id": $entryId,
                          "canRemoveTagUsages": true,
                          "tagUsages": [
                            {
                              "id": ${tagUsage1.id},
                              "tag": {
                                "id": ${tagUsage1.tag.id},
                                "name": "${tagUsage1.tag.name}"
                              }
                            },
                            {
                              "id": ${tagUsage2.id},
                              "tag": {
                                "id": ${tagUsage2.tag.id},
                                "name": "${tagUsage2.tag.name}"
                              }
                            }
                          ]
                        }
                        """
                                .trimIndent()
                        )
                    )
            )
            listOf(tagUsage1, tagUsage2).forEach {
                wireMockExtension.stubFor(
                    delete(urlPathTemplate("/api/users/current/{tagType}/{tagUsageId}"))
                        .withPathParam("tagType", equalTo(apiType.tagType))
                        .withPathParam("tagUsageId", equalTo(it.id.toString()))
                        .withHeader(CONTENT_TYPE, equalTo(APPLICATION_JSON_VALUE))
                        .withHeader(USER_AGENT, equalTo(DEFAULT_USER_AGENT))
                        .withCookie(COOKIE_HEADER_KEY, equalTo(cookie))
                        .willReturn(ok().withStatus(204))
                )
            }

            mockMvc
                .post("/api/update/tags/delete") {
                    contentType = APPLICATION_JSON
                    content =
                        """
                {
                    "subRequests": [
                        {
                            "apiType": "$apiType",
                            "entryId": $entryId,
                            "tags": [
                                {
                                    "id": ${tagUsage1.tag.id},
                                    "name": "${tagUsage1.tag.name}"
                                },
                                {
                                    "id": ${tagUsage2.tag.id},
                                    "name": "${tagUsage2.tag.name}"
                                }
                            ]
                        }
                    ],
                    "clientType": "$VOCADB_BETA"
                }
                """
                            .trimIndent()
                    cookie(Cookie(COOKIE_HEADER_KEY, cookie))
                }
                .asyncDispatch()
                .andExpect { status { isOk() } }
        }

        @ParameterizedTest
        @ArgumentsSource(DeleteTagsInvalidRequestData::class)
        fun `delete tags test (invalid request)`(request: String, expectedMessage: String, @Given cookie: String) {
            testInvalidRequest(request, expectedMessage, "/update/tags/delete", Cookie(COOKIE_HEADER_KEY, cookie))
        }
    }

    @Nested
    inner class ReplaceNndTagWithEventTest {
        private val dataFolder = "responses/integration/update/replace_tag_with_release_event"

        @ParameterizedTest
        @ValueSource(strings = ["no_release_events", "has_release_events"])
        fun `replace tag with event test (success)`(case: String, @Given cookie: String) {
            wireMockExtension.stubFor(
                get(urlPathTemplate("/api/{apiType}/{id}/tagUsages"))
                    .withPathParam("apiType", equalTo(SONGS.toString()))
                    .withPathParam("id", equalTo("743185"))
                    .withHeader(CONTENT_TYPE, equalTo(APPLICATION_JSON_VALUE))
                    .withHeader(USER_AGENT, equalTo(DEFAULT_USER_AGENT))
                    .withCookie(COOKIE_HEADER_KEY, equalTo(cookie))
                    .willReturn(okJson(loadResource("$dataFolder/$case/tag_usages_response.json").decodeToString()))
            )
            wireMockExtension.stubFor(
                get(urlPathTemplate("/api/songs/{id}/for-edit"))
                    .withPathParam("id", equalTo("743185"))
                    .withHeader(CONTENT_TYPE, equalTo(APPLICATION_JSON_VALUE))
                    .withHeader(USER_AGENT, equalTo(DEFAULT_USER_AGENT))
                    .withCookie(COOKIE_HEADER_KEY, equalTo(cookie))
                    .willReturn(okJson(loadResource("$dataFolder/$case/song_for_edit_response.json").decodeToString()))
            )
            wireMockExtension.stubFor(
                post(urlPathTemplate("/api/songs/{id}"))
                    .withPathParam("id", equalTo("743185"))
                    .withFormParam(
                        "contract",
                        equalToJson(loadResource("$dataFolder/$case/updated_song_data.json").decodeToString()),
                    )
                    .withHeader(CONTENT_TYPE, equalTo(APPLICATION_FORM_URLENCODED_VALUE))
                    .withHeader(USER_AGENT, equalTo(DEFAULT_USER_AGENT))
                    .withCookie(COOKIE_HEADER_KEY, equalTo(cookie))
                    .willReturn(ok())
            )
            wireMockExtension.stubFor(
                delete(urlPathTemplate("/api/users/current/{tagType}/{tagUsageId}"))
                    .withPathParam("tagType", equalTo(SONGS.tagType))
                    .withPathParam("tagUsageId", equalTo("1747034"))
                    .withHeader(CONTENT_TYPE, equalTo(APPLICATION_JSON_VALUE))
                    .withHeader(USER_AGENT, equalTo(DEFAULT_USER_AGENT))
                    .withCookie(COOKIE_HEADER_KEY, equalTo(cookie))
                    .willReturn(ok())
            )

            mockMvc
                .post("/api/update/songs/replace_tag_with_event") {
                    contentType = APPLICATION_JSON
                    content =
                        """
                {
                    "subRequests": [
                        {
                            "entryId": 743185,
                            "event": {
                                "id": 8006,
                                "name": "プロセカNEXT 28"
                            },
                            "tags": [
                                {
                                    "id": 7323,
                                    "name": "プロセカ"
                                }
                            ]
                        }
                    ],
                    "clientType": "$VOCADB_BETA"
                }
                """
                            .trimIndent()
                    cookie(Cookie(COOKIE_HEADER_KEY, cookie))
                }
                .asyncDispatch()
                .andExpect { status { isOk() } }
        }

        @ParameterizedTest
        @ArgumentsSource(ReplaceTagWithEventInvalidRequestData::class)
        fun `replace tag with event test (invalid request)`(
            request: String,
            expectedMessage: String,
            @Given cookie: String,
        ) {
            testInvalidRequest(
                request,
                expectedMessage,
                "/update/songs/replace_tag_with_event",
                Cookie(COOKIE_HEADER_KEY, cookie),
            )
        }
    }

    @Nested
    inner class UpdateSongTest {
        private val dataFolder = "responses/integration/update/update_song"

        @Test
        fun `update song test (success)`(@Given cookie: String) {
            wireMockExtension.stubFor(
                get(urlPathTemplate("/api/users/current/songTags/{id}"))
                    .withPathParam("id", equalTo("657775"))
                    .withHeader(CONTENT_TYPE, equalTo(APPLICATION_JSON_VALUE))
                    .withHeader(USER_AGENT, equalTo(DEFAULT_USER_AGENT))
                    .withCookie(COOKIE_HEADER_KEY, equalTo(cookie))
                    .willReturn(
                        okJson(loadResource("$dataFolder/tag_selection_by_current_user_response.json").decodeToString())
                    )
            )
            wireMockExtension.stubFor(
                put(urlPathTemplate("/api/users/current/songTags/{id}"))
                    .withPathParam("id", equalTo("657775"))
                    .withHeader(CONTENT_TYPE, equalTo(APPLICATION_JSON_VALUE))
                    .withHeader(USER_AGENT, equalTo(DEFAULT_USER_AGENT))
                    .withCookie(COOKIE_HEADER_KEY, equalTo(cookie))
                    .withRequestBody(
                        equalToJson(
                            loadResource("$dataFolder/tag_update_request_body.json").decodeToString(),
                            true,
                            false,
                        )
                    )
                    .willReturn(
                        okJson(loadResource("$dataFolder/tag_selection_by_current_user_response.json").decodeToString())
                    )
            )
            wireMockExtension.stubFor(
                get(urlPathTemplate("/api/songs/{id}/for-edit"))
                    .withPathParam("id", equalTo("657775"))
                    .withHeader(CONTENT_TYPE, equalTo(APPLICATION_JSON_VALUE))
                    .withHeader(USER_AGENT, equalTo(DEFAULT_USER_AGENT))
                    .withCookie(COOKIE_HEADER_KEY, equalTo(cookie))
                    .willReturn(okJson(loadResource("$dataFolder/song_for_edit_response.json").decodeToString()))
            )
            wireMockExtension.stubFor(
                post(urlPathTemplate("/api/songs/{id}"))
                    .withPathParam("id", equalTo("657775"))
                    .withFormParam(
                        "contract",
                        equalToJson(loadResource("$dataFolder/updated_song_data.json").decodeToString()),
                    )
                    .withHeader(CONTENT_TYPE, equalTo(APPLICATION_FORM_URLENCODED_VALUE))
                    .withHeader(USER_AGENT, equalTo(DEFAULT_USER_AGENT))
                    .withCookie(COOKIE_HEADER_KEY, equalTo(cookie))
                    .willReturn(ok())
            )

            mockMvc
                .post("/api/update/songs/update_tags_and_pvs") {
                    contentType = APPLICATION_JSON
                    content =
                        """
                {
                    "subRequests": [
                        {
                            "songId": 657775,
                            "tags": [
                                4582
                            ],
                            "nndPvsToDisable": [
                                {
                                    "id": "sm43909677",
                                    "reason": "DELETED"
                                },
                                {
                                    "id": "sm44527560",
                                    "reason": "NOT_FOUND"
                                },
                                {
                                    "id": "sm00000000",
                                    "reason": "???"
                                }
                            ]
                        }
                    ],
                    "clientType": "$VOCADB_BETA"
                }
                """
                            .trimIndent()
                    cookie(Cookie(COOKIE_HEADER_KEY, cookie))
                }
                .asyncDispatch()
                .andExpect { status { isOk() } }
        }

        @Test
        fun `update song test (invalid request)`(@Given cookie: String) {
            testInvalidRequest(
                """
                {
                    "subRequests": [
                        {
                            "songId": 657775,
                            "tags": [
                                4582
                            ],
                            "nndPvsToDisable": [
                                {
                                    "id": " ",
                                    "reason": " "
                                }
                            ]
                        }
                    ],
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
                      "field": "subRequests[0].nndPvsToDisable[].id",
                      "message": "must not be blank"
                    },
                    {
                      "field": "subRequests[0].nndPvsToDisable[].reason",
                      "message": "must not be blank"
                    }
                  ],
                  "title": "Constraint Violation"
                }
                """
                    .trimIndent(),
                "/update/songs/update_tags_and_pvs",
                Cookie(COOKIE_HEADER_KEY, cookie),
            )
        }
    }

    companion object {
        class DeleteTagsInvalidRequestData : ArgumentsProvider {
            private fun buildArgs(apiType: ApiType): Stream<ArgumentSet> {
                return Stream.of(
                    argumentSet(
                        "apiType=$apiType, no tags specified",
                        """
                        {
                            "subRequests": [
                                {
                                    "apiType": "$apiType",
                                    "entryId": 1,
                                    "tags": []
                                }
                            ],
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
                              "field": "subRequests[0].tags",
                              "message": "must not be empty"
                            }
                          ],
                          "title": "Constraint Violation"
                        }
                        """
                            .trimIndent(),
                    ),
                    argumentSet(
                        "apiType=$apiType, empty tag name",
                        """
                        {
                            "subRequests": [
                                {
                                    "apiType": "$apiType",
                                    "entryId": 1,
                                    "tags": [
                                        {
                                            "id": 1,
                                            "name": " "
                                        }
                                    ]
                                }
                            ],
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
                              "field": "subRequests[0].tags[0].name",
                              "message": "must not be blank"
                            }
                          ],
                          "title": "Constraint Violation"
                        }
                        """
                            .trimIndent(),
                    ),
                )
            }

            override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
                return ApiType.entries
                    .map { buildArgs(it) }
                    .fold(Stream.empty()) { acc, stream -> Stream.concat(acc, stream) }
            }
        }

        class ReplaceTagWithEventInvalidRequestData : ArgumentsProvider {
            override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
                return Stream.of(
                    argumentSet(
                        "empty event name, no tags specified",
                        """
                        {
                            "subRequests": [
                                {
                                    "entryId": 0,
                                    "event": {
                                        "id": 0,
                                        "name": " "
                                    },
                                    "tags": []
                                }
                            ],
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
                              "field": "subRequests[0].event.name",
                              "message": "must not be blank"
                            },
                            {
                              "field": "subRequests[0].tags",
                              "message": "size must be between 1 and 1"
                            }
                          ],
                          "title": "Constraint Violation"
                        }
                        """
                            .trimIndent(),
                    ),
                    argumentSet(
                        "empty tag name",
                        """
                        {
                            "subRequests": [
                                {
                                    "entryId": 1,
                                    "event": {
                                        "id": 0,
                                        "name": "event"
                                    },
                                    "tags": [
                                        {
                                            "id": 1,
                                            "name": " "
                                        }
                                    ]
                                }
                            ],
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
                              "field": "subRequests[0].tags[0].name",
                              "message": "must not be blank"
                            }
                          ],
                          "title": "Constraint Violation"
                        }
                        """
                            .trimIndent(),
                    ),
                    argumentSet(
                        "too many tags specified",
                        """
                        {
                            "subRequests": [
                                {
                                    "entryId": 1,
                                    "event": {
                                        "id": 0,
                                        "name": "event"
                                    },
                                    "tags": [
                                        {
                                            "id": 1,
                                            "name": "name1"
                                        },
                                        {
                                            "id": 2,
                                            "name": "name2"
                                        }
                                    ]
                                }
                            ],
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
                              "field": "subRequests[0].tags",
                              "message": "size must be between 1 and 1"
                            }
                          ],
                          "title": "Constraint Violation"
                        }
                        """
                            .trimIndent(),
                    ),
                )
            }
        }
    }
}
