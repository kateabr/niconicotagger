package niconicotagger.dto.inner.vocadb.search.result

import niconicotagger.dto.inner.vocadb.VocaDbCustomQueryData

data class VocaDbCustomQuerySearchResult<T : VocaDbCustomQueryData>(
    override val items: List<T>,
    override val totalCount: Long
) : SearchResult<T>
