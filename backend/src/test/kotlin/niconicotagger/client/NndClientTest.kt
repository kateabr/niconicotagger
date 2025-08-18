package niconicotagger.client

import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.notFound
import com.github.tomakehurst.wiremock.client.WireMock.ok
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.client.WireMock.urlPathTemplate
import com.github.tomakehurst.wiremock.http.Body
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo
import com.github.tomakehurst.wiremock.junit5.WireMockTest
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import kotlinx.coroutines.runBlocking
import niconicotagger.Utils.jsonMapper
import niconicotagger.Utils.loadResource
import niconicotagger.Utils.xmlMapper
import niconicotagger.constants.Constants.API_SEARCH_FIELDS
import niconicotagger.constants.Constants.DEFAULT_USER_AGENT
import niconicotagger.constants.Constants.GENRE_FILTER
import niconicotagger.dto.api.misc.ClientType
import niconicotagger.dto.api.misc.EventDateBounds
import niconicotagger.dto.api.misc.NndSortOrder.PUBLISH_TIME
import niconicotagger.dto.api.request.VideosByNndEventTagsRequest
import niconicotagger.dto.api.request.VideosByNndTagsRequest
import niconicotagger.dto.inner.nnd.Error
import niconicotagger.dto.inner.nnd.NndApiSearchResult
import niconicotagger.dto.inner.nnd.NndMeta
import niconicotagger.dto.inner.nnd.NndTag
import niconicotagger.dto.inner.nnd.NndThumbnail
import niconicotagger.dto.inner.nnd.NndThumbnailError
import niconicotagger.dto.inner.nnd.NndThumbnailOk
import niconicotagger.dto.inner.nnd.NndVideoData
import niconicotagger.dto.inner.nnd.ThumbData
import org.assertj.core.api.Assertions.assertThat
import org.instancio.Instancio
import org.instancio.junit.Given
import org.instancio.junit.InstancioExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.EmptySource
import org.junit.jupiter.params.provider.FieldSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.HttpHeaders.USER_AGENT
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.MediaType.APPLICATION_XML_VALUE
import org.springframework.http.MediaType.TEXT_HTML_VALUE
import org.springframework.web.reactive.function.client.WebClient

@WireMockTest
@ExtendWith(InstancioExtension::class)
class NndClientTest {

    @ParameterizedTest
    @FieldSource("testData")
    fun `get thumbnail test`(
        body: ByteArray,
        expected: NndThumbnail,
        wm: WireMockRuntimeInfo,
        @Given id: String,
    ): Unit = runBlocking {
        stubFor(
            get(urlPathTemplate("/api/getthumbinfo/{id}"))
                .withPathParam("id", equalTo(id))
                .withHeader(USER_AGENT, equalTo(DEFAULT_USER_AGENT))
                .willReturn(ok().withResponseBody(Body(body)).withHeader(CONTENT_TYPE, APPLICATION_XML_VALUE))
        )

        assertThat(createClient(thumbBaseUrl = wm.httpBaseUrl).getThumbInfo(id))
            .usingRecursiveComparison()
            .isEqualTo(expected)
    }

    @Test
    fun `get formatted description test`(wm: WireMockRuntimeInfo, @Given id: String): Unit = runBlocking {
        stubFor(
            get(urlPathTemplate("/watch/{id}"))
                .withPathParam("id", equalTo(id))
                .withHeader(USER_AGENT, equalTo(DEFAULT_USER_AGENT))
                .willReturn(ok().withResponseBody(Body(sampleEmbed)).withHeader(CONTENT_TYPE, TEXT_HTML_VALUE))
        )

        assertThat(createClient(embedBaseUrl = wm.httpBaseUrl).getFormattedDescription(id))
            .isEqualTo(expectedDescription)
    }

