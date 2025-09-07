package niconicotagger.dto.inner.misc

enum class EventStatus(val priority: Long) {
    ENDED(1),
    ONGOING(0),
    UPCOMING(2),
    ENDLESS(3),
    OUT_OF_RECENT_SCOPE(100),
}
