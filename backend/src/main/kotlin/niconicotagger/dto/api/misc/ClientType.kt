package niconicotagger.dto.api.misc

import com.fasterxml.jackson.annotation.JsonValue

enum class ClientType {
    VOCADB, VOCADB_BETA;

    @JsonValue
    override fun toString(): String = name.lowercase()
}
