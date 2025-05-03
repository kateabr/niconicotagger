package niconicotagger.serde

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer

open class TrimmingStringDeserializer : JsonDeserializer<String?>() {
    override fun deserialize(p: JsonParser?, ctxt: DeserializationContext?): String? {
        return p?.valueAsString?.trim()
    }
}
