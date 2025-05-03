package niconicotagger.mapper

import niconicotagger.dto.api.request.VideosByNndTagsRequest
import niconicotagger.dto.api.request.VideosByVocaDbTagRequest
import org.assertj.core.api.Assertions.assertThat
import org.instancio.junit.Given
import org.instancio.junit.InstancioExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mapstruct.factory.Mappers

@ExtendWith(InstancioExtension::class)
class RequestMapperTest {
    private val mapper: RequestMapper = Mappers.getMapper(RequestMapper::class.java)

    @Test
    fun `VideosByNndTagsRequest mapping test`(@Given request: VideosByVocaDbTagRequest, @Given tags: Set<String>) {
        assertThat(mapper.map(request, tags))
            .usingRecursiveComparison()
            .isEqualTo(
                VideosByNndTagsRequest(
                    tags,
                    request.scope,
                    request.startOffset,
                    request.maxResults,
                    request.orderBy,
                    request.clientType,
                )
            )
    }
}
