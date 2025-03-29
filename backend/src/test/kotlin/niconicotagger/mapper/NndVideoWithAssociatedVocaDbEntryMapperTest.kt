package niconicotagger.mapper

import niconicotagger.dto.api.misc.AvailableNndVideoWithAdditionalData
import niconicotagger.dto.api.misc.DispositionRelativelyToDate
import niconicotagger.dto.api.misc.DispositionRelativelyToDate.Disposition.EARLY
import niconicotagger.dto.api.misc.DispositionRelativelyToDate.Disposition.LATE
import niconicotagger.dto.api.misc.DispositionRelativelyToDate.Disposition.PERFECT
import niconicotagger.dto.api.misc.EventDateBounds
import niconicotagger.dto.api.misc.FrontendColorCode.DANGER
import niconicotagger.dto.api.misc.FrontendColorCode.SUCCESS
import niconicotagger.dto.api.misc.FrontendColorCode.WARNING
import niconicotagger.dto.api.misc.NndTagData
import niconicotagger.dto.api.misc.NndTagType
import niconicotagger.dto.api.misc.NndVideoWithAssociatedVocaDbEntryForEvent
import niconicotagger.dto.api.misc.NndVideoWithAssociatedVocaDbEntryForTag
import niconicotagger.dto.api.misc.ReleaseEvent
import niconicotagger.dto.api.misc.SongEntryWithReleaseEventInfo
import niconicotagger.dto.api.misc.SongEntryWithTagAssignmentInfo
import niconicotagger.dto.inner.nnd.NndVideoData
import niconicotagger.dto.inner.vocadb.PublisherInfo
import niconicotagger.dto.inner.vocadb.VocaDbSongEntryWithTags
import niconicotagger.dto.inner.vocadb.VocaDbSongWithReleaseEvents
import niconicotagger.dto.inner.vocadb.VocaDbTag
import niconicotagger.dto.inner.vocadb.VocaDbTagSelectable
import org.assertj.core.api.Assertions.assertThat
import org.instancio.Instancio
import org.instancio.Select.all
import org.instancio.Select.field
import org.instancio.Select.root
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import org.mapstruct.factory.Mappers
import java.time.Instant
import java.time.temporal.ChronoUnit.DAYS
import java.util.stream.Stream

class NndVideoWithAssociatedVocaDbEntryMapperTest {
    private val mapper: NndVideoWithAssociatedVocaDbEntryMapper =
        Mappers.getMapper(NndVideoWithAssociatedVocaDbEntryMapper::class.java)

    @Test
    fun `NND video with associated VocaDB entry mapping test (for tagging)`() {
        val nndTagMappings = Instancio.createMap(String::class.java, VocaDbTag::class.java)
        val video = Instancio.of(NndVideoData::class.java)
            .ignore(all(field("userId"), field("description")))
            .set(field("tags"), nndTagMappings.keys.toList())
            .create()
        val songEntry = Instancio.of(VocaDbSongEntryWithTags::class.java)
            .withNullable(root())
            .setBlank(field("tags"))
            .create()
        val videoTags = Instancio.ofMap(String::class.java, NndTagType::class.java)
            .generate(root()) { gen ->
                gen.map<String, NndTagType>().withKeys(*video.tags.toTypedArray()).size(video.tags.size)
            }
            .create()
        val description = Instancio.create(String::class.java)
        val publisher = Instancio.of(PublisherInfo::class.java)
            .withNullable(root())
            .create()
        val resultingTagSet = Instancio.createList(VocaDbTagSelectable::class.java)

        assertThat(mapper.mapForTag(video, songEntry, videoTags, description, resultingTagSet, publisher))
            .usingRecursiveComparison()
            .isEqualTo(
                NndVideoWithAssociatedVocaDbEntryForTag(
                    AvailableNndVideoWithAdditionalData(
                        video.id,
                        video.title,
                        description,
                        videoTags.entries.map { NndTagData(it.key, it.value, false) },
                        video.length,
                        video.viewCounter,
                        video.publishedAt,
                        video.likeCounter
                    ),
                    songEntry?.let {
                        SongEntryWithTagAssignmentInfo(
                            songEntry.id,
                            songEntry.name,
                            songEntry.type,
                            songEntry.artistString,
                            songEntry.publishedAt,
                            resultingTagSet
                        )
                    },
                    publisher
                )
            )
    }

    @ParameterizedTest
    @ArgumentsSource(TestData::class)
    fun `NND video with associated VocaDB entry mapping test (for event)`(
        nndTagMappings: Map<String, VocaDbTag>,
        video: NndVideoData,
        songEntry: VocaDbSongWithReleaseEvents?,
        publishDate: Instant,
        eventDateBounds: EventDateBounds,
        videoTagTypes: Map<String, NndTagType>,
        description: String?,
        publisher: PublisherInfo?,
        expectedObject: NndVideoWithAssociatedVocaDbEntryForEvent
    ) {
        assertThat(
            mapper.mapForEvent(
                video,
                songEntry,
                publishDate,
                eventDateBounds,
                videoTagTypes,
                description,
                publisher
            )
        )
            .usingRecursiveComparison()
            .isEqualTo(expectedObject)
    }

