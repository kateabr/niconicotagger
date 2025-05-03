package niconicotagger.dto.inner.misc

import com.fasterxml.jackson.annotation.JsonAlias

data class SongPv(@JsonAlias("pvId") val id: String, val name: String, val disabled: Boolean, val service: PvService)

enum class PvService {
    Nothing,
    NicoNicoDouga,
    Youtube,
    SoundCloud,
    Vimeo,
    Piapro,
    Bilibili,
    File,
    LocalFile,
    Creofuga,
    Bandcamp,
}
