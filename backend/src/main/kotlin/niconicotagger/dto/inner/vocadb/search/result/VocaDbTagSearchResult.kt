package niconicotagger.dto.inner.vocadb.search.result

import niconicotagger.dto.inner.vocadb.VocaDbTag

data class VocaDbTagSearchResult(override val items: List<VocaDbTag>, override val totalCount: Long) :
    SearchResult<VocaDbTag>
