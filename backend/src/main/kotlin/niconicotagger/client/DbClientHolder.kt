package niconicotagger.client

import com.fasterxml.jackson.databind.json.JsonMapper
import niconicotagger.configuration.AppConfiguration
import niconicotagger.dto.api.misc.ClientType
import niconicotagger.dto.api.misc.ClientType.VOCADB
import niconicotagger.dto.api.misc.ClientType.VOCADB_BETA
import org.springframework.stereotype.Component

@Component
class DbClientHolder(config: AppConfiguration, jsonMapper: JsonMapper) {
    private val databaseClients = mapOf(
        VOCADB to VocaDbClient(config.vocaDbClientBaseAddress.regular, jsonMapper),
        VOCADB_BETA to VocaDbClient(config.vocaDbClientBaseAddress.beta, jsonMapper)
    )

    fun getClient(clientType: ClientType): VocaDbClient {
        return databaseClients[clientType]
            ?: error("Could not match client type \"$clientType\" with any existing client")
    }
}
