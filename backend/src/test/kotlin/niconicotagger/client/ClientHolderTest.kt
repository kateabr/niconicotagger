package niconicotagger.client

import niconicotagger.AbstractApplicationContextTest
import niconicotagger.dto.api.misc.ClientType
import niconicotagger.dto.api.misc.ClientType.VOCADB
import niconicotagger.dto.api.misc.ClientType.VOCADB_BETA
import niconicotagger.dto.api.misc.SupportedDatabase
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatNoException
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired

class ClientHolderTest : AbstractApplicationContextTest() {
    @Autowired lateinit var clientHolder: DbClientHolder

    @ParameterizedTest
    @EnumSource(ClientType::class)
    fun `client holder test`(clientType: ClientType) {
        assertThatNoException().isThrownBy { clientHolder.getClient(clientType) }
    }

    @Test
    fun `supported databases test`() {
        assertThat(clientHolder.getSupportedDatabases())
            .containsExactlyInAnyOrder(
                SupportedDatabase(VOCADB, VOCADB.displayName, "https://vocadb.net"),
                SupportedDatabase(VOCADB_BETA, VOCADB_BETA.displayName, "https://beta.vocadb.net"),
            )
    }
}
