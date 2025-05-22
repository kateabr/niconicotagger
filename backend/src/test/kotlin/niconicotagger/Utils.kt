package niconicotagger

import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
import com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import java.util.function.Supplier
import niconicotagger.dto.inner.misc.SongType
import org.instancio.Instancio
import org.instancio.Select.root
import org.instancio.Select.types
import org.instancio.settings.Keys.MAP_MAX_SIZE
import org.instancio.settings.Keys.MAP_MIN_SIZE
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset.UTC

object Utils {
    val jsonMapper: JsonMapper =
        JsonMapper.builder()
            .findAndAddModules()
            .disable(WRITE_DATES_AS_TIMESTAMPS)
            .disable(FAIL_ON_UNKNOWN_PROPERTIES)
            .build()
    val xmlMapper: XmlMapper =
        XmlMapper.builder().findAndAddModules().addModule(JavaTimeModule()).disable(FAIL_ON_UNKNOWN_PROPERTIES).build()

    val eventPreviewFixedDate = LocalDate.of(2025, 5, 22)
    val eventPreviewMapperFixedClock: Clock = Clock.fixed(eventPreviewFixedDate.atStartOfDay().toInstant(UTC), UTC)

    fun loadResource(path: String) =
        Utils::class.java.classLoader.getResourceAsStream(path)?.readAllBytes() ?: error("Invalid path $path")

    fun createSampleSongTypeStats(songType: SongType?): Map<SongType, Int> {
        val baseApi =
            Instancio.ofMap(SongType::class.java, Int::class.java)
                .withSetting(MAP_MIN_SIZE, SongType.entries.size)
                .withSetting(MAP_MAX_SIZE, SongType.entries.size)
                .supply(types().of(Int::class.java), Supplier { 0 })
        return if (songType == null) baseApi.create()
        else baseApi.generate(root()) { gen -> gen.map<SongType, Int>().with(songType, 1) }.create()
    }

    fun createSampleSongTypeStats(songTypes: List<SongType>): Map<SongType, Int> {
        val result = SongType.entries.associateWith { _ -> 0 }.toMutableMap()
        songTypes.forEach { result.computeIfPresent(it) { _, v -> v + 1 } }
        return result
    }
}
