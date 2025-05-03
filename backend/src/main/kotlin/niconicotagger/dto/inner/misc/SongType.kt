package niconicotagger.dto.inner.misc

import niconicotagger.dto.inner.vocadb.VocaDbTag

enum class SongType(private val tagIdsToIgnore: Set<Long>) {
    Unspecified(emptySet()),
    Original(setOf(6479, 22, 74)),
    Remaster(setOf(1519, 391, 371, 392, 74)),
    Remix(setOf(371, 74, 391, 4709)),
    Cover(setOf(74, 371, 392)),
    Instrumental(setOf(208)),
    MusicPV(setOf(7378, 74, 4582)),
    Mashup(setOf(3392)),
    DramaPV(
        setOf(
            104,
            1736,
            7276,
            3180,
            7728,
            8509,
            7748,
            7275,
            6701,
            3186,
            8130,
            6700,
            7615,
            6703,
            6702,
            7988,
            6650,
            8043,
            8409,
            423,
        )
    ),
    Other(emptySet());

    fun tagIsApplicable(tag: VocaDbTag): Boolean = !tagIdsToIgnore.contains(tag.id)
}
