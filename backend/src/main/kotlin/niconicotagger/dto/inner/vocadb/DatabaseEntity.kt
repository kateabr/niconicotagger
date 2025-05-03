package niconicotagger.dto.inner.vocadb

interface DatabaseEntity {
    val id: Long
    val name: String

    fun getPathSegment(): String
}
