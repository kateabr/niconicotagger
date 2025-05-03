package niconicotagger.serde

import niconicotagger.Utils.jsonMapper
import niconicotagger.dto.inner.vocadb.VocaDbTagMapping
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MiscDeserializationTest {
    @Test
    fun `VocaDbTagMapping$sourceTag normalization test`() {
        assertThat(
                jsonMapper.readValue(
                    """
                {
                  "sourceTag": "ｱアあＡA１1",
                  "tag": {
                    "id": 1,
                    "name": "name"
                  }
                }
                """
                        .trimIndent(),
                    VocaDbTagMapping::class.java,
                )
            )
            .extracting { it.sourceTag }
            .isEqualTo("アアあAA11")
    }
}
