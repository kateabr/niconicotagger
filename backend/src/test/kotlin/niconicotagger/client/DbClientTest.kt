package niconicotagger.client

import com.github.tomakehurst.wiremock.client.WireMock.delete
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.equalToDateTime
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.ok
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathTemplate
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo
import com.github.tomakehurst.wiremock.junit5.WireMockTest
import java.net.URI
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import kotlinx.coroutines.runBlocking
import niconicotagger.Utils.jsonMapper
import niconicotagger.Utils.loadResource
import niconicotagger.constants.Constants.COOKIE_HEADER_KEY
import niconicotagger.constants.Constants.DEFAULT_USER_AGENT
import niconicotagger.dto.api.misc.ApiType
import niconicotagger.dto.api.misc.ApiType.ARTISTS
import niconicotagger.dto.api.misc.ApiType.SONGS
import niconicotagger.dto.api.misc.VocaDbSortOrder
import niconicotagger.dto.inner.misc.ArtistRole.Arranger
import niconicotagger.dto.inner.misc.ArtistRole.Composer
import niconicotagger.dto.inner.misc.ArtistRole.Default
import niconicotagger.dto.inner.misc.ArtistType
import niconicotagger.dto.inner.misc.ArtistType.Circle
import niconicotagger.dto.inner.misc.ArtistType.Producer
import niconicotagger.dto.inner.misc.ArtistType.SynthesizerV
import niconicotagger.dto.inner.misc.EntryField.Artists
import niconicotagger.dto.inner.misc.EntryField.Tags
import niconicotagger.dto.inner.misc.EntryField.WebLinks
import niconicotagger.dto.inner.misc.ReleaseEventCategory.Anniversary
import niconicotagger.dto.inner.misc.ReleaseEventCategory.Club
import niconicotagger.dto.inner.misc.ReleaseEventCategory.Festival
import niconicotagger.dto.inner.misc.ReleaseEventCategory.Unspecified
import niconicotagger.dto.inner.misc.SongType.Cover
import niconicotagger.dto.inner.misc.SongType.Original
import niconicotagger.dto.inner.misc.SongType.Remaster
import niconicotagger.dto.inner.misc.UserGroup
import niconicotagger.dto.inner.misc.UserGroup.Admin
import niconicotagger.dto.inner.misc.UserGroup.Moderator
import niconicotagger.dto.inner.misc.UserGroup.Trusted
import niconicotagger.dto.inner.misc.WebLink
import niconicotagger.dto.inner.vocadb.MainPicture
import niconicotagger.dto.inner.vocadb.VocaDbArtist
import niconicotagger.dto.inner.vocadb.VocaDbArtistEntryData
import niconicotagger.dto.inner.vocadb.VocaDbCustomQueryArtistData
import niconicotagger.dto.inner.vocadb.VocaDbCustomQueryData
import niconicotagger.dto.inner.vocadb.VocaDbCustomQuerySongData
import niconicotagger.dto.inner.vocadb.VocaDbEntryArtist
import niconicotagger.dto.inner.vocadb.VocaDbFrontPageData
import niconicotagger.dto.inner.vocadb.VocaDbReleaseEvent
import niconicotagger.dto.inner.vocadb.VocaDbReleaseEventSeries
import niconicotagger.dto.inner.vocadb.VocaDbSongEntryWithNndPvsTagsAndReleaseEvents
import niconicotagger.dto.inner.vocadb.VocaDbSongEntryWithTags
import niconicotagger.dto.inner.vocadb.VocaDbTag
import niconicotagger.dto.inner.vocadb.VocaDbTagMapping
import niconicotagger.dto.inner.vocadb.VocaDbTagSelectable
import niconicotagger.dto.inner.vocadb.VocaDbTagUsage
import niconicotagger.dto.inner.vocadb.VocaDbTagUsages
import niconicotagger.dto.inner.vocadb.VocaDbUser
import niconicotagger.dto.inner.vocadb.search.result.VocaDbCustomQuerySearchResult
import niconicotagger.dto.inner.vocadb.search.result.VocaDbSongEntryWithNndPvsTagsAndReleaseEventsSearchResult
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.assertj.core.api.Assertions.assertThatNoException
import org.assertj.core.api.InstanceOfAssertFactories.list
import org.assertj.core.api.InstanceOfAssertFactories.map
import org.instancio.Instancio
import org.instancio.Select.field
import org.instancio.junit.Given
import org.instancio.junit.InstancioExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE
import org.junit.jupiter.params.provider.FieldSource
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.HttpHeaders.SET_COOKIE
import org.springframework.http.HttpHeaders.USER_AGENT
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE

