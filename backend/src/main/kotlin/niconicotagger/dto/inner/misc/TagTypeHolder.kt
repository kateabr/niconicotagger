package niconicotagger.dto.inner.misc

import niconicotagger.dto.api.misc.NndTagType
import niconicotagger.dto.api.misc.NndTagType.MAPPED
import niconicotagger.dto.api.misc.NndTagType.SCOPE
import niconicotagger.dto.api.misc.NndTagType.TARGET
import niconicotagger.dto.api.request.VideosByNndTagsRequestBase
import niconicotagger.dto.inner.vocadb.VocaDbTagMapping
import niconicotagger.serde.Utils.kata2hiraAndLowercase

class TagTypeHolder {
    private val tagStates = mutableMapOf<String, NndTagType>()

    fun put(tag: String, state: NndTagType) {
        tagStates.putIfAbsent(kata2hiraAndLowercase(tag).lowercase(), state)
    }

    fun get(tag: String): NndTagType {
        return requireNotNull(tagStates[kata2hiraAndLowercase(tag).lowercase()]) {
            "Tag type for \"$tag\" is undefined"
        }
    }

    fun storeRequestTags(request: VideosByNndTagsRequestBase): TagTypeHolder {
        request.tags.forEach { put(it, TARGET) }
        request.scope
            .split(" ")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .filter { it != "OR" && !it.startsWith("-") }
            .map { it.lowercase() }
            .forEach { put(it, SCOPE) }
        return this
    }

    fun storeTagMappings(tagMappings: List<VocaDbTagMapping>): TagTypeHolder {
        tagMappings.forEach { put(it.sourceTag, MAPPED) }
        return this
    }
}
