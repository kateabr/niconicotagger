package niconicotagger.dto.inner.nnd

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import java.time.Instant
import niconicotagger.serde.StringNormalizingDeserializer
import niconicotagger.serde.TimeStringFromSecondsDeserializer
import niconicotagger.serde.UnescapingStringDeserializer

data class NndEmbedOk(
    val videoId: String,
    val title: String,
    @JsonProperty("videoUploaderId") val userId: Long? = null,
    val channel: Channel? = null,
    @JsonDeserialize(using = UnescapingStringDeserializer::class) val description: String,
    @JsonProperty("firstRetrieve") val uploadDate: Instant,
    @JsonProperty("lengthInSeconds")
    @JsonDeserialize(using = TimeStringFromSecondsDeserializer::class)
    val length: String,
    @JsonDeserialize(contentUsing = StringNormalizingDeserializer::class) val tags: List<String>,
) : NndEmbed, GenericNndOkVideoData {
    data class Channel(val id: Long, val name: String)

    override fun videoId() = videoId

    override fun title() = title

    override fun description() = description

    override fun userId() = userId

    override fun channelId() = channel?.id

    override fun uploadDate() = uploadDate

    override fun length() = length

    override fun tags() = tags.map { NndTag(it, false) }
}
