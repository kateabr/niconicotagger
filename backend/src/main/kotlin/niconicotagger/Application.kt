package niconicotagger

import niconicotagger.configuration.AppConfiguration
import niconicotagger.configuration.PublisherLinkConfig
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication(exclude = [ErrorMvcAutoConfiguration::class])
@EnableConfigurationProperties(value = [AppConfiguration::class, PublisherLinkConfig::class])
@EnableScheduling
class Application private constructor() {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(Application::class.java, *args)
        }
    }
}
