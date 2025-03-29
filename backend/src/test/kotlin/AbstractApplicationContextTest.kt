import io.mockk.junit5.MockKExtension
import niconicotagger.Application
import org.instancio.junit.InstancioExtension
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK
import org.springframework.test.context.junit.jupiter.SpringExtension


@ExtendWith(value = [SpringExtension::class, InstancioExtension::class, MockKExtension::class])
@SpringBootTest(
    webEnvironment = MOCK,
    classes = [Application::class]
)
abstract class AbstractApplicationContextTest
