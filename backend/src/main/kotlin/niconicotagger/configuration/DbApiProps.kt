package niconicotagger.configuration

import niconicotagger.configuration.dto.BasicProps
import niconicotagger.dto.api.misc.ClientType
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.api-props", ignoreUnknownFields = true)
class DbApiProps(val database: Map<ClientType, BasicProps>)
