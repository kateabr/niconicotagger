package niconicotagger.controller

import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import jakarta.servlet.http.Cookie
import niconicotagger.AbstractApplicationContextTest
import niconicotagger.Utils.eventPreviewMapperFixedClock
import niconicotagger.mapper.ReleaseEventMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.RegisterExtension
import org.mapstruct.factory.Mappers
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.annotation.DirtiesContext.ClassMode.BEFORE_CLASS
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.json.JsonCompareMode.STRICT
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post

@AutoConfigureMockMvc
@DirtiesContext(classMode = BEFORE_CLASS)
@Import(AbstractControllerTest.ControllerTestConfiguration::class)
abstract class AbstractControllerTest : AbstractApplicationContextTest() {
    @Autowired lateinit var mockMvc: MockMvc

    @BeforeEach
    fun resetWiremock() {
        wireMockExtension.resetToDefaultMappings()
    }

    protected fun testInvalidRequest(
        request: String,
        expectedResponse: String,
        uriPath: String,
        cookie: Cookie? = null,
    ) {
        mockMvc
            .post("/api$uriPath") {
                contentType = APPLICATION_JSON
                content = request
                cookie?.let { cookie(it) }
            }
            .andExpect {
                status { is4xxClientError() }
                content { json(expectedResponse, STRICT) }
            }
    }

    @TestConfiguration
    internal class ControllerTestConfiguration {
        @Bean
        @Primary
        fun releaseEventMapperWithFixedClock(): ReleaseEventMapper {
            val mapper = Mappers.getMapper(ReleaseEventMapper::class.java)
            mapper.clock = eventPreviewMapperFixedClock
            return mapper
        }
    }

    companion object {
        @JvmStatic
        @RegisterExtension
        val wireMockExtension: WireMockExtension =
            WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build()

        @DynamicPropertySource
        @JvmStatic
        fun dynamicProperties(registry: DynamicPropertyRegistry) {
            registry.add("app.vocadb-client-base-address.beta") { wireMockExtension.baseUrl() }
            registry.add("app.nnd-client-properties.thumbnail") { wireMockExtension.baseUrl() }
            registry.add("app.nnd-client-properties.embed") { wireMockExtension.baseUrl() }
            registry.add("app.nnd-client-properties.api-base-address") { wireMockExtension.baseUrl() }
        }
    }
}
