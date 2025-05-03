package niconicotagger.dto.api.misc

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import java.time.Instant
import niconicotagger.serde.LocalDateToInstantDeserializer

@JsonIgnoreProperties(ignoreUnknown = false)
data class EventDateBounds(
    @JsonDeserialize(using = LocalDateToInstantDeserializer::class) val from: Instant,
    @JsonDeserialize(using = LocalDateToInstantDeserializer::class) val to: Instant?,
    val applyToSearch: Boolean,
)
