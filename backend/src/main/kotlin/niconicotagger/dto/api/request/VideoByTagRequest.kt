package niconicotagger.dto.api.request

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import niconicotagger.dto.api.misc.ClientType
import niconicotagger.dto.api.misc.EventDateBounds
import niconicotagger.dto.api.misc.NndSortOrder
import niconicotagger.serde.NndScopeNormalizingDeserializer
import niconicotagger.serde.StringNormalizingDeserializer

sealed interface VideoByTagRequest {
    val scope: String
    val startOffset: Long
    val maxResults: Long
    val orderBy: NndSortOrder
    val clientType: ClientType
}

sealed interface VideosByNndTagsRequestBase : VideoByTagRequest {
    override val scope: String
    override val startOffset: Long
    override val maxResults: Long
    override val orderBy: NndSortOrder
    override val clientType: ClientType
    val tags: Set<String>

    fun joinTags() = tags.joinToString(" OR ", "", " $scope")
}

@JsonIgnoreProperties(ignoreUnknown = false)
data class VideosByNndTagsRequest(
    @JsonDeserialize(contentUsing = StringNormalizingDeserializer::class)
    @field:NotEmpty
    override val tags: Set<String>,
    @JsonDeserialize(using = NndScopeNormalizingDeserializer::class) override val scope: String,
    @field:Min(0) override val startOffset: Long,
    @field:Min(10) @field:Max(100) override val maxResults: Long,
    override val orderBy: NndSortOrder,
    override val clientType: ClientType,
) : VideosByNndTagsRequestBase

@JsonIgnoreProperties(ignoreUnknown = false)
data class VideosByNndEventTagsRequest(
    @JsonDeserialize(contentUsing = StringNormalizingDeserializer::class)
    @field:NotEmpty
    override val tags: Set<String>,
    @JsonDeserialize(using = NndScopeNormalizingDeserializer::class) override val scope: String,
    @field:Min(0) override val startOffset: Long,
    @field:Min(10) @field:Max(100) override val maxResults: Long,
    override val orderBy: NndSortOrder,
    val dates: EventDateBounds,
    override val clientType: ClientType,
) : VideosByNndTagsRequestBase

@JsonIgnoreProperties(ignoreUnknown = false)
data class VideosByVocaDbTagRequest(
    @JsonDeserialize(using = StringNormalizingDeserializer::class) @field:NotBlank val tag: String,
    @JsonDeserialize(using = NndScopeNormalizingDeserializer::class) override val scope: String,
    @field:Min(0) override val startOffset: Long,
    @field:Min(10) @field:Max(100) override val maxResults: Long,
    override val orderBy: NndSortOrder,
    override val clientType: ClientType,
) : VideoByTagRequest
