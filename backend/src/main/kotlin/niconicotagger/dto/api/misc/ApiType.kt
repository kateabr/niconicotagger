package niconicotagger.dto.api.misc

import com.fasterxml.jackson.annotation.JsonValue

enum class ApiType(val tagType: String) {
    SONGS("SongTags"),
    ARTISTS("ArtistTags");

    @JsonValue override fun toString() = name.lowercase()
}
