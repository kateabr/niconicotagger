package niconicotagger.serde

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import niconicotagger.serde.Utils.kata2hiraAndLowercase
import niconicotagger.serde.Utils.normalizeToken

class VocaDbTagMappingsNormalizingDeserializer : TrimmingStringDeserializer() {
    override fun deserialize(p: JsonParser?, ctxt: DeserializationContext?): String? {
        return super.deserialize(p, ctxt)?.let { kata2hiraAndLowercase(normalizeToken(it)) }
    }
}
