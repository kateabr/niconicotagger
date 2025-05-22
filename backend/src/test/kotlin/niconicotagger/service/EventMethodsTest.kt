package niconicotagger.service

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyAll
import io.mockk.coVerifyCount
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockkClass
import io.mockk.verifyAll
import io.mockk.verifyCount
import java.time.Duration
import java.time.OffsetDateTime
import kotlinx.coroutines.runBlocking
import niconicotagger.dto.api.misc.ClientType
import niconicotagger.dto.api.request.EventScheduleRequest
import niconicotagger.dto.api.request.GetReleaseEventRequest
import niconicotagger.dto.api.response.ReleaseEventPreviewResponse
import niconicotagger.dto.api.response.ReleaseEventWithVocaDbTagsResponse
import niconicotagger.dto.api.response.ReleaseEventWitnNndTagsResponse
import niconicotagger.dto.inner.misc.ReleaseEventCategory
import niconicotagger.dto.inner.misc.WebLink
import niconicotagger.dto.inner.vocadb.VocaDbFrontPageData
import niconicotagger.dto.inner.vocadb.VocaDbReleaseEvent
import niconicotagger.dto.inner.vocadb.VocaDbReleaseEventSeries
import org.assertj.core.api.Assertions.assertThat
import org.instancio.Instancio
import org.instancio.Select.field
import org.instancio.junit.Given
import org.instancio.junit.InstancioExtension
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.test.util.ReflectionTestUtils

@ExtendWith(InstancioExtension::class)
class EventMethodsTest : AggregatingServiceTest() {

    @AfterEach
    fun confirm() {
        verifyAll { dbClientHolder.getClient(any()) }
    }

    @ParameterizedTest
    @ValueSource(longs = [1])
    @NullSource
    fun `get event by name test`(seriesId: Long?, @Given eventName: String, @Given clientType: ClientType): Unit =
        runBlocking {
            val resultPlaceholder = mockkClass(ReleaseEventWitnNndTagsResponse::class)
            every { resultPlaceholder.nndTags } returns Instancio.createList(String::class.java)
            coEvery { dbClient.getEventByName(eq(eventName), eq("WebLinks")) } returns
                Instancio.of(VocaDbReleaseEvent::class.java).set(field("seriesId"), seriesId).create()
            if (seriesId != null) {
                coEvery { dbClient.getEventSeriesById(eq(seriesId), eq("WebLinks")) } returns
                    Instancio.create(VocaDbReleaseEventSeries::class.java)
            }
            every { eventMapper.mapWithLinks(any(), any(VocaDbReleaseEventSeries::class)) } returns resultPlaceholder

            assertThat(aggregatingService.getReleaseEventByName(GetReleaseEventRequest(eventName, clientType)))
                .isEqualTo(resultPlaceholder)

            coVerify { dbClient.getEventByName(any(), any()) }
            coVerifyCount { (if (seriesId == null) 0 else 1) * { dbClient.getEventSeriesById(any(), any()) } }
            confirmVerified(dbClient)
            verifyAll { eventMapper.mapWithLinks(any(), any(VocaDbReleaseEventSeries::class)) }
        }

    @ParameterizedTest
    @ValueSource(longs = [1])
    @NullSource
    fun `get event with linked tag test`(
        seriesId: Long?,
        @Given eventName: String,
        @Given eventSeries: VocaDbReleaseEventSeries,
        @Given vocaDbTagId: Long,
        @Given clientType: ClientType,
    ): Unit = runBlocking {
        val releaseEvent =
            Instancio.of(VocaDbReleaseEvent::class.java)
                .set(field("seriesId"), seriesId)
                .set(field("webLinks"), listOf(WebLink("https://beta.vocadb.net/T/$vocaDbTagId")))
                .create()
        val resultPlaceholder = mockkClass(ReleaseEventWithVocaDbTagsResponse::class)
        coEvery { dbClient.getEventByName(eq(eventName), eq("Tags")) } returns releaseEvent
        if (seriesId != null) {
            coEvery { dbClient.getEventSeriesById(eq(seriesId)) } returns eventSeries
        }
        every { eventMapper.mapWithTags(eq(releaseEvent), seriesId?.let { eq(eventSeries) } ?: isNull()) } returns
            resultPlaceholder

        assertThat(aggregatingService.getReleaseEventWithLinkedTags(GetReleaseEventRequest(eventName, clientType)))
            .isEqualTo(resultPlaceholder)

        coVerify { dbClient.getEventByName(any(), any()) }
        coVerifyCount { (if (seriesId != null) 1 else 0) * { dbClient.getEventSeriesById(any()) } }
        confirmVerified(dbClient)
        verifyAll { eventMapper.mapWithTags(any(), any()) }
    }

    @Test
    fun `get recent events test`(@Given request: EventScheduleRequest): Unit = runBlocking {
        val uniqueIds = mutableSetOf<Long>()
        val outOfScopeEvent =
            Instancio.of(VocaDbReleaseEvent::class.java)
                .set(field("date"), OffsetDateTime.now().plusMonths(1).toInstant())
                .withNullable(field("endDate"))
                .filter<Long>(field("id")) { uniqueIds.add(it) }
                .create()
        val regularEvent =
            Instancio.of(VocaDbReleaseEvent::class.java)
                .set(field("date"), OffsetDateTime.now().toInstant())
                .withNullable(field("endDate"))
                .filter<Long>(field("id")) { uniqueIds.add(it) }
                .create()
        val frontPageEvent =
            Instancio.of(VocaDbReleaseEvent::class.java)
                .set(field("date"), OffsetDateTime.now().toInstant())
                .withNullable(field("endDate"))
                .filter<Long>(field("id")) { uniqueIds.add(it) }
                .create()
        val allEventsForPreview = listOf(outOfScopeEvent, regularEvent, frontPageEvent)
        coEvery { dbClient.getAllEventsForYear(eq(request.useCached)) } returns listOf(outOfScopeEvent, regularEvent)
        coEvery { dbClient.getFrontPageData() } returns VocaDbFrontPageData(listOf(frontPageEvent, regularEvent))
        every {
            eventMapper.mapForPreview(
                match { allEventsForPreview.map { event -> event.id }.contains(it.id) },
                eq(ReflectionTestUtils.getField(aggregatingService, "eventScope") as Duration),
                eq(ReflectionTestUtils.getField(aggregatingService, "offlineEvents") as Set<ReleaseEventCategory>),
            )
        } returns Instancio.create(ReleaseEventPreviewResponse::class.java)

        assertThat(aggregatingService.getRecentEvents(request)).hasSameSizeAs(allEventsForPreview)

        coVerifyAll {
            dbClient.getAllEventsForYear(any())
            dbClient.getFrontPageData()
            aggregatingService.getRecentEvents(any())
        }
        verifyCount { allEventsForPreview.size * { eventMapper.mapForPreview(any(), any(), any()) } }
    }
}
