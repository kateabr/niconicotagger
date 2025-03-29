package niconicotagger.dto.inner.vocadb

data class VocaDbTagUsages(
    val canRemoveTagUsages: Boolean,
    val tagUsages: List<VocaDbTagUsage>
)

data class VocaDbTagUsage(
    val id: Long,
    val tag: VocaDbTag
)
