package niconicotagger.serde

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import niconicotagger.serde.Utils.normalizeToken

class NndTagStringDeserializer : JsonDeserializer<List<String>>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): List<String> {
        return p.valueAsString.split(" ").map { normalizeToken(it) }
    }
}
