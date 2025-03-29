package niconicotagger.dto.api.misc

import com.fasterxml.jackson.annotation.JsonValue

enum class FrontendColorCode {
    PRIMARY,
    INFO,
    SUCCESS,
    WARNING,
    DANGER,
    DARK,
    SECONDARY;

    @JsonValue
    override fun toString() = name.lowercase()
}
