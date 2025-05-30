package niconicotagger.controller

import jakarta.validation.Valid
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import niconicotagger.constants.Constants.COOKIE_HEADER_KEY
import niconicotagger.dto.api.request.DeleteTagsRequestWrapper
import niconicotagger.dto.api.request.MassAddReleaseEventRequest
import niconicotagger.dto.api.request.SongTagsAndEventsMassUpdateRequest
import niconicotagger.dto.api.request.SongTagsAndPvsMassUpdateRequest
import niconicotagger.dto.api.response.UpdateError
import niconicotagger.dto.api.response.UpdateReport
import niconicotagger.dto.api.response.UpdateSuccess
import niconicotagger.service.UpdatingService
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(value = ["/api/update"])
class UpdatingController(private val service: UpdatingService) {

    @PostMapping(value = ["/songs/add_release_event"])
    suspend fun addReleaseEvent(
        @Valid @RequestBody request: MassAddReleaseEventRequest,
        @CookieValue(COOKIE_HEADER_KEY) cookie: String,
    ): List<UpdateError> =
        withContext(SupervisorJob()) {
                request.subRequests.map {
                    async {
                        try {
                            service.addReleaseEvent(it, request.clientType, cookie)
                            UpdateSuccess()
                        } catch (expected: Exception) {
                            UpdateError(it.entryId, expected.message)
                        }
                    }
                }
            }
            .awaitAll()
            .filterIsInstance<UpdateError>()

    @PostMapping(value = ["/tags/delete"])
    suspend fun deleteTags(
        @Valid @RequestBody reqWrapper: DeleteTagsRequestWrapper,
        @CookieValue(COOKIE_HEADER_KEY) cookie: String,
    ): UpdateReport = runBlocking {
        // only 1 request at a time to not overload the db
        return@runBlocking try {
            service.deleteTags(reqWrapper.request, reqWrapper.clientType, cookie)
            UpdateSuccess()
        } catch (expected: Exception) {
            UpdateError(reqWrapper.request.entryId, expected.message)
        }
    }

    @PostMapping(value = ["/songs/replace_tag_with_event"])
    suspend fun updateSongEventsAndTags(
        @Valid @RequestBody request: SongTagsAndEventsMassUpdateRequest,
        @CookieValue(COOKIE_HEADER_KEY) cookie: String,
    ): List<UpdateError> =
        withContext(SupervisorJob()) {
                request.subRequests.map {
                    async {
                        try {
                            if (it.event != null) {
                                service.addReleaseEvent(it, request.clientType, cookie)
                            }
                            service.deleteTags(it, request.clientType, cookie)
                            UpdateSuccess()
                        } catch (expected: Exception) {
                            UpdateError(it.entryId, expected.message)
                        }
                    }
                }
            }
            .awaitAll()
            .filterIsInstance<UpdateError>()

    @PostMapping(value = ["/songs/update_tags_and_pvs"])
    suspend fun updateSongTagsAndPvs(
        @Valid @RequestBody request: SongTagsAndPvsMassUpdateRequest,
        @CookieValue(COOKIE_HEADER_KEY) cookie: String,
    ): List<UpdateError> =
        withContext(SupervisorJob()) {
                request.subRequests.map {
                    async {
                        try {
                            service.assignSongTags(it, request.clientType, cookie)
                            service.disablePvs(it, request.clientType, cookie)
                            it.pvId?.let { pvId -> service.removeSongsByPvFromCache(request.clientType, pvId) }
                            UpdateSuccess()
                        } catch (expected: Exception) {
                            UpdateError(it.songId, expected.message)
                        }
                    }
                }
            }
            .awaitAll()
            .filterIsInstance<UpdateError>()
}