@WireMockTest
@ExtendWith(InstancioExtension::class)
class DbClientTest {

    @ParameterizedTest
    @EnumSource(UserGroup::class, names = ["Nothing", "Limited", "Regular"], mode = EXCLUDE)
    fun `login test (success)`(
        groupId: UserGroup,
        wm: WireMockRuntimeInfo,
        @Given username: String,
        @Given password: String,
        @Given cookieValue: String,
    ): Unit = runBlocking {
        stubFor(
            post(urlPathEqualTo("/api/users/login"))
                .withRequestBody(
                    equalToJson(
                        """
                {
                  "keepLoggedIn": true,
                  "userName": "$username",
                  "password": "$password"
                }
            """
                            .trimIndent()
                    )
                )
                .withHeader(CONTENT_TYPE, equalTo(APPLICATION_JSON_VALUE))
                .withHeader(USER_AGENT, equalTo(DEFAULT_USER_AGENT))
                .willReturn(ok().withHeader(SET_COOKIE, "$COOKIE_HEADER_KEY=$cookieValue"))
        )
        stubFor(
            get(urlPathEqualTo("/api/users/current"))
                .withHeader(USER_AGENT, equalTo(DEFAULT_USER_AGENT))
                .withCookie(COOKIE_HEADER_KEY, equalTo(cookieValue))
                .willReturn(
                    okJson(
                        """
                        {
                            "active": true,
                            "groupId": "$groupId"
                        }
                        """
                            .trimIndent()
                    )
                )
        )

        assertThat(DbClient(wm.httpBaseUrl, jsonMapper).login(username, password))
            .hasSize(1)
            .extractingByKey(COOKIE_HEADER_KEY)
            .asInstanceOf(list(String::class.java))
            .hasSize(1)
            .first()
            .isEqualTo(cookieValue)
    }

    @ParameterizedTest
    @FieldSource("loginFailureTestData")
    fun `login test (failure)`(
        user: VocaDbUser,
        wm: WireMockRuntimeInfo,
        @Given username: String,
        @Given password: String,
        @Given cookieValue: String,
    ) {
        stubFor(
            post(urlPathEqualTo("/api/users/login"))
                .withRequestBody(
                    equalToJson(
                        """
                {
                  "keepLoggedIn": true,
                  "userName": "$username",
                  "password": "$password"
                }
            """
                            .trimIndent()
                    )
                )
                .withHeader(CONTENT_TYPE, equalTo(APPLICATION_JSON_VALUE))
                .withHeader(USER_AGENT, equalTo(DEFAULT_USER_AGENT))
                .willReturn(ok().withHeader(SET_COOKIE, "$COOKIE_HEADER_KEY=$cookieValue"))
        )
        stubFor(
            get(urlPathEqualTo("/api/users/current"))
                .withHeader(USER_AGENT, equalTo(DEFAULT_USER_AGENT))
                .willReturn(
                    okJson(
                        """
                        {
                            "active": ${user.active},
                            "groupId": "${user.groupId}"
                        }
                        """
                            .trimIndent()
                    )
                )
        )

        assertThatExceptionOfType(IllegalStateException::class.java).isThrownBy {
            runBlocking { DbClient(wm.httpBaseUrl, jsonMapper).login(username, password) }
        }
    }

    @Test
    fun `get tag by name test (success)`(wm: WireMockRuntimeInfo): Unit = runBlocking {
        stubFor(
            get(urlPathEqualTo("/api/tags"))
                .withQueryParams(mapOf("query" to equalTo("shoegaze"), "maxResults" to equalTo("1")))
                .withHeader(USER_AGENT, equalTo(DEFAULT_USER_AGENT))
                .willReturn(
                    ok()
                        .withBody(loadResource("responses/vocadb/tag_lookup_result.json"))
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                )
        )

        assertThat(DbClient(wm.httpBaseUrl, jsonMapper).getTagByName("shoegaze"))
            .usingRecursiveComparison()
            .isEqualTo(VocaDbTag(395, "shoegaze"))
    }

