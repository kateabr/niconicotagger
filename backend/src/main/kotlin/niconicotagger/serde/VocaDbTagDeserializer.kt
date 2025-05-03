package niconicotagger.serde

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.core.io.SerializedString
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import niconicotagger.dto.inner.vocadb.VocaDbTag

class VocaDbTagDeserializer : JsonDeserializer<VocaDbTag>() {
    private val tagFieldName = SerializedString("tag")

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): VocaDbTag {
        var id = 0L
        var name = ""
        while (!p.nextFieldName(tagFieldName)) {}
        p.nextToken()
        do {
            p.nextToken()
            if (p.currentToken == JsonToken.FIELD_NAME)
                when (p.valueAsString) {
                    "id" -> {
                        p.nextToken()
                        id = p.valueAsLong
                    }

                    "name" -> name = p.nextTextValue()
                }
        } while (p.currentToken != JsonToken.END_OBJECT)
        do {
            p.nextToken()
        } while (p.currentToken != JsonToken.END_OBJECT)
        return VocaDbTag(id, name)
    }
}
