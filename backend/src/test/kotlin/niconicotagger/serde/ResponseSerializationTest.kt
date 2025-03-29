package niconicotagger.serde

import Utils.createSampleSongTypeStats
import Utils.jsonMapper
import net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson
import niconicotagger.dto.api.misc.ApiType.ARTISTS
import niconicotagger.dto.api.misc.ApiType.SONGS
import niconicotagger.dto.api.misc.AvailableNndVideo
import niconicotagger.dto.api.misc.AvailableNndVideoWithAdditionalData
import niconicotagger.dto.api.misc.NndTagData
import niconicotagger.dto.api.misc.NndTagType.MAPPED
import niconicotagger.dto.api.misc.NndTagType.NONE
import niconicotagger.dto.api.misc.NndTagType.SCOPE
import niconicotagger.dto.api.misc.NndTagType.TARGET
import niconicotagger.dto.api.misc.NndVideoWithAssociatedVocaDbEntry
import niconicotagger.dto.api.misc.NndVideoWithAssociatedVocaDbEntryForEvent
import niconicotagger.dto.api.misc.NndVideoWithAssociatedVocaDbEntryForTag
import niconicotagger.dto.api.misc.PvWithSuggestedTags
import niconicotagger.dto.api.misc.QueryConsoleArtistData
import niconicotagger.dto.api.misc.QueryConsoleData
import niconicotagger.dto.api.misc.QueryConsoleSongData
import niconicotagger.dto.api.misc.ReleaseEvent
import niconicotagger.dto.api.misc.SongEntry
import niconicotagger.dto.api.misc.SongEntryBase
import niconicotagger.dto.api.misc.SongEntryWithReleaseEventInfo
import niconicotagger.dto.api.misc.SongEntryWithTagAssignmentInfo
import niconicotagger.dto.api.misc.UnavailableNndVideo
import niconicotagger.dto.api.misc.VocaDbSongEntryWithPvs
import niconicotagger.dto.api.response.QueryConsoleResponse
import niconicotagger.dto.api.response.ReleaseEventWithVocaDbTagsResponse
import niconicotagger.dto.api.response.ReleaseEventWitnNndTagsResponse
import niconicotagger.dto.api.response.SongsWithPvsResponse
import niconicotagger.dto.api.response.VideosByNndTagsResponseForEvent
import niconicotagger.dto.api.response.VideosByNndTagsResponseForTagging
import niconicotagger.dto.api.response.VideosByTagsResponse
import niconicotagger.dto.api.response.VideosByTagsResponseForTagging
import niconicotagger.dto.inner.misc.SongType.Original
import niconicotagger.dto.inner.vocadb.VocaDbTag
import niconicotagger.dto.inner.vocadb.VocaDbTagSelectable
import org.instancio.Instancio
import org.instancio.Select.all
import org.instancio.Select.field
import org.instancio.TypeToken
import org.instancio.settings.Keys.COLLECTION_MAX_SIZE
import org.instancio.settings.Keys.COLLECTION_MIN_SIZE
import org.instancio.settings.Keys.MAP_MAX_SIZE
import org.instancio.settings.Keys.MAP_MIN_SIZE
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.ArgumentSet
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import java.time.Duration
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.ZoneOffset.UTC
import java.time.format.DateTimeFormatter.ISO_DATE
import java.util.stream.Stream

class ResponseSerializationTest {

    @ParameterizedTest
    @ArgumentsSource(QueryConsoleResponseTestData::class)
    fun `QueryConsoleResponse serialization test`(
        responseObject: QueryConsoleResponse<QueryConsoleData>,
        json: String
    ) {
        assertThatJson(jsonMapper.writeValueAsString(responseObject)).isEqualTo(json)
    }

    @ParameterizedTest
    @ArgumentsSource(ReleaseEventResponseTestData::class)
    fun `ReleaseEventResponse serialization test`(responseObject: ReleaseEventWitnNndTagsResponse, json: String) {
        assertThatJson(jsonMapper.writeValueAsString(responseObject)).isEqualTo(json)
    }

    @ParameterizedTest
    @ArgumentsSource(ReleaseEventWithLinkedTagResponseTestData::class)
    fun `ReleaseEventWithLinkedTagResponse serialization test`(
        responseObject: ReleaseEventWithVocaDbTagsResponse,
        json: String
    ) {
        assertThatJson(jsonMapper.writeValueAsString(responseObject)).isEqualTo(json)
    }

