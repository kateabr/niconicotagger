package niconicotagger.service

import com.github.benmanes.caffeine.cache.Caffeine
import com.sksamuel.aedile.core.asCache
import java.net.URI
import java.util.concurrent.TimeUnit.HOURS
import niconicotagger.client.DbClientHolder
import niconicotagger.client.NicologClient
import niconicotagger.client.NndClient
import niconicotagger.configuration.DbApiProps
import niconicotagger.configuration.NndApiProps
import niconicotagger.configuration.dto.BasicProps
import niconicotagger.configuration.dto.NndPublisherType
import niconicotagger.configuration.dto.NndPublisherType.NND_CHANNEL
import niconicotagger.configuration.dto.NndPublisherType.NND_CHANNEL_HANDLE
import niconicotagger.configuration.dto.NndPublisherType.NND_CHANNEL_SHORTENED
import niconicotagger.configuration.dto.NndPublisherType.NND_USER
import niconicotagger.constants.Constants.DATABASE_PLACEHOLDER
import niconicotagger.dto.api.misc.ClientType
import niconicotagger.dto.inner.nnd.NndThumbnailOk
import niconicotagger.dto.inner.nnd.NndVideoData
import niconicotagger.dto.inner.vocadb.PublisherInfo
import org.springframework.stereotype.Service
import org.springframework.web.util.UriComponentsBuilder

@Service
class PublisherInfoService(
    private val dbApiProps: DbApiProps,
    private val nndApiProps: NndApiProps,
    private val dbClientHolder: DbClientHolder,
    private val nndClient: NndClient,
    private val nicologClient: NicologClient,
) {
    private val publisherCache =
        Caffeine.newBuilder().expireAfterAccess(12, HOURS).maximumSize(10_000).asCache<String, PublisherInfo>()

    private fun getClient(clientType: ClientType) = dbClientHolder.getClient(clientType)

    private fun getPublisherLinkProps(publisherType: String): BasicProps =
        when (publisherType) {
            "VOCADB",
            "VOCADB_BETA" -> dbApiProps.database[ClientType.valueOf(publisherType)]!!

            "NND_USER",
            "NND_CHANNEL",
            "NND_CHANNEL_SHORTENED",
            "NND_CHANNEL_HANDLE" -> nndApiProps.publisherProps[NndPublisherType.valueOf(publisherType)]!!

            else -> error("Cannot provide publisher link base for $publisherType: path properties not found")
        }

    private suspend fun buildLink(id: Long, publisherType: String): URI = buildLink(id.toString(), publisherType)

    private suspend fun buildLink(id: String, publisherType: String): URI =
        getPublisherLinkProps(publisherType).let {
            UriComponentsBuilder.fromUri(it.host)
                .pathSegment(*listOfNotNull(it.publisherPath, "${it.idPrefix ?: ""}$id").toTypedArray())
                .build(emptyMap<String, Any>())
        }

    private suspend fun buildLinkPath(id: Long, publisherType: String): String =
        buildLink(id, publisherType).path.substring(1)

    private suspend fun createPublisher(id: Long, name: String?, dbOrService: String) =
        PublisherInfo(
            buildLink(id, dbOrService).toString(),
            buildLinkPath(id, dbOrService),
            name,
            resolvePublisherType(dbOrService),
        )

    // to process db entries regardless of the db they belong to
    private fun resolvePublisherType(dbOrService: String) =
        if (ClientType.entries.any { it.name == dbOrService }) DATABASE_PLACEHOLDER else dbOrService

    // lookup order: database -> NND -> Nicolog
    @Suppress("CognitiveComplexMethod")
    suspend fun getPublisher(video: NndVideoData, clientType: ClientType): PublisherInfo? {
        // try to find a database entry for the publisher
        if (video.userId != null) {
            val dbPublisher =
                publisherCache.getOrNull("${video.userId}_$clientType") {
                    getClient(clientType).getArtistByQuery(buildLinkPath(video.userId, NND_USER.name))?.let {
                        createPublisher(it.id, it.name, clientType.name)
                    }
                }
            if (dbPublisher != null) return dbPublisher
        }
        if (video.channelId != null) {
            val dbPublisherFullPath =
                publisherCache.getOrNull("ch${video.channelId}_$clientType") {
                    getClient(clientType)
                        .findArtistDuplicate(buildLink(video.channelId, NND_CHANNEL.name).toString())
                        ?.let { createPublisher(it.id, it.name, clientType.name) }
                }
            if (dbPublisherFullPath != null) return dbPublisherFullPath

            val dbPublisherShortPath =
                publisherCache.getOrNull("ch${video.channelId}_$clientType") {
                    getClient(clientType)
                        .findArtistDuplicate(buildLink(video.channelId, NND_CHANNEL_SHORTENED.name).toString())
                        ?.let { createPublisher(it.id, it.name, clientType.name) }
                }
            if (dbPublisherShortPath != null) return dbPublisherShortPath

            val channelHandle = nndClient.getChannelHandle(video.channelId)
            if (channelHandle != null) {
                val dbPublisherHandle =
                    publisherCache.getOrNull("ch${video.channelId}_$clientType") {
                        getClient(clientType)
                            .findArtistDuplicate(buildLink(channelHandle, NND_CHANNEL_HANDLE.name).toString())
                            ?.let { createPublisher(it.id, it.name, clientType.name) }
                    }
                if (dbPublisherHandle != null) return dbPublisherHandle
            }
        }

        // publisher not in the database yet; try to extract info from NND
        val nndPublisher =
            nndClient.getThumbInfo(video.id).let { thumbnail ->
                when (thumbnail) {
                    is NndThumbnailOk -> {
                        thumbnail.data.userId?.let { userId ->
                            publisherCache.getOrNull("${userId}_$NND_USER") {
                                createPublisher(userId, thumbnail.data.publisherName, NND_USER.name)
                            }
                        }
                            ?: thumbnail.data.channelId?.let { channelId ->
                                publisherCache.getOrNull("${channelId}_$NND_CHANNEL") {
                                    createPublisher(channelId, thumbnail.data.publisherName, NND_CHANNEL.name)
                                }
                            }
                    }

                    else -> null
                }
            }
        if (nndPublisher != null) return nndPublisher

        // if everything else fails, try to extract publisher's name from nicolog
        if (video.userId != null) {
            val nicologPublisher =
                publisherCache.getOrNull("${video.userId}_$NND_USER") {
                    nicologClient.getUserName(video.userId)?.let { createPublisher(video.userId, it, NND_USER.name) }
                }
            if (nicologPublisher != null) return nicologPublisher
        }
        if (video.channelId != null) {
            val nicologPublisher =
                publisherCache.getOrNull("${video.channelId}_$NND_CHANNEL") {
                    nicologClient.getChannelName(video.channelId)?.let {
                        createPublisher(video.channelId, it, NND_CHANNEL.name)
                    }
                }
            if (nicologPublisher != null) return nicologPublisher
        }

        // if that failed too, return only id
        if (video.userId != null) {
            return createPublisher(video.userId, null, NND_USER.name)
        }
        if (video.channelId != null) {
            return createPublisher(video.channelId, null, NND_CHANNEL.name)
        }

        // normally should not get here
        error("Could not obtain publisher info for video ${video.id}")
    }
}
