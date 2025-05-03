package niconicotagger.dto.inner.vocadb

data class PublisherInfo(val link: String, val linkText: String?, val name: String?, val type: PublisherType)

enum class PublisherType {
    NND_USER,
    NND_CHANNEL,
    DATABASE,
}
