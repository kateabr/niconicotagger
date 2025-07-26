package niconicotagger.client

import com.fasterxml.jackson.databind.json.JsonMapper
import niconicotagger.configuration.DbApiProps
import niconicotagger.dto.api.misc.ClientType
import niconicotagger.dto.api.misc.SupportedDatabase
import org.springframework.stereotype.Component

@Component
class DbClientHolder(dbApiProps: DbApiProps, jsonMapper: JsonMapper) {
    private val databaseClients =
        ClientType.entries.associateWith { DbClient(dbApiProps.database[it]!!.host.toString(), jsonMapper) }
    private val supportedDatabases =
        databaseClients.keys.map { SupportedDatabase(it, it.displayName, dbApiProps.database[it]!!.host.toString()) }

    fun getClient(clientType: ClientType): DbClient {
        return databaseClients[clientType]
            ?: error("Could not match client type \"$clientType\" with any existing client")
    }

    fun getSupportedDatabases(): List<SupportedDatabase> = supportedDatabases
}
