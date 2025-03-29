package niconicotagger.mapper

import niconicotagger.dto.api.request.VideosByNndTagsRequest
import niconicotagger.dto.api.request.VideosByVocaDbTagRequest
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingConstants.ComponentModel.SPRING

@Mapper(componentModel = SPRING)
abstract class RequestMapper {
    @Mapping(target = "tags", expression = "java(tags)")
    abstract fun map(request: VideosByVocaDbTagRequest, tags: Set<String>): VideosByNndTagsRequest
}
