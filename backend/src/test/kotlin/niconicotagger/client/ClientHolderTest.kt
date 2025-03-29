package niconicotagger.client

import Utils.jsonMapper
import niconicotagger.configuration.AppConfiguration
import niconicotagger.configuration.NndClientProperties
import niconicotagger.configuration.VocaDbClientBaseAddress
import niconicotagger.dto.api.misc.ClientType
import org.assertj.core.api.Assertions.assertThatNoException
import org.instancio.Instancio
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class ClientHolderTest {
    @ParameterizedTest
    @EnumSource(ClientType::class)
    fun `client holder test`(clientType: ClientType) {
        assertThatNoException().isThrownBy {
            DbClientHolder(
                AppConfiguration(
                    Instancio.create(VocaDbClientBaseAddress::class.java),
                    Instancio.create(NndClientProperties::class.java)
                ), jsonMapper
            ).getClient(clientType)
        }
    }
}
