package niconicotagger.configuration

import java.net.URL
import niconicotagger.configuration.dto.BasicProps
import niconicotagger.configuration.dto.NndPublisherType
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.api-props.service.nnd")
class NndApiProps(
    val channelHost: URL,
    val thumbnailHost: URL,
    val embedHost: URL,
    val snapshotApiHost: URL,
    val publisherProps: Map<NndPublisherType, BasicProps>,
)
