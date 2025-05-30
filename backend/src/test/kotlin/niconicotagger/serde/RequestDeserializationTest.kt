package niconicotagger.serde

import com.fasterxml.jackson.databind.ObjectMapper
import java.time.Instant
import niconicotagger.dto.api.misc.ApiType.ARTISTS
import niconicotagger.dto.api.misc.ApiType.SONGS
import niconicotagger.dto.api.misc.ClientType.VOCADB
import niconicotagger.dto.api.misc.ClientType.VOCADB_BETA
import niconicotagger.dto.api.misc.EventDateBounds
import niconicotagger.dto.api.misc.NndSortOrder.VIEW_COUNT
import niconicotagger.dto.api.misc.PvToDisable
import niconicotagger.dto.api.misc.ReleaseEvent
import niconicotagger.dto.api.misc.VocaDbSortOrder.AdditionDate
import niconicotagger.dto.api.request.AddReleaseEventRequest
import niconicotagger.dto.api.request.DeleteTagsRequest
import niconicotagger.dto.api.request.DeleteTagsRequestWrapper
import niconicotagger.dto.api.request.EventScheduleRequest
import niconicotagger.dto.api.request.MassAddReleaseEventRequest
import niconicotagger.dto.api.request.QueryConsoleRequest
import niconicotagger.dto.api.request.SongTagsAndPvsMassUpdateRequest
import niconicotagger.dto.api.request.SongTagsAndPvsUpdateRequest
import niconicotagger.dto.api.request.SongsWithPvsRequest
import niconicotagger.dto.api.request.VideosByNndEventTagsRequest
import niconicotagger.dto.api.request.VideosByNndTagsRequest
import niconicotagger.dto.api.request.VideosByVocaDbTagRequest
import niconicotagger.dto.inner.vocadb.VocaDbTag
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.FieldSource

class RequestDeserializationTest {

    private val strictJsonMapper = ObjectMapper().findAndRegisterModules()

    @ParameterizedTest(name = "{0}")
    @FieldSource("testData")
    fun <T> `request deserialization test`(objectClass: Class<T>, json: String, expectedObject: T) {
        assertThat(strictJsonMapper.readValue(json, objectClass))
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .isEqualTo(expectedObject)
    }