    @Test
    fun `get tag by name test (too many tags)`(wm: WireMockRuntimeInfo) {
        stubFor(
            get(urlPathEqualTo("/api/tags"))
                .withQueryParams(mapOf("query" to equalTo("shoegaze"), "maxResults" to equalTo("1")))
                .withHeader(USER_AGENT, equalTo(DEFAULT_USER_AGENT))
                .willReturn(
                    ok()
                        .withBody(loadResource("responses/vocadb/tag_lookup_result_too_many_suggestions.json"))
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                )
        )

        assertThatExceptionOfType(IllegalStateException::class.java).isThrownBy {
            runBlocking { DbClient(wm.httpBaseUrl, jsonMapper).getTagByName("shoegaze") }
        }
    }

    @Test
    fun `get song for edit test`(wm: WireMockRuntimeInfo, @Given songId: Long, @Given cookie: String): Unit =
        runBlocking {
            stubFor(
                get(urlPathTemplate("/api/songs/{id}/for-edit"))
                    .withPathParam("id", equalTo(songId.toString()))
                    .withCookie(COOKIE_HEADER_KEY, equalTo(cookie))
                    .withHeader(USER_AGENT, equalTo(DEFAULT_USER_AGENT))
                    .willReturn(
                        ok()
                            .withBody(loadResource("responses/vocadb/song_for_edit.json"))
                            .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                    )
            )

            assertThat(DbClient(wm.httpBaseUrl, jsonMapper).getSongForEdit(songId, cookie))
                .asInstanceOf(map(String::class.java, Any::class.java))
                .extractingByKey("id")
                .isEqualTo(657_775)
        }

    @ParameterizedTest
    @ValueSource(longs = [63L])
    @NullSource
    fun `get event test`(seriesId: Long?, wm: WireMockRuntimeInfo): Unit = runBlocking {
        stubFor(
            get(urlPathEqualTo("/api/releaseEvents"))
                .withQueryParams(
                    mapOf(
                        "query" to equalTo("重音テト誕生祭 2023"),
                        "lang" to equalTo("Default"),
                        "fields" to equalTo("WebLinks,Tags"),
                        "nameMatchMode" to equalTo("Exact"),
                        "maxResults" to equalTo("1"),
                        "getTotalCount" to equalTo("true"),
                    )
                )
                .withHeader(USER_AGENT, equalTo(DEFAULT_USER_AGENT))
                .willReturn(
                    ok()
                        .withBody(
                            if (seriesId == null) loadResource("responses/vocadb/event_lookup_result.json")
                            else loadResource("responses/vocadb/event_lookup_result_with_series.json")
                        )
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                )
        )

        assertThat(DbClient(wm.httpBaseUrl, jsonMapper).getEventByName("重音テト誕生祭 2023", WebLinks, Tags))
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .isEqualTo(
                VocaDbReleaseEvent(
                    3508,
                    OffsetDateTime.parse("2023-04-01T00:00:00Z").toInstant(),
                    null,
                    "重音テト誕生祭 2023",
                    Unspecified,
                    listOf(
                        WebLink(
                            "http://www.nicovideo.jp/tag/%E9%87%8D%E9%9F%B3%E3%83%86%E3%83%88%E8%AA%95%E7%94%9F%E7%A5%AD2023"
                        ),
                        WebLink("https://blog.nicovideo.jp/niconews/190156.html"),
                    ),
                    listOf(VocaDbTag(2987, "birthday"), VocaDbTag(9325, "15th birthday")),
                    seriesId,
                    null,
                    null,
                )
            )
    }

    @Test
    fun `get event series test`(wm: WireMockRuntimeInfo): Unit = runBlocking {
        stubFor(
            get(urlPathTemplate("/api/releaseEventSeries/{id}"))
                .withPathParam("id", equalTo("63"))
                .withQueryParams(mapOf("fields" to equalTo("None")))
                .withHeader(USER_AGENT, equalTo(DEFAULT_USER_AGENT))
                .willReturn(
                    ok()
                        .withBody(loadResource("responses/vocadb/event_series_lookup_result.json"))
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                )
        )

        assertThat(DbClient(wm.httpBaseUrl, jsonMapper).getEventSeriesById(63))
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .isEqualTo(VocaDbReleaseEventSeries(63, Anniversary, emptyList()))
    }