    @Test
    fun `get formatted description test (redirection)`(
        wm: WireMockRuntimeInfo,
        @Given id: String,
        @Given newId: Long,
    ): Unit = runBlocking {
        stubFor(
            get(urlPathTemplate("/watch/{id}"))
                .withPathParam("id", equalTo(id))
                .withHeader(USER_AGENT, equalTo(DEFAULT_USER_AGENT))
                .willReturn(
                    ok()
                        .withResponseBody(Body("Found. Redirecting to https://embed.nicovideo.jp/watch/$newId"))
                        .withHeader(CONTENT_TYPE, TEXT_HTML_VALUE)
                )
        )
        stubFor(
            get(urlPathTemplate("/watch/{id}"))
                .withPathParam("id", equalTo(newId.toString()))
                .withHeader(USER_AGENT, equalTo(DEFAULT_USER_AGENT))
                .willReturn(ok().withResponseBody(Body(sampleEmbed)).withHeader(CONTENT_TYPE, TEXT_HTML_VALUE))
        )

        assertThat(createClient(embedBaseUrl = wm.httpBaseUrl).getFormattedDescription(id))
            .isEqualTo(expectedDescription)
    }

    @ParameterizedTest
    @ValueSource(strings = ["scope_tag"])
    @EmptySource
    fun `get videos by NND tag`(scope: String, wm: WireMockRuntimeInfo): Unit = runBlocking {
        stubFor(
            get(urlPathTemplate("/api/v2/snapshot/video/contents/search"))
                .withQueryParams(
                    mapOf(
                        "q" to equalTo("tag $scope"),
                        "_offset" to equalTo("0"),
                        "_limit" to equalTo("100"),
                        "_sort" to equalTo("startTime"),
                        "targets" to equalTo("tagsExact"),
                        "fields" to equalTo(API_SEARCH_FIELDS),
                        "jsonFilter" to equalToJson(jsonMapper.writeValueAsString(GENRE_FILTER)),
                    )
                )
                .withHeader(USER_AGENT, equalTo(DEFAULT_USER_AGENT))
                .willReturn(
                    ok()
                        .withBody(loadResource("responses/nnd/video_by_tag_lookup_result.json"))
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                )
        )

        assertThat(
                createClient(apiBaseUrl = wm.httpBaseUrl)
                    .getVideosByTags(
                        VideosByNndTagsRequest(
                            setOf("tag"),
                            scope,
                            0,
                            100,
                            PUBLISH_TIME,
                            Instancio.create(ClientType::class.java),
                        )
                    )
            )
            .usingRecursiveComparison()
            .isEqualTo(
                NndApiSearchResult(
                    NndMeta(56),
                    listOf(
                        NndVideoData(
                            "sm44015940",
                            "【詩岸カバー曲】火̸̘̍曜̴̗́日̷̮͠,̶̟͝幽̵̞̏霊̸̭̿,̶̩̎機̴̲͆械̸̳̎ ̸͖͗",
                            listOf("synthesizervカバー曲", "詩岸", "Indie", "NNI", "海外組vocaloid", "shoegaze"),
                            78_431_138,
                            null,
                            Duration.ofSeconds(204),
                            "<strong>火曜日,幽霊,機械（ft.诗岸） <br><br><br>Music & Lyrics & Art works by Rockflos </strong><br>lyrics： <br>到不了的Utopia <br>入口是废弃教室六零八 <br>周二现身幽灵小姐 <br>从老电视机爬出来的吗 <br>头穿过显像管 相互作用力 <br>发生弹性形变 正交分解题 <br>就关掉大脑 学会服从吧 <br>可今天失去的 明天怎么拿 <br>周二会现身幽灵 <br>我们只听见机器轰鸣 <br>忘记歌词唱啦啦啦啦 <br>你脑子都装了什么呀 <br>在六零八找她问话 <br>她又会说什么作为回答 <br>在六零八找她问话 <br>我也不知道有意义吗 <br>生产噪音波的机器 <br>一切就如同空集没意义 <br>砸坏玻璃的安那其 <br>其实我也想偷偷地逃离 <br>坐对面的少女 不该帮助她 <br>怎么也变成了 爱听悄悄话 <br>昨天看的书 不是好结局 <br>我的心被撕碎 杂乱的思绪 <br>周二会现身幽灵 <br>我们只听见机器轰鸣 <br>忘记歌词唱啦啦啦啦 <br>你脑子都装了什么呀 <br>在六零八找她问话 <br>她又会说什么作为回答 <br>在六零八找她问话 <br>我也不知道有意义吗",
                            OffsetDateTime.parse("2024-08-24T17:36:03+09:00").toInstant(),
                            59,
                            0,
                        ),
                        NndVideoData(
                            "so42972459",
                            "【犯罪者茶番劇】赤堀と碇が無色透名祭に参加するそうです　字幕のみ版 / aq_rm＆ar_f4",
                            listOf("犯罪者茶番劇", "音楽", "VOCALOID", "無色透名祭Ⅱ", "無色透名祭Ⅱ参加曲", "無色透名祭Ⅱ作者不明曲リンク"),
                            null,
                            2_648_319,
                            Duration.ofSeconds(47),
                            "【無色透名祭Ⅱ】参加作品です。<br><a href=\"https://site.nicovideo.jp/mushokutomeisai/\" target=\"_blank\">https://site.nicovideo.jp/mushokutomeisai/</a><br>応募番号: M2_1306",
                            OffsetDateTime.parse("2023-11-02T22:00:00+09:00").toInstant(),
                            1_690,
                            1,
                        ),
                    ),
                )
            )
    }