    companion object {
        val testData =
            listOf(
                arguments(
                    MassAddReleaseEventRequest::class.java,
                    """
                {
                    "subRequests": [
                        {
                            "entryId": 1,
                            "event": {
                                "id": 1,
                                "name": "name"
                            }
                        }
                    ],
                    "clientType": "vocadb"
                }
                """
                        .trimIndent(),
                    MassAddReleaseEventRequest(listOf(AddReleaseEventRequest(1, ReleaseEvent(1, "name", null))), VOCADB),
                ),
                arguments(
                    DeleteTagsRequestWrapper::class.java,
                    """
                {
                    "request": {
                        "apiType": "songs",
                        "entryId": 1,
                        "tags": [
                            {
                                "id": 1,
                                "name": "1"
                            },
                            {
                                "id": 2,
                                "name": "2"
                            }
                        ]
                    },
                    "clientType": "vocadb_beta"
                }
                """
                        .trimIndent(),
                    DeleteTagsRequestWrapper(
                        DeleteTagsRequest(SONGS, 1, listOf(VocaDbTag(1, "1"), VocaDbTag(2, "2"))),
                        VOCADB_BETA,
                    ),
                ),
                arguments(
                    QueryConsoleRequest::class.java,
                    """
                {
                    "apiType": "artists",
                    "query": "getTotalCount=true&maxResults=1&tagId[]=158&tagId[]=3353&advancedFilters[0][filterType]=HasMedia&advancedFilters[0][negate]=true",
                    "clientType": "vocadb"
                }
                """
                        .trimIndent(),
                    QueryConsoleRequest(
                        ARTISTS,
                        "getTotalCount=true&maxResults=1&tagId[]=158&tagId[]=3353&advancedFilters[0][filterType]=HasMedia&advancedFilters[0][negate]=true",
                        VOCADB,
                    ),
                ),
                arguments(
                    SongsWithPvsRequest::class.java,
                    """
                {
                    "startOffset": 0,
                    "maxResults": 10,
                    "orderBy": "AdditionDate",
                    "clientType": "vocadb"
                }
                """
                        .trimIndent(),
                    SongsWithPvsRequest(0, 10, AdditionDate, VOCADB),
                ),
                arguments(
                    SongTagsAndPvsMassUpdateRequest::class.java,
                    """
                {
                    "subRequests": [
                        {
                            "songId": 1,
                            "pvId": "1",
                            "tags": [
                                1
                            ],
                            "nndPvsToDisable": [
                                {
                                    "id": "id",
                                    "reason": "DELETED"
                                }
                            ]
                        }
                    ],
                    "clientType": "vocadb"
                }
                """
                        .trimIndent(),
                    SongTagsAndPvsMassUpdateRequest(
                        listOf(SongTagsAndPvsUpdateRequest(1, "1", listOf(1), setOf(PvToDisable("id", "DELETED")))),
                        VOCADB,
                    ),
                ),
                arguments(
                    VideosByNndTagsRequest::class.java,
                    """
                {
                    "tags": [
                        "ｱアあＡAａa１1",
                        "tag"
                    ],
                    "scope": "  OR ｲイいＩIｉi１1 ｳウうＵUｕu１1 OR ｴエえＥEｅe１1   ",
                    "startOffset": 0,
                    "maxResults": 100,
                    "orderBy": "viewCounter",
                    "clientType": "vocadb_beta"
                }
                """
                        .trimIndent(),
                    VideosByNndTagsRequest(
                        setOf("あああaaaa11", "tag"),
                        "イイいIIii11 ウウうUUuu11 OR エエえEEee11",
                        0,
                        100,
                        VIEW_COUNT,
                        VOCADB_BETA,
                    ),
                ),
                arguments(
                    VideosByNndEventTagsRequest::class.java,
                    """
                {
                    "tags": [
                        "ｱアあＡAａa１1",
                        "tag"
                    ],
                    "scope": "  OR ｲイいＩIｉi１1 ｳウうＵUｕu１1 OR ｴエえＥEｅe１1   ",
                    "startOffset": 0,
                    "maxResults": 100,
                    "orderBy": "viewCounter",
                    "dates": {
                        "from": "2014-01-01",
                        "to": "2014-01-02",
                        "applyToSearch": true
                    },
                    "clientType": "vocadb_beta"
                }
                """
                        .trimIndent(),
                    VideosByNndEventTagsRequest(
                        setOf("アアあAAaa11", "tag"),
                        "イイいIIii11 ウウうUUuu11 OR エエえEEee11",
                        0,
                        100,
                        VIEW_COUNT,
                        EventDateBounds(
                            Instant.parse("2014-01-01T00:00:00Z"),
                            Instant.parse("2014-01-02T00:00:00Z"),
                            true,
                        ),
                        VOCADB_BETA,
                    ),
                ),
                arguments(
                    VideosByVocaDbTagRequest::class.java,
                    """
                {
                    "tag": "ｱアあＡAａa１1",
                    "scope": "  OR ｲイいＩIｉi１1 ｳウうＵUｕu１1 OR ｴエえＥEｅe１1   ",
                    "startOffset": 0,
                    "maxResults": 100,
                    "orderBy": "viewCounter",
                    "clientType": "vocadb"
                }
                """
                        .trimIndent(),
                    VideosByVocaDbTagRequest(
                        "アアあAAaa11",
                        "イイいIIii11 ウウうUUuu11 OR エエえEEee11",
                        0,
                        100,
                        VIEW_COUNT,
                        VOCADB,
                    ),
                ),
                arguments(
                    EventScheduleRequest::class.java,
                    """
                    {
                        "clientType": "vocadb_beta",
                        "useCached": false
                    }
                    """
                        .trimIndent(),
                    EventScheduleRequest(VOCADB_BETA, false),
                ),
                arguments(
                    EventScheduleRequest::class.java,
                    """
                    {
                        "clientType": "vocadb_beta"
                    }
                    """
                        .trimIndent(),
                    EventScheduleRequest(VOCADB_BETA, true),
                ),
            )
    }
}
