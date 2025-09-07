package niconicotagger.service

import com.sksamuel.aedile.core.Cache
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyAll
import io.mockk.coVerifyCount
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.verifyAll
import io.mockk.verifyCount
import java.time.Duration
import java.time.OffsetDateTime
import java.util.function.Consumer
import java.util.stream.Stream
import kotlinx.coroutines.runBlocking
import niconicotagger.Utils.clientSpecificDbTagProps
import niconicotagger.Utils.mockPrivateField
import niconicotagger.client.DbClient
import niconicotagger.dto.api.misc.ClientType
import niconicotagger.dto.api.request.EventScheduleRequest
import niconicotagger.dto.api.request.GetReleaseEventRequest
import niconicotagger.dto.api.response.ReleaseEventPreview
import niconicotagger.dto.api.response.ReleaseEventWithVocaDbTagsResponse
import niconicotagger.dto.api.response.ReleaseEventWitnNndTagsResponse
import niconicotagger.dto.inner.misc.EntryField.Tags
import niconicotagger.dto.inner.misc.EntryField.WebLinks
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
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.ArgumentSet
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.MethodSource
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
            coEvery { dbClient.getEventByName(eq(eventName), eq(WebLinks)) } returns
                Instancio.of(VocaDbReleaseEvent::class.java).set(field("seriesId"), seriesId).create()
            if (seriesId != null) {
                coEvery { dbClient.getEventSeriesById(eq(seriesId), eq(WebLinks)) } returns
                    Instancio.create(VocaDbReleaseEventSeries::class.java)
            }
            every { eventMapper.mapWithLinks(any(), any(VocaDbReleaseEventSeries::class)) } returns resultPlaceholder

            assertThat(aggregatingService.getReleaseEventByName(GetReleaseEventRequest(eventName, clientType)))
                .isEqualTo(resultPlaceholder)

            coVerify { dbClient.getEventByName(any(), *anyVararg()) }
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
        coEvery { dbClient.getEventByName(eq(eventName), eq(Tags)) } returns releaseEvent
        if (seriesId != null) {
            coEvery { dbClient.getEventSeriesById(eq(seriesId)) } returns eventSeries
        }
        every { eventMapper.mapWithTags(eq(releaseEvent), seriesId?.let { eq(eventSeries) } ?: isNull()) } returns
            resultPlaceholder

        assertThat(aggregatingService.getReleaseEventWithLinkedTags(GetReleaseEventRequest(eventName, clientType)))
            .isEqualTo(resultPlaceholder)

        coVerify { dbClient.getEventByName(any(), *anyVararg()) }
        coVerifyCount { (if (seriesId != null) 1 else 0) * { dbClient.getEventSeriesById(any()) } }
        confirmVerified(dbClient)
        verifyAll { eventMapper.mapWithTags(any(), any()) }
    }

    @ParameterizedTest
    @MethodSource("getRecentEventsTestData")
    fun `get recent events test`(request: EventScheduleRequest, eventScope: Duration): Unit = runBlocking {
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
        val endlessEvent1 =
            Instancio.of(VocaDbReleaseEvent::class.java)
                .set(field("date"), OffsetDateTime.now().toInstant())
                .withNullable(field("endDate"))
                .filter<Long>(field("id")) { uniqueIds.add(it) }
                .create()
        val endlessEvent2 =
            Instancio.of(VocaDbReleaseEvent::class.java)
                .set(field("date"), OffsetDateTime.now().toInstant())
                .withNullable(field("endDate"))
                .filter<Long>(field("id")) { uniqueIds.add(it) }
                .create()
        val allEventsForPreview = listOf(outOfScopeEvent, regularEvent, frontPageEvent, endlessEvent1, endlessEvent2)
        mockEventScheduleCache(dbClient)
        coEvery {
            dbClient.getReleaseEventSchedule(eq(request.useCached), eq(clientSpecificDbTagProps.endlessEvent.id))
        } answers { callOriginal() }
        coEvery { dbClient.getAllEventsForYear() } returns listOf(outOfScopeEvent, regularEvent, endlessEvent1)
        coEvery { dbClient.getFrontPageData() } returns VocaDbFrontPageData(listOf(frontPageEvent, regularEvent))
        coEvery { dbClient.getEndlessEvents(eq(clientSpecificDbTagProps.endlessEvent.id)) } returns
            listOf(endlessEvent1, endlessEvent2)
        every {
            eventMapper.mapForPreview(
                match { allEventsForPreview.map { event -> event.id }.contains(it.id) },
                eq(eventScope),
                eq(ReflectionTestUtils.getField(aggregatingService, "offlineEvents") as Set<ReleaseEventCategory>),
                any(),
            )
        } returns Instancio.create(ReleaseEventPreview::class.java)

        assertThat(aggregatingService.getRecentEvents(request))
            .satisfies(Consumer { assertThat(it.eventPreviews).hasSameSizeAs(allEventsForPreview) })
            .satisfies(Consumer { assertThat(it.eventScopeDays).isEqualTo(eventScope.toDays()) })

        coVerifyAll {
            dbClient.getReleaseEventSchedule(any(), any())
            dbClient.getAllEventsForYear()
            dbClient.getFrontPageData()
            dbClient.getEndlessEvents(any())
            aggregatingService.getRecentEvents(any())
        }
        verifyCount { allEventsForPreview.size * { eventMapper.mapForPreview(any(), any(), any(), any()) } }
    }

    companion object {
        @JvmStatic
        fun getRecentEventsTestData(): Stream<ArgumentSet> {
            val requestWithScope = Instancio.create(EventScheduleRequest::class.java)
            return Stream.of(
                argumentSet(
                    "scope specified in request",
                    requestWithScope,
                    Duration.ofDays(requestWithScope.eventScopeDays!!),
                ),
                argumentSet(
                    "scope not specified in request",
                    Instancio.of(EventScheduleRequest::class.java).ignore(field("eventScopeDays")).create(),
                    defaultEventScope,
                ),
            )
        }

        private fun mockEventScheduleCache(dbClient: DbClient) {
            val mockedCache = mockk<Cache<String, EventScheduleRequest>>()
            dbClient.mockPrivateField("eventScheduleCache", mockedCache)
            coEvery { mockedCache.getIfPresent(any()) } returns null
            coEvery { mockedCache.put(any(), any(EventScheduleRequest::class)) } returnsArgument 1
        }
    }
}
