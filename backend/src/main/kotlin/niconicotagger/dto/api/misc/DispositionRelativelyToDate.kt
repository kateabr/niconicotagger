package niconicotagger.dto.api.misc

import com.fasterxml.jackson.annotation.JsonValue

data class DispositionRelativelyToDate(val disposition: Disposition, val byDays: Long, val color: FrontendColorCode) {
    enum class Disposition {
        EARLY,
        PERFECT,
        LATE;

        @JsonValue override fun toString() = name.lowercase()
    }
}
