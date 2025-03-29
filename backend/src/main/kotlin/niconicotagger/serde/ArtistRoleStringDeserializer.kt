package niconicotagger.serde

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import niconicotagger.dto.inner.misc.ArtistRole

class ArtistRoleStringDeserializer : JsonDeserializer<List<ArtistRole>>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): List<ArtistRole> {
        return p.valueAsString.split(", ").map(ArtistRole::valueOf)
    }
}
