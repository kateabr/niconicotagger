package niconicotagger.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import java.time.Duration
import niconicotagger.client.DbClient
import niconicotagger.client.DbClientHolder
import niconicotagger.client.NndClient
import niconicotagger.dto.inner.misc.ReleaseEventCategory.AlbumRelease
import niconicotagger.dto.inner.misc.ReleaseEventCategory.Club
import niconicotagger.dto.inner.misc.ReleaseEventCategory.Concert
import niconicotagger.dto.inner.misc.ReleaseEventCategory.Convention
import niconicotagger.dto.inner.misc.ReleaseEventCategory.Other
import niconicotagger.mapper.NndVideoWithAssociatedVocaDbEntryMapper
import niconicotagger.mapper.QueryResponseMapper
import niconicotagger.mapper.ReleaseEventMapper
import niconicotagger.mapper.RequestMapper
import niconicotagger.mapper.SongWithPvsMapper
import org.instancio.junit.InstancioExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(InstancioExtension::class)
abstract class AggregatingServiceTest {
    val dbClient = mockk<DbClient>()
    val dbClientHolder = mockk<DbClientHolder>()
    val nndClient = mockk<NndClient>()
    val eventMapper = mockk<ReleaseEventMapper>()
    val songMapper = mockk<NndVideoWithAssociatedVocaDbEntryMapper>()
    val requestMapper = mockk<RequestMapper>()
    val queryResponseMapper = mockk<QueryResponseMapper>()
    val songWithPvsMapper = mockk<SongWithPvsMapper>()
    val publisherInfoService = mockk<PublisherInfoService>()
    val aggregatingService =
        spyk(
            AggregatingService(
                dbClientHolder,
                nndClient,
                eventMapper,
                songMapper,
                requestMapper,
                queryResponseMapper,
                songWithPvsMapper,
                publisherInfoService,
                setOf(AlbumRelease, Club, Concert, Convention, Other),
                defaultEventScope,
            )
        )

    @BeforeEach
    fun setup() {
        every { dbClientHolder.getClient(any()) } returns dbClient
    }

    companion object {
        val defaultEventScope: Duration = Duration.ofDays(14)
    }
}
