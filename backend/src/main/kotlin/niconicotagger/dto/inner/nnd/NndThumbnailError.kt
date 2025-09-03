package niconicotagger.dto.inner.nnd

data class NndThumbnailError(val error: Error) : NndThumbnail, GenericNndErrorVideoData {
    override fun code() = error.code
}

data class Error(val code: String, val description: String)