    @ParameterizedTest
    @ValueSource(strings = ["scope_tag"])
    @EmptySource
    fun `get videos by NND event tag`(scope: String, wm: WireMockRuntimeInfo): Unit = runBlocking {
        stubFor(
            get(urlPathTemplate("/api/v2/snapshot/video/contents/search"))
                .withQueryParams(
                    mapOf(
                        "q" to equalTo("tag $scope"),
                        "_offset" to equalTo("0"),
                        "_limit" to equalTo("100"),
                        "_sort" to equalTo("startTime"),
                        "targets" to equalTo("tagsExact"),
                        "fields" to equalTo(API_SEARCH_FIELDS),
                        "jsonFilter" to equalToJson(jsonMapper.writeValueAsString(GENRE_FILTER)),
                    )
                )
                .withHeader(USER_AGENT, equalTo(DEFAULT_USER_AGENT))
                .willReturn(
                    ok()
                        .withBody(loadResource("responses/nnd/video_by_tag_lookup_result.json"))
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                )
        )

        assertThat(
                createClient(apiBaseUrl = wm.httpBaseUrl)
                    .getVideosByTags(
                        VideosByNndEventTagsRequest(
                            setOf("tag"),
                            scope,
                            0,
                            100,
                            PUBLISH_TIME,
                            EventDateBounds(
                                OffsetDateTime.parse("2023-11-02T22:00:00+09:00").minusDays(1).toInstant(),
                                OffsetDateTime.parse("2024-08-24T17:36:03+09:00").plusDays(1).toInstant(),
                                false,
                            ),
                            Instancio.create(Long::class.java),
                            Instancio.create(ClientType::class.java),
                        )
                    )
            )
            .usingRecursiveComparison()
            .isEqualTo(
                NndApiSearchResult(
                    NndMeta(56),
                    listOf(
                        NndVideoData(
                            "sm44015940",
                            "【詩岸カバー曲】火̸̘̍曜̴̗́日̷̮͠,̶̟͝幽̵̞̏霊̸̭̿,̶̩̎機̴̲͆械̸̳̎ ̸͖͗",
                            listOf("synthesizervカバー曲", "詩岸", "Indie", "NNI", "海外組vocaloid", "shoegaze"),
                            78_431_138,
                            null,
                            Duration.ofSeconds(204),
                            "<strong>火曜日,幽霊,機械（ft.诗岸） <br><br><br>Music & Lyrics & Art works by Rockflos </strong><br>lyrics： <br>到不了的Utopia <br>入口是废弃教室六零八 <br>周二现身幽灵小姐 <br>从老电视机爬出来的吗 <br>头穿过显像管 相互作用力 <br>发生弹性形变 正交分解题 <br>就关掉大脑 学会服从吧 <br>可今天失去的 明天怎么拿 <br>周二会现身幽灵 <br>我们只听见机器轰鸣 <br>忘记歌词唱啦啦啦啦 <br>你脑子都装了什么呀 <br>在六零八找她问话 <br>她又会说什么作为回答 <br>在六零八找她问话 <br>我也不知道有意义吗 <br>生产噪音波的机器 <br>一切就如同空集没意义 <br>砸坏玻璃的安那其 <br>其实我也想偷偷地逃离 <br>坐对面的少女 不该帮助她 <br>怎么也变成了 爱听悄悄话 <br>昨天看的书 不是好结局 <br>我的心被撕碎 杂乱的思绪 <br>周二会现身幽灵 <br>我们只听见机器轰鸣 <br>忘记歌词唱啦啦啦啦 <br>你脑子都装了什么呀 <br>在六零八找她问话 <br>她又会说什么作为回答 <br>在六零八找她问话 <br>我也不知道有意义吗",
                            OffsetDateTime.parse("2024-08-24T17:36:03+09:00").toInstant(),
                            59,
                            0,
                        ),
                        NndVideoData(
                            "so42972459",
                            "【犯罪者茶番劇】赤堀と碇が無色透名祭に参加するそうです　字幕のみ版 / aq_rm＆ar_f4",
                            listOf("犯罪者茶番劇", "音楽", "VOCALOID", "無色透名祭Ⅱ", "無色透名祭Ⅱ参加曲", "無色透名祭Ⅱ作者不明曲リンク"),
                            null,
                            2_648_319,
                            Duration.ofSeconds(47),
                            "【無色透名祭Ⅱ】参加作品です。<br><a href=\"https://site.nicovideo.jp/mushokutomeisai/\" target=\"_blank\">https://site.nicovideo.jp/mushokutomeisai/</a><br>応募番号: M2_1306",
                            OffsetDateTime.parse("2023-11-02T22:00:00+09:00").toInstant(),
                            1_690,
                            1,
                        ),
                    ),
                )
            )
    }

