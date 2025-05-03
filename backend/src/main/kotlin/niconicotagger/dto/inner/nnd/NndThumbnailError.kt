package niconicotagger.dto.inner.nnd

data class NndThumbnailError(val error: Error) : NndThumbnail

data class Error(val code: String, val description: String)
