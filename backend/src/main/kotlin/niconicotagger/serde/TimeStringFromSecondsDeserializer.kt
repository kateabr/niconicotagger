package niconicotagger.serde

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import java.time.Duration
import java.time.Duration.ZERO
import niconicotagger.serde.Utils.durationToTimeString

class TimeStringFromSecondsDeserializer : JsonDeserializer<String>() {
    override fun deserialize(p: JsonParser?, ctxt: DeserializationContext?): String {
        return durationToTimeString(p?.valueAsLong?.let { Duration.ofSeconds(it) } ?: ZERO)
    }
}
