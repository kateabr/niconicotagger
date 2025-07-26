package niconicotagger.configuration

import niconicotagger.AbstractApplicationContextTest
import niconicotagger.configuration.dto.NndPublisherType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired

class NndApiPropsTest : AbstractApplicationContextTest() {
    @Autowired lateinit var nndApiProps: NndApiProps

    @ParameterizedTest
    @EnumSource(NndPublisherType::class)
    fun `publisher type config test`(publisherType: NndPublisherType) {
        assertThat(nndApiProps.publisherProps).containsKey(publisherType)
    }
}
