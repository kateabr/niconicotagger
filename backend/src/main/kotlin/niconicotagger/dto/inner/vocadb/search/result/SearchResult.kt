package niconicotagger.dto.inner.vocadb.search.result

sealed interface SearchResult<T> {
    val items: List<T>
    val totalCount: Long
}
