package niconicotagger.dto.inner.nnd

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import niconicotagger.serde.DurationFromSecondsDeserializer
import niconicotagger.serde.NndTagStringDeserializer
import niconicotagger.serde.UnescapingStringDeserializer
import java.time.Duration
import java.time.Instant

data class NndApiSearchResult(val meta: NndMeta, val data: List<NndVideoData>)

data class NndMeta(val totalCount: Long)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class NndVideoData(
    @JsonProperty("contentId")
    val id: String,
    @JsonDeserialize(using = UnescapingStringDeserializer::class)
    val title: String,
    @JsonDeserialize(using = NndTagStringDeserializer::class)
    val tags: List<String>,
    val userId: Long?,
    val channelId: Long?,
    @JsonProperty("lengthSeconds")
    @JsonDeserialize(using = DurationFromSecondsDeserializer::class)
    val length: Duration,
    @JsonDeserialize(using = UnescapingStringDeserializer::class)
    val description: String?,
    @JsonProperty("startTime")
    val publishedAt: Instant,
    val viewCounter: Long,
    val likeCounter: Long
)
