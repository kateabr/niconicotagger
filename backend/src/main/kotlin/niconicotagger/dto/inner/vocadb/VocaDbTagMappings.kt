package niconicotagger.dto.inner.vocadb

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import niconicotagger.serde.StringNormalizingDeserializer

data class VocaDbTagMappings(val items: List<VocaDbTagMapping>, val totalCount: Long)

data class VocaDbTagMapping(
    // normalized character width, for pretty display
    @JsonDeserialize(using = StringNormalizingDeserializer::class) val sourceTag: String,
    val tag: VocaDbTag,
)
