package niconicotagger.dto.inner.vocadb.search.result

import niconicotagger.dto.inner.vocadb.VocaDbReleaseEvent

data class VocaDbReleaseEventSearchResult(override val items: List<VocaDbReleaseEvent>, override val totalCount: Long) :
    SearchResult<VocaDbReleaseEvent>
