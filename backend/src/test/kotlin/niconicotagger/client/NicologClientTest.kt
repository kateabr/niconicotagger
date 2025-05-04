package niconicotagger.client

import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.ok
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.client.WireMock.urlPathTemplate
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo
import com.github.tomakehurst.wiremock.junit5.WireMockTest
import kotlinx.coroutines.runBlocking
import niconicotagger.Utils.loadResource
import niconicotagger.constants.Constants.DEFAULT_USER_AGENT
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.HttpHeaders.USER_AGENT
import org.springframework.http.MediaType.TEXT_HTML_VALUE

@WireMockTest
class NicologClientTest {
    @ParameterizedTest
    @CsvSource(value = ["44309388,綾鷹", "443093884564,null"], nullValues = ["null"])
    fun `get user name (success)`(id: Long, expectedName: String?, wm: WireMockRuntimeInfo): Unit = runBlocking {
        stubFor(
            get(urlPathTemplate("/user/{id}"))
                .withPathParam("id", equalTo(id.toString()))
                .withHeader(USER_AGENT, equalTo(DEFAULT_USER_AGENT))
                .willReturn(
                    ok()
                        .withBody(loadResource("responses/nicolog/user_${id}_lookup_result.html").toString())
                        .withHeader(CONTENT_TYPE, TEXT_HTML_VALUE)
                )
        )

        assertThat(NicologClient().getUserName(id)).isEqualTo(expectedName)
    }

    @ParameterizedTest
    @CsvSource(value = ["2648319,ボカロ曲匿名投稿イベント 無色透名祭", "264831945876,null"], nullValues = ["null"])
    fun `get channel name (success)`(id: Long, expectedName: String?, wm: WireMockRuntimeInfo): Unit = runBlocking {
        stubFor(
            get(urlPathTemplate("/ch/{id}"))
                .withPathParam("id", equalTo(id.toString()))
                .withHeader(USER_AGENT, equalTo(DEFAULT_USER_AGENT))
                .willReturn(
                    ok()
                        .withBody(loadResource("responses/nicolog/channel_ch${id}_lookup_result.html").toString())
                        .withHeader(CONTENT_TYPE, TEXT_HTML_VALUE)
                )
        )

        assertThat(NicologClient().getChannelName(id)).isEqualTo(expectedName)
    }
}
