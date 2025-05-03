package niconicotagger.dto.inner.vocadb.search.result

import niconicotagger.dto.inner.vocadb.VocaDbSongEntryWithNndPvsAndTags

data class VocaDbSongEntryWithNndPvsAndTagsSearchResult(
    override val items: List<VocaDbSongEntryWithNndPvsAndTags>,
    override val totalCount: Long,
) : SearchResult<VocaDbSongEntryWithNndPvsAndTags>
