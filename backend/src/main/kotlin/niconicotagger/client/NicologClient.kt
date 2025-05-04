package niconicotagger.client

import kotlinx.coroutines.reactor.awaitSingle
import niconicotagger.constants.Constants.DEFAULT_USER_AGENT
import org.jsoup.Jsoup
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.HttpHeaders.USER_AGENT
import org.springframework.http.MediaType.TEXT_HTML_VALUE
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
class NicologClient {
    private val client: WebClient =
        WebClient.builder().baseUrl("https://www.nicolog.jp").defaultHeader(USER_AGENT, DEFAULT_USER_AGENT).build()

    suspend fun getUserName(userId: Long): String? {
        val html =
            client
                .get()
                .uri("/user/{userId}", mapOf("userId" to userId))
                .header(CONTENT_TYPE, TEXT_HTML_VALUE)
                .exchangeToMono { it.bodyToMono(String::class.java) }
                .awaitSingle()
        return Jsoup.parse(html).title().removeSuffix("(ID:$userId)｜ユーザー動画｜ニコログ").removeSuffix("さん ").trim().ifEmpty {
            null
        }
    }

    suspend fun getChannelName(channelId: Long): String? {
        val html =
            client
                .get()
                .uri("/ch/{channelId}", mapOf("channelId" to channelId))
                .header(CONTENT_TYPE, TEXT_HTML_VALUE)
                .exchangeToMono { it.bodyToMono(String::class.java) }
                .awaitSingle()
        return Jsoup.parse(html).title().removeSuffix("(ID:ch$channelId)｜チャンネル動画｜ニコログ").trim().ifEmpty { null }
    }
}
