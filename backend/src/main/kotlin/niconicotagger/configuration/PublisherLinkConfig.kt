package niconicotagger.configuration

import niconicotagger.dto.api.misc.ClientType
import niconicotagger.dto.inner.vocadb.PublisherType
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "publisher-link-config")
class PublisherLinkConfig(
    private val nndUser: LinkData,
    private val nndChannel: LinkData,
    private val vocadbBeta: LinkData,
    private val vocadb: LinkData
) {
    private fun getLinkData(publisherType: PublisherType, clientType: ClientType? = null): LinkData {
        return when (publisherType) {
            PublisherType.NND_USER -> nndUser
            PublisherType.NND_CHANNEL -> nndChannel
            PublisherType.DATABASE -> when (clientType) {
                ClientType.VOCADB -> vocadb
                ClientType.VOCADB_BETA -> vocadbBeta
                null -> error("Cannot provide publisher link base for $publisherType: client type not provided")
            }
        }
    }

    fun getFullLink(id: Long, publisherType: PublisherType, clientType: ClientType? = null): String =
        getLinkData(publisherType, clientType).fullLink() + id

    fun getLinkPath(id: Long, publisherType: PublisherType, clientType: ClientType? = null): String =
        getLinkData(publisherType, clientType).path + id
}

class LinkData(private val host: String, val path: String) {
    fun fullLink() = host + path
}
