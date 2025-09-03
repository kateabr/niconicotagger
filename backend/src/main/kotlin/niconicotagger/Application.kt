package niconicotagger

import niconicotagger.configuration.DbApiProps
import niconicotagger.configuration.DbTagProps
import niconicotagger.configuration.NndApiProps
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication(exclude = [ErrorMvcAutoConfiguration::class])
@EnableConfigurationProperties(value = [DbApiProps::class, NndApiProps::class, DbTagProps::class])
@EnableScheduling
class Application private constructor() {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(Application::class.java, *args)
        }
    }
}
