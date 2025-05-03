package niconicotagger.dto.api.misc

import niconicotagger.dto.inner.vocadb.VocaDbTagSelectable

data class PvWithSuggestedTags(val video: AvailableNndVideo, val suggestedTags: List<VocaDbTagSelectable>)
