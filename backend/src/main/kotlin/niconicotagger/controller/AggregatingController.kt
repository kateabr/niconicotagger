package niconicotagger.controller

import jakarta.validation.Valid
import niconicotagger.dto.api.misc.QueryConsoleData
import niconicotagger.dto.api.request.GetReleaseEventRequest
import niconicotagger.dto.api.request.QueryConsoleRequest
import niconicotagger.dto.api.request.SongsWithPvsRequest
import niconicotagger.dto.api.request.VideosByNndEventTagsRequest
import niconicotagger.dto.api.request.VideosByNndTagsRequest
import niconicotagger.dto.api.request.VideosByVocaDbTagRequest
import niconicotagger.dto.api.response.QueryConsoleResponse
import niconicotagger.dto.api.response.ReleaseEventWithVocaDbTagsResponse
import niconicotagger.dto.api.response.ReleaseEventWitnNndTagsResponse
import niconicotagger.dto.api.response.SongsWithPvsResponse
import niconicotagger.dto.api.response.VideosByNndTagsResponseForEvent
import niconicotagger.dto.api.response.VideosByNndTagsResponseForTagging
import niconicotagger.dto.api.response.VideosByVocaDbTagResponse
import niconicotagger.service.AggregatingService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(value = ["/api/get"])
class AggregatingController(private val service: AggregatingService) {

    @PostMapping(value = ["/data/by_custom_query"])
    suspend fun getDataByCustomQuery(
        @Valid @RequestBody request: QueryConsoleRequest
    ): QueryConsoleResponse<out QueryConsoleData> {
        return service.getDataWithTagsByCustomQuery(request)
    }

    @PostMapping(value = ["/release_event"])
    suspend fun getReleaseEvent(@Valid @RequestBody request: GetReleaseEventRequest): ReleaseEventWitnNndTagsResponse {
        return service.getReleaseEventByName(request)
    }

    @PostMapping(value = ["/release_event/with_linked_tags"])
    suspend fun getReleaseEventWithLinkedTags(
        @Valid @RequestBody request: GetReleaseEventRequest
    ): ReleaseEventWithVocaDbTagsResponse {
        return service.getReleaseEventWithLinkedTags(request)
    }

    @PostMapping(value = ["/videos/by_nnd_tags/for_tagging"])
    suspend fun getVideosByNndTagsForTagging(
        @Valid @RequestBody request: VideosByNndTagsRequest
    ): VideosByNndTagsResponseForTagging {
        return service.getVideosByNndTags(request)
    }

    @PostMapping(value = ["/videos/by_nnd_tags/for_event"])
    suspend fun getVideosByNndTagsForEvent(
        @Valid @RequestBody request: VideosByNndEventTagsRequest
    ): VideosByNndTagsResponseForEvent {
        return service.getVideosByEventNndTags(request)
    }

    @PostMapping(value = ["/videos/by_vocadb_tag"])
    suspend fun getVideosByVocaDbTags(
        @Valid @RequestBody request: VideosByVocaDbTagRequest
    ): VideosByVocaDbTagResponse {
        return service.getVideosByVocaDbTagMappings(request)
    }

    @PostMapping(value = ["/songs"])
    suspend fun getVocaDbSongEntriesForTagging(@Valid @RequestBody request: SongsWithPvsRequest): SongsWithPvsResponse {
        return service.getSongsWithPvsForTagging(request)
    }
}
