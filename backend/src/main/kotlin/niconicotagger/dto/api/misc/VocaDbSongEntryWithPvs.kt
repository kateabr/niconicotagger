package niconicotagger.dto.api.misc

data class VocaDbSongEntryWithPvs(
    val entry: SongEntry,
    val availablePvs: List<PvWithSuggestedTags>,
    val unavailablePvs: List<UnavailableNndVideo>,
)
