package niconicotagger.configuration

import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import niconicotagger.client.NndClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class NndClientConfiguration {
    @Bean
    fun nndClient(config: AppConfiguration, jsonMapper: JsonMapper, xmlMapper: XmlMapper) =
        NndClient(
            config.nndClientProperties.thumbnail,
            config.nndClientProperties.embed,
            config.nndClientProperties.apiBaseAddress,
            jsonMapper,
            xmlMapper,
        )
}