    @ParameterizedTest
    @ArgumentsSource(VideosByTagsResponseTestData::class)
    fun `VideosByTagsResponse serialization test`(
        responseObject: VideosByTagsResponse<NndVideoWithAssociatedVocaDbEntry<SongEntryBase>, SongEntryBase>,
        expectedJson: String
    ) {
        assertThatJson(jsonMapper.writeValueAsString(responseObject)).isEqualTo(expectedJson)
    }

    @ParameterizedTest
    @ArgumentsSource(SongsWithPvsResponseTestData::class)
    fun `SongsWithPvsResponse serialization test`(responseObject: SongsWithPvsResponse, expectedJson: String) {
        assertThatJson(jsonMapper.writeValueAsString(responseObject)).isEqualTo(expectedJson)
    }

    companion object {
        class ReleaseEventResponseTestData : ArgumentsProvider {
            private fun timezoneAdjustment(): ArgumentSet {
                val responseObject = Instancio.of(ReleaseEventWitnNndTagsResponse::class.java)
                    .set(
                        field("date"),
                        OffsetDateTime.of(LocalDateTime.parse("2025-04-05T01:00:00"), ZoneOffset.ofHours(3)).toInstant()
                    )
                    .set(
                        field("endDate"),
                        OffsetDateTime.of(LocalDateTime.parse("2025-04-05T03:00:00"), ZoneOffset.ofHours(3)).toInstant()
                    )
                    .create()
                val json = """
                    {
                        "id": ${responseObject.id},
                        "date": "2025-04-04",
                        "endDate": "2025-04-05",
                        "name": "${responseObject.name}",
                        "category": "${responseObject.category}",
                        "nndTags": ${responseObject.nndTags.map { "\"$it\"" }},
                        "suggestFiltering": ${responseObject.suggestFiltering},
                        "seriesId": ${responseObject.seriesId}
                    }
                    """.trimIndent()
                return argumentSet(
                    "with timezone adjustment in dates",
                    responseObject,
                    json
                )
            }

            private fun noDates(): ArgumentSet {
                val responseObject = Instancio.of(ReleaseEventWitnNndTagsResponse::class.java)
                    .ignore(all(field("date"), field("endDate")))
                    .create()
                val json = """
                    {
                        "id": ${responseObject.id},
                        "date": null,
                        "endDate": null,
                        "name": "${responseObject.name}",
                        "category": "${responseObject.category}",
                        "nndTags": ${responseObject.nndTags.map { "\"$it\"" }},
                        "suggestFiltering": ${responseObject.suggestFiltering},
                        "seriesId": ${responseObject.seriesId}
                    }
                    """.trimIndent()
                return argumentSet(
                    "with empty date fields",
                    responseObject,
                    json
                )
            }

            override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
                return Stream.of(timezoneAdjustment(), noDates())
            }

        }

        class ReleaseEventWithLinkedTagResponseTestData : ArgumentsProvider {
            private fun timezoneAdjustment(): ArgumentSet {
                val responseObject = Instancio.of(ReleaseEventWithVocaDbTagsResponse::class.java)
                    .set(
                        field("date"),
                        OffsetDateTime.of(LocalDateTime.parse("2025-04-05T01:00:00"), ZoneOffset.ofHours(3)).toInstant()
                    )
                    .set(
                        field("endDate"),
                        OffsetDateTime.of(LocalDateTime.parse("2025-04-05T03:00:00"), ZoneOffset.ofHours(3)).toInstant()
                    )
                    .setBlank(field("vocaDbTags"))
                    .create()
                val json = """
                    {
                        "id": ${responseObject.id},
                        "date": "2025-04-04",
                        "endDate": "2025-04-05",
                        "name": "${responseObject.name}",
                        "category": "${responseObject.category}",
                        "vocaDbTags": []
                    }
                    """.trimIndent()
                return argumentSet(
                    "with timezone adjustment in dates",
                    responseObject,
                    json
                )
            }

