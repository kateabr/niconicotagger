package niconicotagger.dto.api.misc

import com.fasterxml.jackson.annotation.JsonValue

enum class ClientType(val displayName: String) {
    VOCADB("VocaDB"),
    VOCADB_BETA("VocaDB BETA");

    @JsonValue override fun toString(): String = name.lowercase()
}