    companion object {
        class TestData : ArgumentsProvider {
            private fun perfect(
                publishDate: Instant,
                oneDayEvent: Boolean
            ): Pair<EventDateBounds, DispositionRelativelyToDate> {
                return if (oneDayEvent)
                    EventDateBounds(
                        publishDate.truncatedTo(DAYS),
                        null,
                        true
                    ) to DispositionRelativelyToDate(PERFECT, 0, SUCCESS)
                else
                    EventDateBounds(
                        publishDate.truncatedTo(DAYS),
                        publishDate.truncatedTo(DAYS).plus(3, DAYS),
                        true
                    ) to DispositionRelativelyToDate(PERFECT, 0, SUCCESS)
            }

            private fun early(
                publishDate: Instant,
                days: Long,
                oneDayEvent: Boolean
            ): Pair<EventDateBounds, DispositionRelativelyToDate> {
                return if (oneDayEvent)
                    EventDateBounds(
                        // adjusting by 1 day because of the date truncation
                        publishDate.truncatedTo(DAYS).plus(days + 1, DAYS),
                        publishDate.truncatedTo(DAYS).plus(days + 2, DAYS),
                        true
                    ) to DispositionRelativelyToDate(EARLY, days, if (days > 7) DANGER else WARNING)
                else
                    EventDateBounds(
                        // adjusting by 1 day because of the date truncation
                        publishDate.truncatedTo(DAYS).plus(days + 1, DAYS),
                        publishDate.truncatedTo(DAYS).plus(days + 4, DAYS),
                        true
                    ) to DispositionRelativelyToDate(EARLY, days, if (days > 7) DANGER else WARNING)
            }

            private fun late(
                publishDate: Instant,
                days: Long,
                oneDayEvent: Boolean
            ): Pair<EventDateBounds, DispositionRelativelyToDate> {
                return if (oneDayEvent)
                    EventDateBounds(
                        publishDate.truncatedTo(DAYS).minus(days + 1, DAYS),
                        publishDate.truncatedTo(DAYS).minus(days, DAYS),
                        true
                    ) to DispositionRelativelyToDate(LATE, days, if (days > 7) DANGER else WARNING)
                else
                    EventDateBounds(
                        publishDate.truncatedTo(DAYS).minus(days + 3, DAYS),
                        publishDate.truncatedTo(DAYS).minus(days, DAYS),
                        true
                    ) to DispositionRelativelyToDate(LATE, days, if (days > 7) DANGER else WARNING)
            }

            override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
                val nndTagMappings = Instancio.createMap(String::class.java, VocaDbTag::class.java)
                val video = Instancio.of(NndVideoData::class.java)
                    .ignore(all(field("userId"), field("description")))
                    .set(field("tags"), nndTagMappings.keys.toList())
                    .create()
                val songEntry = Instancio.of(VocaDbSongWithReleaseEvents::class.java)
                    .withNullable(root())
                    .create()
                val publishDate = Instancio.create(Instant::class.java)
                val videoTagTypes = video.tags.associateWith { Instancio.create(NndTagType::class.java) }
                val description = Instancio.of(String::class.java)
                    .withNullable(root())
                    .create()
                val publisher =
                    if (songEntry == null) Instancio.of(PublisherInfo::class.java)
                        .withNullable(root())
                        .create()
                    else null

                return listOf(true, false)
                    .flatMap {
                        listOf(
                            early(publishDate, 8, it) to if (it) "one-day event" else "several-day event",
                            early(publishDate, 7, it) to if (it) "one-day event" else "several-day event",
                            perfect(publishDate, it) to if (it) "one-day event" else "several-day event",
                            late(publishDate, 7, it) to if (it) "one-day event" else "several-day event",
                            late(publishDate, 8, it) to if (it) "one-day event" else "several-day event"
                        )
                    }
                    .map {
                        argumentSet(
                            "${it.second}, ${it.first.second}",
                            nndTagMappings,
                            video,
                            songEntry,
                            publishDate,
                            it.first.first,
                            videoTagTypes,
                            description,
                            publisher,
                            NndVideoWithAssociatedVocaDbEntryForEvent(
                                AvailableNndVideoWithAdditionalData(
                                    video.id,
                                    video.title,
                                    description,
                                    video.tags.map { NndTagData(it, requireNotNull(videoTagTypes[it]), false) },
                                    video.length,
                                    video.viewCounter,
                                    video.publishedAt,
                                    video.likeCounter
                                ),
                                songEntry?.let { entry ->
                                    SongEntryWithReleaseEventInfo(
                                        entry.id,
                                        entry.name,
                                        entry.type,
                                        entry.artistString,
                                        entry.events.map { event ->
                                            ReleaseEvent(
                                                event.id,
                                                event.name,
                                                event.seriesId
                                            )
                                        })
                                },
                                publisher,
                                publishDate,
                                it.first.second
                            )
                        )
                    }.stream()
            }

        }

    }
}
