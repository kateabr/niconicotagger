package niconicotagger.dto.api.response

sealed interface ApiResponse<T> {
    val items: List<T>
    val totalCount: Long
}
