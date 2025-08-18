package niconicotagger.configuration

import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
import com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.zalando.problem.jackson.ProblemModule
import org.zalando.problem.violations.ConstraintViolationProblemModule

@Configuration
class ObjectMapperConfiguration {
    @Bean
    @Primary
    fun jsonMapper(
        problemModule: ProblemModule,
        constraintViolationProblemModule: ConstraintViolationProblemModule,
    ): JsonMapper =
        JsonMapper.builder()
            .findAndAddModules()
            .addModules(problemModule, constraintViolationProblemModule)
            .disable(WRITE_DATES_AS_TIMESTAMPS)
            .disable(FAIL_ON_UNKNOWN_PROPERTIES)
            .build()

    @Bean fun xmlMapper(): XmlMapper = XmlMapper.builder().findAndAddModules().addModule(JavaTimeModule()).build()
}
