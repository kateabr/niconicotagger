package niconicotagger.serde

import java.time.Instant
import net.javacrumbs.jsonunit.assertj.assertThatJson
import niconicotagger.Utils.jsonMapper
import niconicotagger.dto.inner.nnd.AndFilter
import niconicotagger.dto.inner.nnd.EqualFilter
import niconicotagger.dto.inner.nnd.OrFilter
import niconicotagger.dto.inner.nnd.RangeFilter
import org.instancio.Instancio
import org.junit.jupiter.api.Test

class JsonFilterSerializationTest {
    @Test
    fun `json filter serialization test`() {
        val orFilters =
            listOf(
                EqualFilter(Instancio.create(String::class.java), Instancio.create(String::class.java)),
                EqualFilter(Instancio.create(String::class.java), Instancio.create(Long::class.java)),
                EqualFilter(Instancio.create(String::class.java), Instancio.create(Instant::class.java)),
            )
        val rangeFilter =
            RangeFilter(
                Instancio.create(String::class.java),
                Instancio.create(Instant::class.java),
                Instancio.create(Instant::class.java),
                Instancio.create(Boolean::class.java),
                Instancio.create(Boolean::class.java),
            )

        assertThatJson(jsonMapper.writeValueAsString(AndFilter(listOf(OrFilter(orFilters), rangeFilter))))
            .isEqualTo(
                """
                {
                  "filters": [
                    {
                      "filters": [
                        {
                          "field": "${orFilters[0].field}",
                          "value": "${orFilters[0].value}",
                          "type": "equal"
                        },
                        {
                          "field": "${orFilters[1].field}",
                          "value": ${orFilters[1].value},
                          "type": "equal"
                        },
                        {
                          "field": "${orFilters[2].field}",
                          "value": "${orFilters[2].value}",
                          "type": "equal"
                        }
                      ],
                      "type": "or"
                    },
                    {
                      "field": "${rangeFilter.field}",
                      "from": "${rangeFilter.from}",
                      "to": "${rangeFilter.to}",
                      "include_lower": ${rangeFilter.includeLower},
                      "include_upper": ${rangeFilter.includeUpper},
                      "type": "range"
                    }
                  ],
                  "type": "and"
                }
                """
                    .trimIndent()
            )
    }
}
