package niconicotagger.dto.inner.vocadb

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import niconicotagger.dto.inner.misc.ArtistRole
import niconicotagger.dto.inner.misc.ArtistType
import niconicotagger.serde.ArtistRoleStringDeserializer

data class VocaDbEntryArtist(
    val isSupport: Boolean,
    @JsonAlias("artist") val artistEntryData: VocaDbArtistEntryData?,
    @JsonDeserialize(using = ArtistRoleStringDeserializer::class) val effectiveRoles: List<ArtistRole> = emptyList(),
)

data class VocaDbArtistEntryData(val id: Long, val name: String, val artistType: ArtistType)
