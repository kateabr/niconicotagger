package niconicotagger.serde

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset.UTC
import java.time.format.DateTimeFormatter.ISO_DATE

class LocalDateToInstantDeserializer : JsonDeserializer<Instant>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): Instant {
        return LocalDate.parse(p.valueAsString, ISO_DATE).atStartOfDay(UTC).toInstant()
    }
}
