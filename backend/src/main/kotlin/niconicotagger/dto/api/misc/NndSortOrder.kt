package niconicotagger.dto.api.misc

import com.fasterxml.jackson.annotation.JsonValue

enum class NndSortOrder(private val value: String) {
    VIEW_COUNT("viewCounter"),
    PUBLISH_TIME("startTime"),
    LIKE_COUNT("likeCounter");

    @JsonValue
    override fun toString() = value
}
