package niconicotagger.dto.inner.nnd

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import java.time.Duration
import java.time.Instant
import niconicotagger.serde.DurationFromSecondsDeserializer
import niconicotagger.serde.StringNormalizingDeserializer
import niconicotagger.serde.UnescapingStringDeserializer

data class NndEmbedOk(
    val videoId: String,
    val title: String,
    @JsonProperty("videoUploaderId") val userId: Long? = null,
    val channel: Channel? = null,
    @JsonDeserialize(using = UnescapingStringDeserializer::class) val description: String,
    @JsonProperty("firstRetrieve") val uploadDate: Instant,
    @JsonProperty("lengthInSeconds")
    @JsonDeserialize(using = DurationFromSecondsDeserializer::class)
    val length: Duration,
    @JsonDeserialize(contentUsing = StringNormalizingDeserializer::class) val tags: List<String>,
) : NndEmbed {
    data class Channel(val id: Long, val name: String)
}
