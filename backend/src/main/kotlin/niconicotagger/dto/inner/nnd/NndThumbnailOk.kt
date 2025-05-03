package niconicotagger.dto.inner.nnd

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonNaming
import java.time.Instant
import niconicotagger.serde.NndTagDeserializer
import niconicotagger.serde.TrimmingStringDeserializer

data class NndThumbnailOk(@JsonProperty("thumb") val data: ThumbData) : NndThumbnail

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(SnakeCaseStrategy::class)
data class ThumbData(
    val videoId: String,
    val title: String,
    @JsonDeserialize(using = TrimmingStringDeserializer::class) val description: String?,
    @JsonProperty("first_retrieve") val uploadDate: Instant,
    val length: String,
    @JsonProperty("view_counter") val views: Long,
    @JsonDeserialize(contentUsing = NndTagDeserializer::class) val tags: List<NndTag>,
    val userId: Long? = null,
    @JsonProperty("ch_id") val channelId: Long? = null,
    @JsonAlias(value = ["user_nickname", "ch_name"]) val publisherName: String? = null,
)

data class NndTag(val name: String, val locked: Boolean)
