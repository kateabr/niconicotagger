package niconicotagger.serde

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import java.time.Duration
import java.time.Duration.ZERO

class DurationFromSecondsDeserializer : JsonDeserializer<Duration>() {
    override fun deserialize(p: JsonParser?, ctxt: DeserializationContext?): Duration {
        return p?.valueAsLong?.let { Duration.ofSeconds(it) } ?: ZERO
    }
}
