package niconicotagger.dto.inner.vocadb.search.result

data class ArtistDuplicateSearchResult(val entry: Entry) {
    data class Entry(val id: Long, val name: DisplayName) {
        data class DisplayName(val displayName: String)
    }
}