    @Test
    fun `get channel handle test (success)`(
        wm: WireMockRuntimeInfo,
        @Given channelId: Long,
        @Given channelHandle: String,
    ): Unit = runBlocking {
        stubFor(
            get(urlPathTemplate("/ch{channelId}"))
                .withPathParam("channelId", equalTo(channelId.toString()))
                .withHeader(USER_AGENT, equalTo(DEFAULT_USER_AGENT))
                .willReturn(ok().withHeader("location", "/$channelHandle"))
        )

        assertThat(createClient(channelBaseHost = wm.httpBaseUrl).getChannelHandle(channelId)).isEqualTo(channelHandle)
    }

    @Test
    fun `get channel handle test (error)`(wm: WireMockRuntimeInfo, @Given channelId: Long): Unit = runBlocking {
        stubFor(
            get(urlPathTemplate("/ch{channelId}"))
                .withPathParam("channelId", equalTo(channelId.toString()))
                .withHeader(USER_AGENT, equalTo(DEFAULT_USER_AGENT))
                .willReturn(notFound())
        )

        assertThat(createClient(channelBaseHost = wm.httpBaseUrl).getChannelHandle(channelId)).isNull()
    }

    companion object {
        val testData =
            listOf(
                argumentSet(
                    "error",
                    loadResource("responses/nnd/nnd_thumbnail_error.xml"),
                    NndThumbnailError(Error("DELETED", "deleted")),
                ),
                argumentSet(
                    "success (user upload)",
                    loadResource("responses/nnd/nnd_thumbnail_ok.xml"),
                    NndThumbnailOk(
                        ThumbData(
                            "sm43909677",
                            "星たちは歌う/小春六花・夏色花梨・花隈千冬",
                            "賑やかしに短い動画を上げました。三人に歌ってもらったよ（輪唱っていいよね）作曲：春乃ねむり歌唱：小春六花・夏色花梨・花隈千冬（SynthesizerV）マイリスト：https://www.nicovideo.jp/mylist/18804133",
                            OffsetDateTime.parse("2024-08-06T00:25:06+09:00").toInstant(),
                            "1:16",
                            125L,
                            listOf(
                                NndTag("SynthesizerV", true),
                                NndTag("小春六花", true),
                                NndTag("夏色花梨", true),
                                NndTag("花隈千冬", true),
                                NndTag("ぼかえり2024夏", true),
                                NndTag("毎月6日はTOKYO6の日", true),
                                NndTag("小春六花オリジナル曲", false),
                                NndTag("夏色花梨オリジナル曲", false),
                                NndTag("花隈千冬オリジナル曲", false),
                                NndTag("VOCALOID", false),
                            ),
                            5_360_605,
                            null,
                            "春乃ねむり",
                        )
                    ),
                ),
                argumentSet(
                    "success (channel upload)",
                    loadResource("responses/nnd/nnd_thumbnail_ok_channel.xml"),
                    NndThumbnailOk(
                        ThumbData(
                            "so42972459",
                            "【犯罪者茶番劇】赤堀と碇が無色透名祭に参加するそうです　字幕のみ版 / aq_rm＆ar_f4",
                            "【無色透名祭Ⅱ】参加作品です。https://site.nicovideo.jp/mushokutomeisai/応募番号: M2_1306",
                            OffsetDateTime.parse("2023-11-02T22:00:00+09:00").toInstant(),
                            "0:47",
                            1684L,
                            listOf(
                                NndTag("犯罪者茶番劇", true),
                                NndTag("音楽", true),
                                NndTag("VOCALOID", true),
                                NndTag("無色透名祭Ⅱ", true),
                                NndTag("無色透名祭Ⅱ参加曲", true),
                                NndTag("無色透名祭Ⅱ作者不明曲リンク", false),
                            ),
                            null,
                            2_648_319,
                            "ボカロ曲匿名投稿イベント 無色透名祭",
                        )
                    ),
                ),
                argumentSet(
                    "success (publisher hidden)",
                    loadResource("responses/nnd/nnd_thumbnail_ok_publisher_hidden.xml"),
                    NndThumbnailOk(
                        ThumbData(
                            "sm12487751",
                            "【ボカロオリジナル曲】ぱんちゅ【初音ミク】",
                            "1000再生感謝感謝ぱんちゅ！！記念すべきボーカロイドオリジナル処女作が「これ」です。あの、タイトルだけ見てポップ系だと思った方ごめんなさい。HRですorz　あ、私、牛乳プリンと申します。ぱんちゅ♪【イラスト】ベリー嬢さん 【HP】→http://milkprin.nukenin.jp/　【twitter】→https://twitter.com/milkprin 【マイリスト】→mylist/17952061 p→ぱんちゅ♪ぱんちゅ♪　です多分。 【追記】mp3,offVoと普通音源とうｐしときました。→http://cid-71426121b38652da.spaces.live.com/ の燃費⑨デシリットル　musicフォルダにて。病人でｻｰｾﾝｗ　　新作、まさかの替え歌→創Keiのアクエリオン sm12779469",
                            Instant.parse("2010-10-20T04:45:12+09:00"),
                            "4:24",
                            1758,
                            listOf(NndTag("VOCALOID", true), NndTag("P名由来リンク", false)),
                        )
                    ),
                ),
            )

        val sampleEmbed = loadResource("responses/nnd/sample_embed.html")
        const val expectedDescription =
            "賑やかしに短い動画を上げました。<br />三人に歌ってもらったよ（輪唱っていいよね）<br><br>作曲：春乃ねむり<br>歌唱：小春六花・夏色花梨・花隈千冬（SynthesizerV）<br>マイリスト：https://www.nicovideo.jp/mylist/18804133<br>　"

        fun createClient(
            channelBaseHost: String = "",
            thumbBaseUrl: String = "",
            embedBaseUrl: String = "",
            apiBaseUrl: String = "",
        ) =
            NndClient(
                channelBaseHost,
                thumbBaseUrl,
                embedBaseUrl,
                apiBaseUrl,
                jsonMapper,
                xmlMapper,
                WebClient.builder(),
            )
    }
}
