package niconicotagger.service

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import niconicotagger.client.DbClientHolder
import niconicotagger.constants.Constants.DEFAULT_USER_AGENT
import niconicotagger.constants.Constants.PVS_DISABLED_EDIT_NOTE
import niconicotagger.dto.api.misc.ClientType
import niconicotagger.dto.api.request.ReleaseEventAdditionRequest
import niconicotagger.dto.api.request.SongTagsAndPvsUpdateRequest
import niconicotagger.dto.api.request.TagDeletionRequest
import niconicotagger.dto.inner.misc.PvService.NicoNicoDouga
import niconicotagger.dto.inner.misc.SongType.Original
import niconicotagger.dto.inner.vocadb.DatabaseEntity
import niconicotagger.dto.inner.vocadb.VocaDbTag
import niconicotagger.dto.inner.vocadb.VocaDbTagUsages
import org.springframework.stereotype.Service

@Service
class UpdatingService(private val dbClientHolder: DbClientHolder) {

    private fun getClient(clientType: ClientType) = dbClientHolder.getClient(clientType)

    suspend fun disablePvs(request: SongTagsAndPvsUpdateRequest, clientType: ClientType, cookie: String) {
        if (request.nndPvsToDisable.isEmpty()) return
        val songData = getClient(clientType).getSongForEdit(request.songId, cookie)
        val pvs = songData["pvs"] as MutableList<MutableMap<String, Any>>
        val nndPvIdsToDisable = request.nndPvsToDisable.map { it.id }.toSet()
        // delete unofficial uploads
        pvs.removeIf { isTargetPv(it, nndPvIdsToDisable, false) }
        // disable original uploads
        pvs.filter { isTargetPv(it, nndPvIdsToDisable, true) }.forEach { pv -> pv["disabled"] = true }
        songData["updateNotes"] =
            request.nndPvsToDisable.joinToString(", ", PVS_DISABLED_EDIT_NOTE, " (via $DEFAULT_USER_AGENT)") {
                "${it.id}: ${it.reason}"
            }
        getClient(clientType).saveSong(request.songId, songData, cookie)
    }

    suspend fun assignSongTags(request: SongTagsAndPvsUpdateRequest, clientType: ClientType, cookie: String) {
        if (request.tags.isEmpty()) return
        val newTags =
            request.tags.map { VocaDbTag(it, "") } +
                getClient(clientType).getSongTags(request.songId, cookie).filter { it.selected }.map { it.tag }
        getClient(clientType).assignSongTags(request.songId, newTags, cookie)
    }

    suspend fun addReleaseEvent(request: ReleaseEventAdditionRequest, clientType: ClientType, cookie: String) {
        val event = requireNotNull(request.event)
        val songData = getClient(clientType).getSongForEdit(request.entryId, cookie)
        val releaseEvents = songData["releaseEvents"] as MutableList<Map<String, Any>>
        if (releaseEvents.any { it["id"].toString() == event.id.toString() }) {
            error("Release event ${formatEntity(event)} is already added")
        }
        releaseEvents.add(mapOf("id" to event.id))
        songData["updateNotes"] = "Added event ${formatEntity(event)} (via $DEFAULT_USER_AGENT)"
        getClient(clientType).saveSong(request.entryId, songData, cookie)
    }

    suspend fun deleteTags(request: TagDeletionRequest, clientType: ClientType, cookie: String) {
        val tagUsagesByRequestTagId =
            dbClientHolder
                .getClient(clientType)
                .getTagUsages(request.apiType, request.entryId, cookie)
                .also { validate(it) }
                .tagUsages
                .filter { request.tags.any { reqTag -> reqTag.id == it.tag.id } }
                .associateBy { it.tag.id }
        request.tags
            .filter { tagUsagesByRequestTagId[it.id] == null }
            .let {
                if (it.isNotEmpty())
                    error(
                        "Following tags were not found on the entry: ${
                        request.tags.joinToString(", ") { tag -> formatEntity(tag) }
                    }"
                    )
            }
        coroutineScope {
            tagUsagesByRequestTagId
                .map { async { getClient(clientType).deleteTagUsage(request.apiType, it.value.id, cookie) } }
                .awaitAll()
        }
    }

    private fun isTargetPv(pv: MutableMap<String, Any>, nndPvIdsToDisable: Set<String>, isOriginal: Boolean): Boolean {
        return pv["service"] == NicoNicoDouga.toString() &&
            (isOriginal == (pv["pvType"] == Original.toString())) &&
            nndPvIdsToDisable.any { pvId -> pvId == pv["pvId"] }
    }

    private fun validate(tagUsages: VocaDbTagUsages): Unit {
        if (!tagUsages.canRemoveTagUsages) error("User lacks permission to remove tags")
    }

    private fun formatEntity(entity: DatabaseEntity): String {
        return "${entity.getPathSegment()}/${entity.id} \"${entity.name}\""
    }
}
