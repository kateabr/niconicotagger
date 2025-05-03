package niconicotagger.controller

import com.github.tomakehurst.wiremock.client.WireMock.badRequest
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.ok
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.serviceUnavailable
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import kotlinx.coroutines.runBlocking
import niconicotagger.constants.Constants.COOKIE_HEADER_KEY
import niconicotagger.constants.Constants.DEFAULT_USER_AGENT
import niconicotagger.dto.api.misc.ClientType.VOCADB_BETA
import org.instancio.junit.Given
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.FieldSource
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.HttpHeaders.SET_COOKIE
import org.springframework.http.HttpHeaders.USER_AGENT
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.test.json.JsonCompareMode.STRICT
import org.springframework.test.web.servlet.post

class AuthorizationControllerTest : AbstractControllerTest() {

    @Test
    fun `login test (success)`(@Given userName: String, @Given password: String, @Given cookie: String): Unit =
        runBlocking {
            wireMockExtension.stubFor(
                post(urlPathEqualTo("/api/users/login"))
                    .withRequestBody(
                        equalToJson(
                            """
                    {
                      "userName": "$userName",
                      "password": "$password",
                      "keepLoggedIn": true
                    }
                    """
                                .trimIndent()
                        )
                    )
                    .withHeader(CONTENT_TYPE, equalTo(APPLICATION_JSON_VALUE))
                    .withHeader(USER_AGENT, equalTo(DEFAULT_USER_AGENT))
                    .willReturn(ok().withHeader(SET_COOKIE, "$COOKIE_HEADER_KEY=$cookie"))
            )
            wireMockExtension.stubFor(
                get("/api/users/current")
                    .withCookie(COOKIE_HEADER_KEY, equalTo(cookie))
                    .withHeader(USER_AGENT, equalTo(DEFAULT_USER_AGENT))
                    .willReturn(
                        okJson(
                            """
                    {
                      "active": true,
                      "groupId": "Trusted"
                    }
                """
                                .trimIndent()
                        )
                    )
            )

            mockMvc
                .post("/api/authorize") {
                    contentType = APPLICATION_JSON
                    content =
                        """
                {
                  "userName": "$userName",
                  "password": "$password",
                  "clientType": "$VOCADB_BETA"
                }
                 """
                            .trimIndent()
                }
                .asyncDispatch()
                .andExpect {
                    status { isOk() }
                    cookie { value(COOKIE_HEADER_KEY, cookie) }
                }
        }

    @Test
    fun `login test (error, db unavailable)`(
        @Given userName: String,
        @Given password: String,
        @Given cookie: String,
    ): Unit = runBlocking {
        wireMockExtension.stubFor(
            post(urlPathEqualTo("/api/users/login"))
                .withRequestBody(
                    equalToJson(
                        """
                    {
                      "userName": "$userName",
                      "password": "$password",
                      "keepLoggedIn": true
                    }
                    """
                            .trimIndent()
                    )
                )
                .withHeader(CONTENT_TYPE, equalTo(APPLICATION_JSON_VALUE))
                .withHeader(USER_AGENT, equalTo(DEFAULT_USER_AGENT))
                .willReturn(serviceUnavailable())
        )

        mockMvc
            .post("/api/authorize") {
                contentType = APPLICATION_JSON
                content =
                    """
                {
                  "userName": "$userName",
                  "password": "$password",
                  "clientType": "$VOCADB_BETA"
                }
                 """
                        .trimIndent()
            }
            .asyncDispatch()
            .andExpect {
                status { isServiceUnavailable() }
                content {
                    json(
                        """
                        {
                          "title": "Service Unavailable",
                          "status": 503,
                          "detail": "503 Service Unavailable from POST ${wireMockExtension.runtimeInfo.httpBaseUrl}/api/users/login"
                        }
                        """
                            .trimIndent(),
                        STRICT,
                    )
                }
                cookie { doesNotExist(COOKIE_HEADER_KEY) }
            }
    }

    @Test
    fun `login test (error, incorrect user credentials)`(
        @Given userName: String,
        @Given password: String,
        @Given cookie: String,
    ): Unit = runBlocking {
        wireMockExtension.stubFor(
            post(urlPathEqualTo("/api/users/login"))
                .withRequestBody(
                    equalToJson(
                        """
                    {
                      "userName": "$userName",
                      "password": "$password",
                      "keepLoggedIn": true
                    }
                    """
                            .trimIndent()
                    )
                )
                .withHeader(CONTENT_TYPE, equalTo(APPLICATION_JSON_VALUE))
                .withHeader(USER_AGENT, equalTo(DEFAULT_USER_AGENT))
                .willReturn(
                    badRequest()
                        .withBody(
                            """
                            {
                              "errors": {
                                "": [
                                  "Username or password doesn't match"
                                ]
                              },
                              "type": "https://tools.ietf.org/html/rfc7231#section-6.5.1",
                              "title": "One or more validation errors occurred.",
                              "status": 400,
                              "traceId": "00-a63eda555adb2184bc5299a123de32d1-039f365b64f95314-00"
                            }
                            """
                                .trimIndent()
                        )
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                )
        )

        mockMvc
            .post("/api/authorize") {
                contentType = APPLICATION_JSON
                content =
                    """
                {
                  "userName": "$userName",
                  "password": "$password",
                  "clientType": "$VOCADB_BETA"
                }
                 """
                        .trimIndent()
            }
            .asyncDispatch()
            .andExpect {
                status { isBadRequest() }
                content {
                    json(
                        """
                        {
                          "title": "Bad Request",
                          "status": 400,
                          "detail": "Username or password doesn't match"
                        }
                        """
                            .trimIndent(),
                        STRICT,
                    )
                }
                cookie { doesNotExist(COOKIE_HEADER_KEY) }
            }
    }

    @ParameterizedTest
    @FieldSource("errorTestData")
    fun `login test (invalid request)`(requestJson: String, responseJson: String): Unit = runBlocking {
        mockMvc
            .post("/api/authorize") {
                contentType = APPLICATION_JSON
                content = requestJson
            }
            .andExpect {
                status { is4xxClientError() }
                content { json(responseJson, STRICT) }
            }
    }

    companion object {
        val errorTestData =
            listOf(
                argumentSet(
                    "empty request",
                    "",
                    """
                {
                  "title": "Bad Request",
                  "status": 400,
                  "detail": "Required request body is missing: public java.lang.Object niconicotagger.controller.AuthorizationController.login(niconicotagger.dto.api.request.LoginRequest,jakarta.servlet.http.HttpServletResponse,kotlin.coroutines.Continuation<? super kotlin.Unit>)"
                }"""
                        .trimIndent(),
                ),
                argumentSet(
                    "empty user data",
                    """
                {
                  "userName": "",
                  "password": "",
                  "clientType": "vocadb_beta"
                }
                 """
                        .trimIndent(),
                    """
                {
                  "type": "https://zalando.github.io/problem/constraint-violation",
                  "status": 400,
                  "violations": [
                    {
                      "field": "password",
                      "message": "must not be blank"
                    },
                    {
                      "field": "userName",
                      "message": "must not be blank"
                    }
                  ],
                  "title": "Constraint Violation"
                }"""
                        .trimIndent(),
                ),
            )
    }
}
