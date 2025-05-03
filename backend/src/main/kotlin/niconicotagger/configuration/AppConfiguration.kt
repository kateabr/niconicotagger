package niconicotagger.configuration

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app")
data class AppConfiguration(
    val vocaDbClientBaseAddress: VocaDbClientBaseAddress,
    val nndClientProperties: NndClientProperties,
)

data class VocaDbClientBaseAddress(val regular: String, val beta: String)

data class NndClientProperties(val thumbnail: String, val embed: String, val apiBaseAddress: String)
