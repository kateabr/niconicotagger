package niconicotagger.configuration

import niconicotagger.dto.api.misc.ClientType
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.db-tag-props")
class DbTagProps(
    private val firstWork: TagProps,
    private val regionBlocked: TagProps,
    private val endlessEvent: TagProps,
) {
    data class TagProps(val embedErrorCode: String?, val database: Map<ClientType, DbProps>) {
        data class DbProps(val id: Long, val name: String?)

        fun toClientSpecificTagProps(clientType: ClientType) =
            ClientSpecificDbTagProps.TagProps(embedErrorCode, database[clientType]!!.id, database[clientType]!!.name)
    }

    fun getClientSpecificProps(clientType: ClientType): ClientSpecificDbTagProps =
        ClientSpecificDbTagProps(
            this.firstWork.toClientSpecificTagProps(clientType),
            this.regionBlocked.toClientSpecificTagProps(clientType),
            this.endlessEvent.toClientSpecificTagProps(clientType),
        )
}

data class ClientSpecificDbTagProps(val firstWork: TagProps, val regionBlocked: TagProps, val endlessEvent: TagProps) {
    data class TagProps(val embedErrorCode: String?, val id: Long, val name: String?)
}
