package niconicotagger.dto.api.misc

import com.fasterxml.jackson.annotation.JsonValue

enum class NndTagType(@JsonValue val color: FrontendColorCode) {
    TARGET(FrontendColorCode.PRIMARY),
    SCOPE(FrontendColorCode.INFO),
    MAPPED(FrontendColorCode.DARK),
    NONE(FrontendColorCode.SECONDARY)
}