    @Test
    fun `get all VocaDB mappings for NND tags test`(wm: WireMockRuntimeInfo): Unit = runBlocking {
        stubFor(
            get(urlPathEqualTo("/api/tags/mappings"))
                .withQueryParams(
                    mapOf("start" to equalTo("0"), "maxEntries" to equalTo("10000"), "getTotalCount" to equalTo("true"))
                )
                .withHeader(USER_AGENT, equalTo(DEFAULT_USER_AGENT))
                .willReturn(
                    ok()
                        .withBody(loadResource("responses/vocadb/tag_mappings_lookup_result.json"))
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                )
        )

        assertThat(DbClient(wm.httpBaseUrl, jsonMapper).getAllVocaDbTagMappings(false))
            .containsExactlyInAnyOrderElementsOf(
                listOf(
                    VocaDbTagMapping("トリップミック", VocaDbTag(2956, "trip hop")),
                    VocaDbTagMapping("triphop", VocaDbTag(2956, "trip hop")),
                    VocaDbTagMapping("三拍子", VocaDbTag(8224, "triple metre")),
                    VocaDbTagMapping("3拍子", VocaDbTag(8224, "triple metre")),
                    VocaDbTagMapping("Tropical_House", VocaDbTag(1582, "tropical house")),
                    VocaDbTagMapping("トロピカルハウス", VocaDbTag(1582, "tropical house")),
                )
            )
    }

    @Test
    fun `get song by NND video id`(wm: WireMockRuntimeInfo): Unit = runBlocking {
        stubFor(
            get(urlPathEqualTo("/api/songs/byPv"))
                .withQueryParams(
                    mapOf(
                        "pvId" to equalTo("sm42029727"),
                        "pvService" to equalTo("NicoNicoDouga"),
                        "fields" to equalTo("Tags,Artists"),
                    )
                )
                .withHeader(USER_AGENT, equalTo(DEFAULT_USER_AGENT))
                .willReturn(
                    ok()
                        .withBody(loadResource("responses/vocadb/song_by_nnd_pv_lookup_result.json"))
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                )
        )

        assertThat(
                DbClient(wm.httpBaseUrl, jsonMapper)
                    .getSongByNndPv(VocaDbSongEntryWithTags::class.java, "sm42029727", Tags, Artists)
            )
            .usingRecursiveComparison()
            .isEqualTo(
                VocaDbSongEntryWithTags(
                    489_536,
                    "Alive",
                    Remaster,
                    listOf(
                        VocaDbEntryArtist(
                            false,
                            VocaDbArtistEntryData(118_397, "重音テトSV", SynthesizerV),
                            listOf(Default),
                        ),
                        VocaDbEntryArtist(
                            false,
                            VocaDbArtistEntryData(1_053, "デスおはぎ", Producer),
                            listOf(Composer, Arranger),
                        ),
                        VocaDbEntryArtist(
                            false,
                            VocaDbArtistEntryData(118_399, "坂内 若", ArtistType.Illustrator),
                            listOf(Default),
                        ),
                        VocaDbEntryArtist(false, VocaDbArtistEntryData(119_530, "ツインドリル", Circle), listOf(Default)),
                    ),
                    "デスおはぎ, ツインドリル feat. 重音テトSV",
                    OffsetDateTime.parse("2023-04-03T00:00:00Z").toInstant(),
                    listOf(VocaDbTag(74, "cover"), VocaDbTag(89, "voicebank demo")),
                )
            )
    }

    @Test
    fun `get artist by NND id test`(wm: WireMockRuntimeInfo): Unit = runBlocking {
        stubFor(
            get(urlPathEqualTo("/api/artists"))
                .withQueryParams(mapOf("query" to equalTo("user/34514369")))
                .withHeader(USER_AGENT, equalTo(DEFAULT_USER_AGENT))
                .willReturn(
                    ok()
                        .withBody(loadResource("responses/vocadb/artist_lookup_result.json"))
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                )
        )

        assertThat(DbClient(wm.httpBaseUrl, jsonMapper).getArtistByQuery("user/34514369"))
            .usingRecursiveComparison()
            .isEqualTo(VocaDbArtist(21_260, "おゆう"))
    }

