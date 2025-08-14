package niconicotagger.dto.api.misc

data class VocaDbSongEntryWithPvs(
    val entry: SongEntryWithPublishDateAndReleaseEventInfo,
    val availablePvs: List<PvWithSuggestedTags>,
    val unavailablePvs: List<UnavailableNndVideo>,
)
