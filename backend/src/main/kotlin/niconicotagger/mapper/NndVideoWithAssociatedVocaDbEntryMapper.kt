package niconicotagger.mapper

import java.time.Instant
import niconicotagger.dto.api.misc.AvailableNndVideoWithAdditionalData
import niconicotagger.dto.api.misc.EventDateBounds
import niconicotagger.dto.api.misc.NndTagData
import niconicotagger.dto.api.misc.NndTagType
import niconicotagger.dto.api.misc.NndVideoWithAssociatedVocaDbEntryForEvent
import niconicotagger.dto.api.misc.NndVideoWithAssociatedVocaDbEntryForTag
import niconicotagger.dto.api.misc.ReleaseEvent
import niconicotagger.dto.api.misc.SongEntryWithTagAssignmentInfo
import niconicotagger.dto.inner.nnd.NndVideoData
import niconicotagger.dto.inner.vocadb.PublisherInfo
import niconicotagger.dto.inner.vocadb.VocaDbReleaseEvent
import niconicotagger.dto.inner.vocadb.VocaDbSongEntryWithTags
import niconicotagger.dto.inner.vocadb.VocaDbSongWithReleaseEvents
import niconicotagger.dto.inner.vocadb.VocaDbTagSelectable
import org.mapstruct.Context
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingConstants.ComponentModel.SPRING

@Mapper(componentModel = SPRING, imports = [Utils::class])
abstract class NndVideoWithAssociatedVocaDbEntryMapper {

    @Mapping(target = "video", expression = "java(mapVideo(video, videoTagTypes, description))")
    @Mapping(target = "entry", expression = "java(mapSongEntryForTag(songEntry, resultingTagSet))")
    @Mapping(target = "publisher", expression = "java(publisher)")
    abstract fun mapForTag(
        video: NndVideoData,
        songEntry: VocaDbSongEntryWithTags?,
        @Context videoTagTypes: Map<String, NndTagType>,
        @Context description: String?,
        @Context resultingTagSet: List<VocaDbTagSelectable>,
        @Context publisher: PublisherInfo?,
    ): NndVideoWithAssociatedVocaDbEntryForTag

    @Mapping(target = "video", expression = "java(mapVideo(video, videoTagTypes, description))")
    @Mapping(target = "publisher", expression = "java(publisher)")
    @Mapping(target = "disposition", expression = "java(Utils.calculateDisposition(publishDate, eventDates))")
    @Mapping(target = "publishedAt", expression = "java(publishDate)")
    abstract fun mapForEvent(
        video: NndVideoData,
        entry: VocaDbSongWithReleaseEvents?,
        @Context publishDate: Instant,
        @Context eventDates: EventDateBounds,
        @Context videoTagTypes: Map<String, NndTagType>,
        @Context description: String?,
        @Context publisher: PublisherInfo?,
    ): NndVideoWithAssociatedVocaDbEntryForEvent

    @Mapping(target = "description", expression = "java(description)")
    @Mapping(target = "tags", expression = "java(mapVideoTags(video, videoTagTypes))")
    protected abstract fun mapVideo(
        video: NndVideoData,
        @Context videoTagTypes: Map<String, NndTagType>,
        @Context description: String?,
    ): AvailableNndVideoWithAdditionalData

    @Mapping(target = "mappedTags", expression = "java(resultingTagSet)")
    protected abstract fun mapSongEntryForTag(
        songEntry: VocaDbSongEntryWithTags?,
        @Context resultingTagSet: List<VocaDbTagSelectable>,
    ): SongEntryWithTagAssignmentInfo?

    protected abstract fun mapReleaseEvent(releaseEvent: VocaDbReleaseEvent, seriesId: Long?): ReleaseEvent

    protected fun mapVideoTags(video: NndVideoData, videoTags: Map<String, NndTagType>): List<NndTagData> {
        return video.tags.map { NndTagData(it, requireNotNull(videoTags[it]), false) }
    }
}
