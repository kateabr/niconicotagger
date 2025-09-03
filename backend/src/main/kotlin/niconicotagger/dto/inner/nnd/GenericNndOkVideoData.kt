package niconicotagger.dto.inner.nnd

import java.time.Instant

sealed interface GenericNndOkVideoData : GenericNndVideoData {
    fun videoId(): String

    fun title(): String

    fun description(): String

    fun userId(): Long?

    fun channelId(): Long?

    fun uploadDate(): Instant

    fun length(): String

    fun tags(): List<NndTag>
}
