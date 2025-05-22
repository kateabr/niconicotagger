package niconicotagger.serde

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import niconicotagger.serde.Utils.kata2hiraAndLowercase

// get rid of character case and width (for raw string comparison)
class StringUnifyingDeserializer : StringNormalizingDeserializer() {
    override fun deserialize(p: JsonParser?, ctxt: DeserializationContext?): String? {
        return super.deserialize(p, ctxt)?.let { kata2hiraAndLowercase(it) }
    }
}
