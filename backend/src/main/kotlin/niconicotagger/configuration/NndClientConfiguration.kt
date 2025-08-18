package niconicotagger.configuration

import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import niconicotagger.client.NndClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class NndClientConfiguration {
    @Bean
    fun nndClient(
        props: NndApiProps,
        jsonMapper: JsonMapper,
        xmlMapper: XmlMapper,
        webClientBuilder: WebClient.Builder,
    ) =
        NndClient(
            props.channelHost.toString(),
            props.thumbnailHost.toString(),
            props.embedHost.toString(),
            props.snapshotApiHost.toString(),
            jsonMapper,
            xmlMapper,
            webClientBuilder,
        )
}
