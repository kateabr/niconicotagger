package niconicotagger.dto.inner.nnd

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME

@JsonTypeInfo(use = NAME, include = PROPERTY, property = "status")
@JsonSubTypes(
    value =
        [
            JsonSubTypes.Type(name = "ok", value = NndThumbnailOk::class),
            JsonSubTypes.Type(name = "fail", value = NndThumbnailError::class),
        ]
)
sealed interface NndThumbnail : GenericNndVideoData
