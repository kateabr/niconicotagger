package niconicotagger.dto.api.misc

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import java.time.Duration
import java.time.Instant
import niconicotagger.serde.DurationToTimeStringSerializer

sealed interface NndVideo {
    val id: String
    val title: String
}

sealed interface AvailableNndVideoBase : NndVideo {
    override val id: String
    override val title: String
    val description: String?
    val tags: List<NndTagData>
}

data class AvailableNndVideo(
    override val id: String,
    override val title: String,
    override val description: String?,
    override val tags: List<NndTagData>,
) : AvailableNndVideoBase

data class AvailableNndVideoWithAdditionalData(
    override val id: String,
    override val title: String,
    override val description: String?,
    override val tags: List<NndTagData>,
    @JsonSerialize(using = DurationToTimeStringSerializer::class) val length: Duration,
    @JsonIgnore val viewCounter: Long,
    @JsonIgnore val publishedAt: Instant,
    @JsonIgnore val likeCounter: Long,
) : AvailableNndVideoBase

data class UnavailableNndVideo(val id: String, val title: String, val error: String)