    @Test
    fun `find artist duplicate test`(wm: WireMockRuntimeInfo): Unit = runBlocking {
        stubFor(
            post(urlPathEqualTo("/Artist/FindDuplicate"))
                .withFormParam("term1", equalTo(""))
                .withFormParam("term2", equalTo(""))
                .withFormParam("term3", equalTo(""))
                .withFormParam("linkUrl", equalTo("https://ch.nicovideo.jp/channel/ch2648319"))
                .withHeader(USER_AGENT, equalTo(DEFAULT_USER_AGENT))
                .willReturn(
                    ok()
                        .withBody(loadResource("responses/vocadb/artist_duplicate_search_result.json"))
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                )
        )

        assertThat(
                DbClient(wm.httpBaseUrl, jsonMapper).findArtistDuplicate("https://ch.nicovideo.jp/channel/ch2648319")
            )
            .usingRecursiveComparison()
            .isEqualTo(VocaDbArtist(106_476, "無色透名祭"))
    }

    @ParameterizedTest
    @CsvSource(value = ["yes,true", "no,false"])
    fun `artist has songs before date test`(
        filenameSuffix: String,
        expectedResult: Boolean,
        wm: WireMockRuntimeInfo,
    ): Unit = runBlocking {
        stubFor(
            get(urlPathEqualTo("/api/songs"))
                .withQueryParams(
                    mapOf(
                        "artistId[]" to equalTo("123"),
                        "beforeDate" to equalTo("beforeDate"),
                        "maxResults" to equalTo("1"),
                        "getTotalCount" to equalTo("true"),
                    )
                )
                .withHeader(USER_AGENT, equalTo(DEFAULT_USER_AGENT))
                .willReturn(
                    ok()
                        .withBody(
                            loadResource(
                                "responses/vocadb/previously_published_songs_lookup_result_$filenameSuffix.json"
                            )
                        )
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                )
        )

        assertThat(DbClient(wm.httpBaseUrl, jsonMapper).artistHasSongsBeforeDate(123, "beforeDate"))
            .isEqualTo(expectedResult)
    }

    @Test
    fun `get song tags test`(wm: WireMockRuntimeInfo, @Given cookie: String): Unit = runBlocking {
        stubFor(
            get(urlPathTemplate("/api/users/current/songTags/{songId}"))
                .withPathParam("songId", equalTo("63276"))
                .withHeader(USER_AGENT, equalTo(DEFAULT_USER_AGENT))
                .withCookie(COOKIE_HEADER_KEY, equalTo(cookie))
                .willReturn(
                    ok()
                        .withBody(loadResource("responses/vocadb/song_tags_lookup_result.json"))
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                )
        )

        assertThat(DbClient(wm.httpBaseUrl, jsonMapper).getSongTags(63_276, cookie))
            .containsExactlyInAnyOrder(
                VocaDbTagSelectable(VocaDbTag(8087, "karaoke available (DAM&JOY)"), true),
                VocaDbTagSelectable(VocaDbTag(8909, "blue"), false),
            )
    }

    @Test
    fun `get tag usages on song test`(wm: WireMockRuntimeInfo, @Given cookie: String): Unit = runBlocking {
        stubFor(
            get(urlPathTemplate("/api/songs/{songId}/tagUsages"))
                .withPathParam("songId", equalTo("657775"))
                .withHeader(USER_AGENT, equalTo(DEFAULT_USER_AGENT))
                .withCookie(COOKIE_HEADER_KEY, equalTo(cookie))
                .willReturn(
                    ok()
                        .withBody(loadResource("responses/vocadb/song_tag_usages_response.json"))
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                )
        )

        assertThat(DbClient(wm.httpBaseUrl, jsonMapper).getTagUsages(SONGS, 657_775, cookie))
            .usingRecursiveComparison()
            .isEqualTo(
                VocaDbTagUsages(
                    true,
                    listOf(
                        VocaDbTagUsage(1_746_998, VocaDbTag(158, "first work")),
                        VocaDbTagUsage(1_746_996, VocaDbTag(7323, "プロセカ")),
                        VocaDbTagUsage(1_572_957, VocaDbTag(10_162, "小樽組")),
                    ),
                )
            )
    }

