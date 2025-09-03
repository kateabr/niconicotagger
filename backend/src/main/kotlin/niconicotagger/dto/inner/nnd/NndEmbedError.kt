package niconicotagger.dto.inner.nnd

data class NndEmbedError(val errorParams: ErrorParams) : NndEmbed, GenericNndErrorVideoData {
    data class ErrorParams(val code: String)

    override fun code() = errorParams.code
}
