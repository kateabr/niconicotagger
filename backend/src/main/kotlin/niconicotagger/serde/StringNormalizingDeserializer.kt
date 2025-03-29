package niconicotagger.serde

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import niconicotagger.serde.Utils.normalizeToken

class StringNormalizingDeserializer : TrimmingStringDeserializer() {
    override fun deserialize(p: JsonParser?, ctxt: DeserializationContext?): String? {
        return super.deserialize(p, ctxt)?.let { normalizeToken(it) }
    }
}
