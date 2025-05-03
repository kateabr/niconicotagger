package niconicotagger.dto.inner.vocadb.search.result

import niconicotagger.dto.inner.vocadb.VocaDbArtist

data class VocaDbArtistSearchResult(override val items: List<VocaDbArtist>, override val totalCount: Long) :
    SearchResult<VocaDbArtist>
