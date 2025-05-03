package niconicotagger.serde

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken.END_OBJECT
import com.fasterxml.jackson.core.JsonToken.VALUE_STRING
import com.fasterxml.jackson.core.io.SerializedString
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import niconicotagger.dto.inner.nnd.NndTag
import niconicotagger.serde.Utils.normalizeToken

class NndTagDeserializer : JsonDeserializer<NndTag>() {
    private val lockFieldName = SerializedString("lock")
    private val tagFieldName = SerializedString("")

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): NndTag {
        if (p.currentToken == VALUE_STRING) return NndTag(normalizeToken(p.valueAsString), false)

        val locked = p.nextFieldName(lockFieldName)
        while (!p.nextFieldName(tagFieldName)) {
            // do nothing
        }
        val tag = p.nextTextValue()
        do {
            p.nextToken()
        } while (p.currentToken != END_OBJECT)
        return NndTag(normalizeToken(tag), locked)
    }
}