    @ParameterizedTest(name = "api type: {0}")
    @FieldSource("queryConsoleResponseDeserializationTestData")
    fun `get data with tags by custom query test`(
        apiType: ApiType,
        expectedObject: VocaDbCustomQuerySearchResult<VocaDbCustomQueryData>,
        wm: WireMockRuntimeInfo,
    ): Unit = runBlocking {
        stubFor(
            get(urlPathTemplate("/api/{apiType}"))
                .withPathParam("apiType", equalTo(apiType.toString()))
                .withQueryParams(
                    mapOf(
                        "fields" to equalTo("Tags"),
                        "getTotalCount" to equalTo("true"),
                        "maxResults" to equalTo("1"),
                        "tagId[]" to equalTo("158"),
                        "tagId[]" to equalTo("3353"),
                        "advancedFilters[0][filterType]" to equalTo("HasMedia"),
                        "advancedFilters[0][negate]" to equalTo("true"),
                    )
                )
                .withHeader(USER_AGENT, equalTo(DEFAULT_USER_AGENT))
                .willReturn(
                    ok()
                        .withBody(loadResource("responses/vocadb/custom_query_${apiType}_response.json"))
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                )
        )

        assertThat(
                DbClient(wm.httpBaseUrl, jsonMapper)
                    .getDataWithTagsByCustomQuery(
                        apiType,
                        "maxResults=1&tagId[]=158&tagId[]=3353&advancedFilters[0][filterType]=HasMedia&advancedFilters[0][negate]=true",
                    )
            )
            .usingRecursiveComparison()
            .isEqualTo(expectedObject)
    }

    @ParameterizedTest
    @EnumSource(ApiType::class)
    fun `delete tag usage test`(
        apiType: ApiType,
        @Given tagUsageId: Long,
        @Given cookie: String,
        wm: WireMockRuntimeInfo,
    ) {
        stubFor(
            delete(urlPathTemplate("/api/users/current/{tagType}/{tagUsageId}"))
                .withPathParam("tagType", equalTo(apiType.tagType))
                .withPathParam("tagUsageId", equalTo(tagUsageId.toString()))
                .withCookie(COOKIE_HEADER_KEY, equalTo(cookie))
                .withHeader(USER_AGENT, equalTo(DEFAULT_USER_AGENT))
                .willReturn(ok())
        )

        assertThatNoException().isThrownBy {
            runBlocking { DbClient(wm.httpBaseUrl, jsonMapper).deleteTagUsage(apiType, tagUsageId, cookie) }
        }
    }

    @Test
    fun `get songs with PVs for tagging test`(
        @Given startOffset: Long,
        @Given maxResults: Long,
        @Given orderBy: VocaDbSortOrder,
        wm: WireMockRuntimeInfo,
    ): Unit = runBlocking {
        stubFor(
            get(urlPathEqualTo("/api/songs"))
                .withQueryParams(
                    mapOf(
                        "start" to equalTo(startOffset.toString()),
                        "maxResults" to equalTo(maxResults.toString()),
                        "sort" to equalTo(orderBy.toString()),
                        "getTotalCount" to equalTo("true"),
                    )
                )
                .withHeader(USER_AGENT, equalTo(DEFAULT_USER_AGENT))
                .willReturn(
                    ok()
                        .withBody(loadResource("responses/vocadb/songs_with_pvs_for_tagging_response.json"))
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                )
        )

        assertThat(DbClient(wm.httpBaseUrl, jsonMapper).getSongs(startOffset, maxResults, orderBy, emptyMap()))
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .isEqualTo(
                VocaDbSongEntryWithNndPvsTagsAndReleaseEventsSearchResult(
                    listOf(
                        VocaDbSongEntryWithNndPvsTagsAndReleaseEvents(
                            743_185,
                            "オッドライアー",
                            Original,
                            emptyList(),
                            "佐伯れん feat. 鏡音リン",
                            Instant.parse("2025-01-25T00:00:00Z"),
                            emptyList(),
                            emptyList(),
                        )
                    ),
                    373_865,
                )
            )
    }

