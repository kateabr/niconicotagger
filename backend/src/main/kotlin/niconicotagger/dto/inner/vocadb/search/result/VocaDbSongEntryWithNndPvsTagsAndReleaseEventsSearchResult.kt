package niconicotagger.dto.inner.vocadb.search.result

import niconicotagger.dto.inner.vocadb.VocaDbSongEntryWithNndPvsTagsAndReleaseEvents

data class VocaDbSongEntryWithNndPvsTagsAndReleaseEventsSearchResult(
    override val items: List<VocaDbSongEntryWithNndPvsTagsAndReleaseEvents>,
    override val totalCount: Long,
) : SearchResult<VocaDbSongEntryWithNndPvsTagsAndReleaseEvents>
