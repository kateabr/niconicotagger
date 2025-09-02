package niconicotagger.dto.inner.nnd

data class NndEmbedError(val errorParams: ErrorParams) : NndEmbed {
    data class ErrorParams(val code: String)
}