    @Test
    fun `get all events for year test`(wm: WireMockRuntimeInfo): Unit = runBlocking {
        stubFor(
            get(urlPathEqualTo("/api/releaseEvents"))
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
                .withHeader(USER_AGENT, equalTo(DEFAULT_USER_AGENT))
                .willReturn(
                    ok()
                        .withBody(loadResource("responses/vocadb/all_events_for_year_response.json"))
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                )
        )

        assertThat(DbClient(wm.httpBaseUrl, jsonMapper).getAllEventsForYear(false))
            .containsExactlyInAnyOrder(
                VocaDbReleaseEvent(
                    8657,
                    OffsetDateTime.parse("2025-01-24T00:00:00Z").toInstant(),
                    null,
                    "病みボカ界隈へようこそ！ 第1話こどもじゃないもん",
                    Club,
                    emptyList(),
                    emptyList(),
                    null,
                    null,
                    MainPicture(URI("https://static.vocadb.net/img/releaseevent/mainOrig/8657.jpg?v=5")),
                ),
                VocaDbReleaseEvent(
                    8842,
                    OffsetDateTime.parse("2026-01-16T00:00:00Z").toInstant(),
                    OffsetDateTime.parse("2026-01-19T00:00:00Z").toInstant(),
                    "第一回ボカロ三部作投稿祭（三部目）",
                    Unspecified,
                    emptyList(),
                    emptyList(),
                    864,
                    VocaDbReleaseEventSeries(864, Festival, emptyList()),
                    null,
                ),
            )
    }

    @Test
    fun `get front page data test`(wm: WireMockRuntimeInfo): Unit = runBlocking {
        stubFor(
            get(urlPathEqualTo("/api/frontpage"))
                .withHeader(USER_AGENT, equalTo(DEFAULT_USER_AGENT))
                .willReturn(
                    ok()
                        .withBody(loadResource("responses/vocadb/front_page_response.json"))
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                )
        )

        assertThat(DbClient(wm.httpBaseUrl, jsonMapper).getFrontPageData())
            .usingRecursiveComparison()
            .isEqualTo(
                VocaDbFrontPageData(
                    listOf(
                        VocaDbReleaseEvent(
                            7231,
                            OffsetDateTime.parse("2024-06-09T00:00:00Z").toInstant(),
                            OffsetDateTime.parse("2025-06-08T00:00:00Z").toInstant(),
                            "モミアゲヲ投稿祭 2024",
                            Unspecified,
                            emptyList(),
                            emptyList(),
                            545,
                            VocaDbReleaseEventSeries(545, Festival, emptyList()),
                            MainPicture(URI("https://static.vocadb.net/img/releaseevent/mainOrig/7231.jpg?v=3")),
                        )
                    )
                )
            )
    }

    companion object {
        val queryConsoleResponseDeserializationTestData =
            VocaDbCustomQueryData::class.sealedSubclasses.map {
                when (it) {
                    VocaDbCustomQueryArtistData::class ->
                        arguments(
                            ARTISTS,
                            VocaDbCustomQuerySearchResult(
                                listOf(
                                    VocaDbCustomQueryArtistData(
                                        85_276,
                                        "Anji-Melody",
                                        listOf(VocaDbTag(204, "illustrator")),
                                        Producer,
                                    )
                                ),
                                121_082,
                            ),
                        )

                    VocaDbCustomQuerySongData::class ->
                        arguments(
                            SONGS,
                            VocaDbCustomQuerySearchResult(
                                listOf(
                                    VocaDbCustomQuerySongData(
                                        717_140,
                                        "『わっれっちゃう。』",
                                        listOf(VocaDbTag(4582, "good tuning")),
                                        Cover,
                                        "Soto feat. 留音ロッカ",
                                    )
                                ),
                                707_091,
                            ),
                        )

                    else -> error("No argument set for subclass ${it.simpleName}")
                }
            }

        val loginFailureTestData =
            Instancio.ofCartesianProduct(VocaDbUser::class.java)
                .ignore(field(VocaDbUser::class.java, "allowedGroups"))
                .with(field(VocaDbUser::class.java, "active"), true, false)
                .with(field(VocaDbUser::class.java, "groupId"), *UserGroup.entries.toTypedArray())
                .create()
                .filter { !it.active || !setOf(Trusted, Moderator, Admin).contains(it.groupId) }
                .map { Arguments.of(it) }
    }
}
