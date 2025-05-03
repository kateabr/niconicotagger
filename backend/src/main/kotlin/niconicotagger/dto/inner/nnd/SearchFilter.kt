package niconicotagger.dto.inner.nnd

import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import niconicotagger.dto.inner.nnd.SearchFilterType.AND
import niconicotagger.dto.inner.nnd.SearchFilterType.EQUAL
import niconicotagger.dto.inner.nnd.SearchFilterType.OR
import niconicotagger.dto.inner.nnd.SearchFilterType.RANGE

interface SearchFilter {
    val type: SearchFilterType
}

@JsonNaming(SnakeCaseStrategy::class)
data class RangeFilter<T>(
    val field: String,
    val from: T,
    val to: T,
    val includeLower: Boolean,
    val includeUpper: Boolean,
) : SearchFilter {
    override val type = RANGE
}

data class EqualFilter<T>(val field: String, val value: T?) : SearchFilter {
    override val type = EQUAL
}

data class AndFilter(val filters: List<SearchFilter>) : SearchFilter {
    override val type = AND
}

data class OrFilter(val filters: List<SearchFilter>) : SearchFilter {
    override val type = OR
}

enum class SearchFilterType {
    EQUAL,
    RANGE,
    OR,
    AND;

    @JsonValue override fun toString() = name.lowercase()
}
