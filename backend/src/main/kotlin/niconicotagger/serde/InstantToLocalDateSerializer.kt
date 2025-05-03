package niconicotagger.serde

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import java.time.Instant
import java.time.ZoneOffset.UTC
import java.time.format.DateTimeFormatter.ISO_DATE

class InstantToLocalDateSerializer : JsonSerializer<Instant?>() {
    override fun serialize(value: Instant?, gen: JsonGenerator?, serializers: SerializerProvider?) {
        gen?.writeString(value?.atOffset(UTC)?.toLocalDate()?.format(ISO_DATE))
    }
}
