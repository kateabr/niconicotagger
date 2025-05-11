package niconicotagger.serde

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import niconicotagger.serde.Utils.normalizeToken

// character case and width handled by NND
class NndScopeNormalizingDeserializer : TrimmingStringDeserializer() {
    override fun deserialize(p: JsonParser?, ctxt: DeserializationContext?): String? {
        return super.deserialize(p, ctxt)?.removePrefix("OR ")?.split(" ")?.joinToString(" ") { normalizeToken(it) }
    }
}
