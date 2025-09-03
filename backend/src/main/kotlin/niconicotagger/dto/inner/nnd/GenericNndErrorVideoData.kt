package niconicotagger.dto.inner.nnd

sealed interface GenericNndErrorVideoData : GenericNndVideoData {
    fun code(): String
}
