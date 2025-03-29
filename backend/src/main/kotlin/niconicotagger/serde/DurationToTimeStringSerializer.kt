package niconicotagger.serde

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import java.time.Duration

class DurationToTimeStringSerializer : JsonSerializer<Duration>() {
    override fun serialize(value: Duration, gen: JsonGenerator, serializers: SerializerProvider?) {
        if (value.toHours() > 0)
            gen.writeString("${value.toHoursPart()}:%02d:%02d".format(value.toMinutesPart(), value.toSecondsPart()))
        else
            gen.writeString("${value.toMinutesPart()}:%02d".format(value.toSecondsPart()))
    }
}
