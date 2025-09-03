package niconicotagger.serde

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import java.time.Duration
import niconicotagger.serde.Utils.durationToTimeString

class DurationToTimeStringSerializer : JsonSerializer<Duration>() {
    override fun serialize(value: Duration, gen: JsonGenerator, serializers: SerializerProvider?) {
        gen.writeString(durationToTimeString(value))
    }
}
