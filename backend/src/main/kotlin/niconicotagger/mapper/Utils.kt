package niconicotagger.mapper

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit.DAYS
import niconicotagger.dto.api.misc.DispositionRelativelyToDate
import niconicotagger.dto.api.misc.DispositionRelativelyToDate.Disposition.EARLY
import niconicotagger.dto.api.misc.DispositionRelativelyToDate.Disposition.LATE
import niconicotagger.dto.api.misc.DispositionRelativelyToDate.Disposition.PERFECT
import niconicotagger.dto.api.misc.EventDateBounds
import niconicotagger.dto.api.misc.FrontendColorCode.DANGER
import niconicotagger.dto.api.misc.FrontendColorCode.SUCCESS
import niconicotagger.dto.api.misc.FrontendColorCode.WARNING
import niconicotagger.dto.api.misc.SongEntryBase
import niconicotagger.dto.inner.misc.EventStatus
import niconicotagger.dto.inner.misc.EventStatus.ENDED
import niconicotagger.dto.inner.misc.EventStatus.ONGOING
import niconicotagger.dto.inner.misc.EventStatus.OUT_OF_RECENT_SCOPE
import niconicotagger.dto.inner.misc.EventStatus.UPCOMING
import niconicotagger.dto.inner.misc.ReleaseEventCategory
import niconicotagger.dto.inner.misc.ReleaseEventCategory.Unspecified
import niconicotagger.dto.inner.misc.SongType
import niconicotagger.dto.inner.vocadb.VocaDbReleaseEvent
import niconicotagger.dto.inner.vocadb.VocaDbReleaseEventSeries

object Utils {
    @JvmStatic
    fun <T : SongEntryBase> calculateSongStats(result: List<T>): Map<SongType, Int> {
        val songStats = result.groupBy { it.type }.mapValues { it.value.size }.toMutableMap()
        SongType.entries.forEach { songStats.putIfAbsent(it, 0) }
        return songStats
    }

    @JvmStatic
    fun calculateDisposition(publishDate: Instant, eventDates: EventDateBounds): DispositionRelativelyToDate {
        val daysFromBeginning = Duration.between(eventDates.from, publishDate).toDays()
        val daysToEnd = Duration.between(publishDate, eventDates.to ?: eventDates.from.plus(1, DAYS)).toDays()
        return if (daysFromBeginning >= 0 && daysToEnd >= 0) DispositionRelativelyToDate(PERFECT, 0, SUCCESS)
        else if (daysToEnd > 0) {
            DispositionRelativelyToDate(EARLY, -daysFromBeginning, if (-daysFromBeginning > 7) DANGER else WARNING)
        } else {
            DispositionRelativelyToDate(LATE, -daysToEnd, if (-daysToEnd > 7) DANGER else WARNING)
        }
    }

    @JvmStatic
    fun mapCategory(event: VocaDbReleaseEvent, series: VocaDbReleaseEventSeries?): ReleaseEventCategory =
        if (event.category == Unspecified && series != null) series.category else event.category

    @JvmStatic
    internal fun getEventStatus(event: VocaDbReleaseEvent, eventScope: Duration, clock: Clock): EventStatus {
        val today = clock.instant().truncatedTo(DAYS)
        if (
            !today.isBefore(requireNotNull(event.date)) && !today.isAfter(requireNotNull(event.endDate ?: event.date))
        ) {
            return ONGOING
        }
        if (Duration.between(event.endDate ?: event.date, today).toDays() in 0..eventScope.toDays()) {
            return ENDED
        }
        if (Duration.between(today, event.date).toDays() in 0..eventScope.toDays()) {
            return UPCOMING
        }
        return OUT_OF_RECENT_SCOPE
    }
}