            private fun noDates(): ArgumentSet {
                val responseObject = Instancio.of(ReleaseEventWithVocaDbTagsResponse::class.java)
                    .ignore(all(field("date"), field("endDate")))
                    .generate(field("vocaDbTags")) { gen -> gen.collection<VocaDbTag>().size(2) }
                    .create()
                val json = """
                    {
                        "id": ${responseObject.id},
                        "date": null,
                        "endDate": null,
                        "name": "${responseObject.name}",
                        "category": "${responseObject.category}",
                        "vocaDbTags": [
                            {
                                "id": ${responseObject.vocaDbTags[0].id},
                                "name": "${responseObject.vocaDbTags[0].name}"
                            },
                            {
                                "id": ${responseObject.vocaDbTags[1].id},
                                "name": "${responseObject.vocaDbTags[1].name}"
                            }
                        ]
                    }
                    """.trimIndent()
                return argumentSet(
                    "with empty date fields",
                    responseObject,
                    json
                )
            }

            override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
                return Stream.of(timezoneAdjustment(), noDates())
            }

        }

        class QueryConsoleResponseTestData : ArgumentsProvider {
            private fun artist(): ArgumentSet {
                val responseObject = Instancio.of(object : TypeToken<QueryConsoleResponse<QueryConsoleArtistData>> {})
                    .withSetting(COLLECTION_MIN_SIZE, 1)
                    .withSetting(COLLECTION_MAX_SIZE, 1)
                    .withSetting(MAP_MIN_SIZE, 1)
                    .withSetting(MAP_MAX_SIZE, 1)
                    .create()
                val json = """
                {
                    "items": [
                        {
                            "id": ${responseObject.items[0].id},
                            "name": "${responseObject.items[0].name}",
                            "tags": [
                                {
                                    "id": ${responseObject.items[0].tags[0].id},
                                    "name": "${responseObject.items[0].tags[0].name}"
                                }
                            ],
                            "type": "${responseObject.items[0].type}"
                        }
                    ],
                    "totalCount": ${responseObject.totalCount},
                    "tagPool": [
                        {
                            "name": "${responseObject.tagPool[0].name}",
                            "id": ${responseObject.tagPool[0].id}
                        }
                    ]
                }
                """.trimIndent()
                return argumentSet(
                    ARTISTS.toString(), responseObject, json
                )
            }

            private fun song(): ArgumentSet {
                val responseObject = Instancio.of(object : TypeToken<QueryConsoleResponse<QueryConsoleSongData>> {})
                    .withSetting(COLLECTION_MIN_SIZE, 1)
                    .withSetting(COLLECTION_MAX_SIZE, 1)
                    .withSetting(MAP_MIN_SIZE, 1)
                    .withSetting(MAP_MAX_SIZE, 1)
                    .create()
                val json = """
                {
                    "items": [
                        {
                            "id": ${responseObject.items[0].id},
                            "name": "${responseObject.items[0].name}",
                            "tags": [
                                {
                                    "id": ${responseObject.items[0].tags[0].id},
                                    "name": "${responseObject.items[0].tags[0].name}"
                                }
                            ],
                            "type": "${responseObject.items[0].type}",
                            "artistString": "${responseObject.items[0].artistString}"
                        }
                    ],
                    "totalCount": ${responseObject.totalCount},
                    "tagPool": [
                        {
                            "name": "${responseObject.tagPool[0].name}",
                            "id": ${responseObject.tagPool[0].id}
                        }
                    ]
                }
                """.trimIndent()
                return argumentSet(
                    SONGS.toString(), responseObject, json
                )
            }

            override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
                return Stream.of(artist(), song())
            }

        }

        class VideosByTagsResponseTestData : ArgumentsProvider {
            private fun videosByNndTagsForTagging(): ArgumentSet {
                val responseObject = VideosByNndTagsResponseForTagging(
                    listOf(
                        Instancio.of(NndVideoWithAssociatedVocaDbEntryForTag::class.java)
                            .ignore(
                                all(
                                    field(AvailableNndVideoWithAdditionalData::class.java, "description"),
                                    field("entry")
                                )
                            )
                            .set(
                                field(AvailableNndVideoWithAdditionalData::class.java, "tags"),
                                listOf(
                                    NndTagData("targetTag", TARGET, true),
                                    NndTagData("scopeTag", SCOPE, true),
                                    NndTagData("mappedTag", MAPPED, true),
                                    NndTagData("randomTag", NONE, true)
                                )
                            )
                            .set(
                                field(AvailableNndVideoWithAdditionalData::class.java, "length"),
                                Duration.ofMinutes(2)
                            )
                            .create(),
                        Instancio.of(NndVideoWithAssociatedVocaDbEntryForTag::class.java)
                            .ignore(all(field("publisher")))
                            .set(
                                field(AvailableNndVideoWithAdditionalData::class.java, "tags"),
                                listOf(NndTagData("targetTag", TARGET, false))
                            )
                            .set(
                                field(AvailableNndVideoWithAdditionalData::class.java, "length"),
                                Duration.ofHours(1).plusMinutes(1).plusSeconds(10)
                            )
                            .generate(
                                field(
                                    SongEntryWithTagAssignmentInfo::class.java,
                                    "mappedTags"
                                )
                            ) { gen -> gen.collection<VocaDbTagSelectable>().size(2) }
                            .set(field(SongEntryWithTagAssignmentInfo::class.java, "type"), Original)
                            .create(),
                        Instancio.of(NndVideoWithAssociatedVocaDbEntryForTag::class.java)
                            .ignore(all(field("publisher"), field("entry")))
                            .set(
                                field(AvailableNndVideoWithAdditionalData::class.java, "tags"),
                                listOf(NndTagData("targetTag", TARGET, false))
                            )
                            .set(
                                field(AvailableNndVideoWithAdditionalData::class.java, "length"),
                                Duration.ofMinutes(2)
                            )
                            .create()
                    ),
                    2,
                    Instancio.create(String::class.java),
                    createSampleSongTypeStats(Original),
                    Instancio.ofList(VocaDbTag::class.java).size(2).create()
                )

                val expectedJson = """
                {
                    "cleanScope": "${responseObject.cleanScope}",
                    "totalCount": ${responseObject.totalCount},
                    "songTypeStats": {
                        "Unspecified": 0,
                        "Original": 1,
                        "Remaster": 0,
                        "Remix": 0,
                        "Cover": 0,
                        "Instrumental": 0,
                        "Mashup": 0,
                        "MusicPV": 0,
                        "DramaPV": 0,
                        "Other": 0
                    },
                    "items": [
                        {
                            "video": {
                                "id": "${responseObject.items[0].video.id}",
                                "title": "${responseObject.items[0].video.title}",
                                "description": null,
                                "tags": [
                                    {
                                        "name": "${responseObject.items[0].video.tags[0].name}",
                                        "type": "${responseObject.items[0].video.tags[0].type.color}",
                                        "locked": ${responseObject.items[0].video.tags[0].locked}
                                    },
                                    {
                                        "name": "${responseObject.items[0].video.tags[1].name}",
                                        "type": "${responseObject.items[0].video.tags[1].type.color}",
                                        "locked": ${responseObject.items[0].video.tags[1].locked}
                                    },
                                    {
                                        "name": "${responseObject.items[0].video.tags[2].name}",
                                        "type": "${responseObject.items[0].video.tags[2].type.color}",
                                        "locked": ${responseObject.items[0].video.tags[2].locked}
                                    },
                                    {
                                        "name": "${responseObject.items[0].video.tags[3].name}",
                                        "type": "${responseObject.items[0].video.tags[3].type.color}",
                                        "locked": ${responseObject.items[0].video.tags[3].locked}
                                    }
                                ],
                                "length": "2:00"
                            },
                            "publisher": {
                                "link": "${responseObject.items[0].publisher?.link}",
                                "linkText": ${if (responseObject.items[0].publisher?.linkText == null) null else "\"" + responseObject.items[0].publisher?.linkText + "\""},
                                "name": "${responseObject.items[0].publisher?.name}",
                                "type": "${responseObject.items[0].publisher?.type}"
                            },
                            "entry": null
                        },
                        {
                            "video": {
                                "id": "${responseObject.items[1].video.id}",
                                "title": "${responseObject.items[1].video.title}",
                                "description": "${responseObject.items[1].video.description}",
                                "tags": [
                                    {
                                        "name": "${responseObject.items[1].video.tags[0].name}",
                                        "type": "${responseObject.items[1].video.tags[0].type.color}",
                                        "locked": ${responseObject.items[1].video.tags[0].locked}
                                    }
                                ],
                                "length": "1:01:10"
                            },
                            "entry": {
                                "id": ${responseObject.items[1].entry?.id},
                                "name": "${responseObject.items[1].entry?.name}",
                                "type": "${responseObject.items[1].entry?.type}",
                                "artistString": "${responseObject.items[1].entry?.artistString}",
                                "publishedOn": "${
                    responseObject.items[1].entry?.publishedAt?.atOffset(UTC)?.toLocalDate()?.format(ISO_DATE)
                }",
                                "mappedTags": [
                                    {
                                        "tag": {
                                            "id": ${responseObject.items[1].entry?.mappedTags?.get(0)?.tag?.id},
                                            "name": "${responseObject.items[1].entry?.mappedTags?.get(0)?.tag?.name}"
                                        },
                                        "selected": ${responseObject.items[1].entry?.mappedTags?.get(0)?.selected}
                                    },
                                    {
                                        "tag": {
                                            "id": ${responseObject.items[1].entry?.mappedTags?.get(1)?.tag?.id},
                                            "name": "${responseObject.items[1].entry?.mappedTags?.get(1)?.tag?.name}"
                                        },
                                        "selected": ${responseObject.items[1].entry?.mappedTags?.get(1)?.selected}
                                    }
                                ]
                            },
                            "publisher": null
                        },
                        {
                            "video": {
                                "id": "${responseObject.items[2].video.id}",
                                "title": "${responseObject.items[2].video.title}",
                                "description": "${responseObject.items[2].video.description}",
                                "tags": [
                                    {
                                        "name": "${responseObject.items[2].video.tags[0].name}",
                                        "type": "${responseObject.items[2].video.tags[0].type.color}",
                                        "locked": ${responseObject.items[2].video.tags[0].locked}
                                    }
                                ],
                                "length": "2:00"
                            },
                            "publisher": null,
                            "entry": null
                        }
                    ],
                    "tagMappings": [
                        {
                            "id": ${responseObject.tagMappings.toList()[0].id},
                            "name": "${responseObject.tagMappings.toList()[0].name}"
                        },
                        {
                            "id": ${responseObject.tagMappings.toList()[1].id},
                            "name": "${responseObject.tagMappings.toList()[1].name}"
                        }
                    ]
                }
            """.trimIndent()

                return argumentSet(
                    VideosByNndTagsResponseForTagging::class.simpleName,
                    responseObject,
                    expectedJson
                )
            }

            private fun videosByTagsResponseForEvent(): ArgumentSet {
                val responseObject = VideosByNndTagsResponseForEvent(
                    listOf(
                        Instancio.of(NndVideoWithAssociatedVocaDbEntryForEvent::class.java)
                            .ignore(
                                all(
                                    field(AvailableNndVideoWithAdditionalData::class.java, "description"),
                                    field("entry")
                                )
                            )
                            .set(
                                field(AvailableNndVideoWithAdditionalData::class.java, "tags"),
                                listOf(
                                    NndTagData("targetTag", TARGET, true),
                                    NndTagData("scopeTag", SCOPE, true),
                                    NndTagData("mappedTag", MAPPED, true),
                                    NndTagData("randomTag", NONE, true)
                                )
                            )
                            .set(
                                field(AvailableNndVideoWithAdditionalData::class.java, "length"),
                                Duration.ofMinutes(2)
                            )
                            .create(),
                        Instancio.of(NndVideoWithAssociatedVocaDbEntryForEvent::class.java)
                            .ignore(all(field("publisher")))
                            .set(
                                field(AvailableNndVideoWithAdditionalData::class.java, "tags"),
                                listOf(NndTagData("targetTag", TARGET, false))
                            )
                            .set(
                                field(AvailableNndVideoWithAdditionalData::class.java, "length"),
                                Duration.ofHours(1).plusMinutes(1).plusSeconds(10)
                            )
                            .set(field(SongEntryWithReleaseEventInfo::class.java, "type"), Original)
                            .generate(field(SongEntryWithReleaseEventInfo::class.java, "events")) { gen ->
                                gen.collection<ReleaseEvent>().size(2)
                            }.create(),
                        Instancio.of(NndVideoWithAssociatedVocaDbEntryForEvent::class.java)
                            .ignore(all(field("publisher"), field("entry")))
                            .set(
                                field(AvailableNndVideoWithAdditionalData::class.java, "tags"),
                                listOf(NndTagData("targetTag", TARGET, false))
                            )
                            .set(
                                field(AvailableNndVideoWithAdditionalData::class.java, "length"),
                                Duration.ofMinutes(2)
                            )
                            .create()
                    ),
                    2,
                    Instancio.create(String::class.java),
                )

                val expectedJson = """
                {
                    "cleanScope": "${responseObject.cleanScope}",
                    "totalCount": ${responseObject.totalCount},
                    "items": [
                        {
                            "video": {
                                "id": "${responseObject.items[0].video.id}",
                                "title": "${responseObject.items[0].video.title}",
                                "description": null,
                                "tags": [
                                    {
                                        "name": "${responseObject.items[0].video.tags[0].name}",
                                        "type": "${responseObject.items[0].video.tags[0].type.color}",
                                        "locked": ${responseObject.items[0].video.tags[0].locked}
                                    },
                                    {
                                        "name": "${responseObject.items[0].video.tags[1].name}",
                                        "type": "${responseObject.items[0].video.tags[1].type.color}",
                                        "locked": ${responseObject.items[0].video.tags[1].locked}
                                    },
                                    {
                                        "name": "${responseObject.items[0].video.tags[2].name}",
                                        "type": "${responseObject.items[0].video.tags[2].type.color}",
                                        "locked": ${responseObject.items[0].video.tags[2].locked}
                                    },
                                    {
                                        "name": "${responseObject.items[0].video.tags[3].name}",
                                        "type": "${responseObject.items[0].video.tags[3].type.color}",
                                        "locked": ${responseObject.items[0].video.tags[3].locked}
                                    }
                                ],
                                "length": "2:00"
                            },
                            "publisher": {
                                "link": "${responseObject.items[0].publisher?.link}",
                                "linkText": ${if (responseObject.items[0].publisher?.linkText == null) null else "\"" + responseObject.items[0].publisher?.linkText + "\""},
                                "name": "${responseObject.items[0].publisher?.name}",
                                "type": "${responseObject.items[0].publisher?.type}"
                            },
                            "entry": null,
                            "disposition": {
                                "disposition": "${responseObject.items[0].disposition.disposition.name.lowercase()}",
                                "byDays": ${responseObject.items[0].disposition.byDays},
                                "color": "${responseObject.items[0].disposition.color}"
                            },
                            "publishedOn": "${responseObject.items[0].publishedAt.atOffset(UTC).toLocalDate()}"
                        },
                        {
                            "video": {
                                "id": "${responseObject.items[1].video.id}",
                                "title": "${responseObject.items[1].video.title}",
                                "description": "${responseObject.items[1].video.description}",
                                "tags": [
                                    {
                                        "name": "${responseObject.items[1].video.tags[0].name}",
                                        "type": "${responseObject.items[1].video.tags[0].type.color}",
                                        "locked": ${responseObject.items[1].video.tags[0].locked}
                                    }
                                ],
                                "length": "1:01:10"
                            },
                            "entry": {
                                "id": ${responseObject.items[1].entry?.id},
                                "name": "${responseObject.items[1].entry?.name}",
                                "type": "${responseObject.items[1].entry?.type}",
                                "artistString": "${responseObject.items[1].entry?.artistString}",
                                "events": [
                                    {
                                        "id": ${responseObject.items[1].entry?.events?.get(0)?.id},
                                        "name": "${responseObject.items[1].entry?.events?.get(0)?.name}",
                                        "seriesId": ${responseObject.items[1].entry?.events?.get(0)?.seriesId}
                                    },
                                    {
                                        "id": ${responseObject.items[1].entry?.events?.get(1)?.id},
                                        "name": "${responseObject.items[1].entry?.events?.get(1)?.name}",
                                        "seriesId": ${responseObject.items[1].entry?.events?.get(1)?.seriesId}
                                    }
                                ]
                            },
                            "publisher": null,
                            "disposition": {
                                "disposition": "${responseObject.items[1].disposition.disposition.name.lowercase()}",
                                "byDays": ${responseObject.items[1].disposition.byDays},
                                "color": "${responseObject.items[1].disposition.color}"
                            },
                            "publishedOn": "${responseObject.items[1].publishedAt.atOffset(UTC).toLocalDate()}"
                        },
                        {
                            "video": {
                                "id": "${responseObject.items[2].video.id}",
                                "title": "${responseObject.items[2].video.title}",
                                "description": "${responseObject.items[2].video.description}",
                                "tags": [
                                    {
                                        "name": "${responseObject.items[2].video.tags[0].name}",
                                        "type": "${responseObject.items[2].video.tags[0].type.color}",
                                        "locked": ${responseObject.items[2].video.tags[0].locked}
                                    }
                                ],
                                "length": "2:00"
                            },
                            "publisher": null,
                            "entry": null,
                            "disposition": {
                                "disposition": "${responseObject.items[2].disposition.disposition.name.lowercase()}",
                                "byDays": ${responseObject.items[2].disposition.byDays},
                                "color": "${responseObject.items[2].disposition.color}"
                            },
                            "publishedOn": "${responseObject.items[2].publishedAt.atOffset(UTC).toLocalDate()}"
                        }
                    ]
                }
            """.trimIndent()

                return argumentSet(
                    VideosByNndTagsResponseForEvent::class.simpleName,
                    responseObject,
                    expectedJson
                )
            }

            override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
                return VideosByTagsResponse::class.sealedSubclasses.map {
                    when (it) {
                        VideosByTagsResponseForTagging::class -> videosByNndTagsForTagging()
                        VideosByNndTagsResponseForEvent::class -> videosByTagsResponseForEvent()
                        else -> error("No arguments configured for class ${it.simpleName}")
                    }
                }.stream()
            }

        }

        class SongsWithPvsResponseTestData : ArgumentsProvider {
            private fun availablePvWithTags(): ArgumentSet {
                val songEntry = Instancio.of(SongEntry::class.java)
                    .set(field("type"), Original)
                    .create()
                val pv1 = Instancio.of(AvailableNndVideo::class.java)
                    .generate(field("tags")) { gen -> gen.collection<NndTagData>().size(2) }
                    .create()
                val pv2 = Instancio.of(AvailableNndVideo::class.java)
                    .ignore(field("description"))
                    .generate(field("tags")) { gen -> gen.collection<NndTagData>().size(2) }
                    .create()
                val responseObject = SongsWithPvsResponse(
                    listOf(
                        VocaDbSongEntryWithPvs(
                            songEntry,
                            listOf(
                                PvWithSuggestedTags(
                                    pv1,
                                    Instancio.ofList(VocaDbTagSelectable::class.java).size(2).create()
                                ),
                                PvWithSuggestedTags(
                                    pv2,
                                    Instancio.ofList(VocaDbTagSelectable::class.java).size(2).create()
                                )
                            ),
                            emptyList()
                        )
                    ),
                    createSampleSongTypeStats(songEntry.type),
                    Instancio.create(Long::class.java)
                )
                val expectedJson = """
                    {
                        "items": [
                            {
                                "entry": {
                                    "id": ${songEntry.id},
                                    "name": "${songEntry.name}",
                                    "type": "${songEntry.type}",
                                    "artistString": "${songEntry.artistString}",
                                    "publishedOn": "${
                    requireNotNull(songEntry.publishedAt).atOffset(UTC).toLocalDate().format(ISO_DATE)
                }"
                                },
                                "availablePvs": [
                                    {
                                        "video": {
                                            "id": "${pv1.id}",
                                            "title": "${pv1.title}",
                                            "description": "${pv1.description}",
                                            "tags": [
                                                {
                                                    "name": "${pv1.tags[0].name}",
                                                    "type": "${pv1.tags[0].type.color}",
                                                    "locked": ${pv1.tags[0].locked}
                                                },
                                                {
                                                    "name": "${pv1.tags[1].name}",
                                                    "type": "${pv1.tags[1].type.color}",
                                                    "locked": ${pv1.tags[1].locked}
                                                }
                                            ]
                                        },
                                        "suggestedTags": [
                                            {
                                                "tag": {
                                                    "id": ${responseObject.items[0].availablePvs[0].suggestedTags[0].tag.id},
                                                    "name": "${responseObject.items[0].availablePvs[0].suggestedTags[0].tag.name}"
                                                },
                                                "selected": ${responseObject.items[0].availablePvs[0].suggestedTags[0].selected}
                                            },
                                            {
                                                "tag": {
                                                    "id": ${responseObject.items[0].availablePvs[0].suggestedTags[1].tag.id},
                                                    "name": "${responseObject.items[0].availablePvs[0].suggestedTags[1].tag.name}"
                                                },
                                                "selected": ${responseObject.items[0].availablePvs[0].suggestedTags[1].selected}
                                            }
                                        ]
                                    },
                                    {
                                        "video": {
                                            "id": "${pv2.id}",
                                            "title": "${pv2.title}",
                                            "description": null,
                                            "tags": [
                                                {
                                                    "name": "${pv2.tags[0].name}",
                                                    "type": "${pv2.tags[0].type.color}",
                                                    "locked": ${pv2.tags[0].locked}
                                                },
                                                {
                                                    "name": "${pv2.tags[1].name}",
                                                    "type": "${pv2.tags[1].type.color}",
                                                    "locked": ${pv2.tags[1].locked}
                                                }
                                            ]
                                        },
                                        "suggestedTags": [
                                            {
                                                "tag": {
                                                    "id": ${responseObject.items[0].availablePvs[1].suggestedTags[0].tag.id},
                                                    "name": "${responseObject.items[0].availablePvs[1].suggestedTags[0].tag.name}"
                                                },
                                                "selected": ${responseObject.items[0].availablePvs[1].suggestedTags[0].selected}
                                            },
                                            {
                                                "tag": {
                                                    "id": ${responseObject.items[0].availablePvs[1].suggestedTags[1].tag.id},
                                                    "name": "${responseObject.items[0].availablePvs[1].suggestedTags[1].tag.name}"
                                                },
                                                "selected": ${responseObject.items[0].availablePvs[1].suggestedTags[1].selected}
                                            }
                                        ]
                                    }
                                ],
                                "unavailablePvs": []
                            }
                        ],
                        "songTypeStats": {
                            "Unspecified": 0,
                            "Original": 1,
                            "Remaster": 0,
                            "Remix": 0,
                            "Cover": 0,
                            "Instrumental": 0,
                            "Mashup": 0,
                            "MusicPV": 0,
                            "DramaPV": 0,
                            "Other": 0
                        },
                        "totalCount": ${responseObject.totalCount}
                    }
                    """.trimIndent()

                return argumentSet(
                    "song has two available PVs with different tags; publish date is present in the entry",
                    responseObject,
                    expectedJson
                )
            }

            private fun unavailablePvAndNoPublishDate(): ArgumentSet {
                val songEntry = Instancio.of(SongEntry::class.java)
                    .ignore(field("publishedAt"))
                    .set(field("type"), Original)
                    .create()
                val pv = Instancio.create(UnavailableNndVideo::class.java)
                val responseObject = SongsWithPvsResponse(
                    listOf(
                        VocaDbSongEntryWithPvs(
                            songEntry,
                            emptyList(),
                            listOf(
                                pv
                            )
                        )
                    ),
                    createSampleSongTypeStats(songEntry.type),
                    Instancio.create(Long::class.java)
                )
                val expectedJson = """
                    {
                        "items": [
                            {
                                "entry": {
                                    "id": ${songEntry.id},
                                    "name": "${songEntry.name}",
                                    "type": "${songEntry.type}",
                                    "artistString": "${songEntry.artistString}",
                                    "publishedOn": null
                                },
                                "availablePvs": [],
                                "unavailablePvs": [
                                    {
                                        "id": "${pv.id}",
                                        "title": "${pv.title}",
                                        "error": "${pv.error}"
                                    }
                                ]
                            }
                        ],
                        "songTypeStats": {
                            "Unspecified": 0,
                            "Original": 1,
                            "Remaster": 0,
                            "Remix": 0,
                            "Cover": 0,
                            "Instrumental": 0,
                            "Mashup": 0,
                            "MusicPV": 0,
                            "DramaPV": 0,
                            "Other": 0
                        },
                        "totalCount": ${responseObject.totalCount}
                    }
                    """.trimIndent()

                return argumentSet(
                    "song has an unavailable PV that should be disabled; publish date is missing from the entry",
                    responseObject,
                    expectedJson
                )
            }

            override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
                return Stream.of(availablePvWithTags(), unavailablePvAndNoPublishDate())
            }

        }

    }

}
