use serde::{Deserialize, Serialize};
use crate::client::models::activity::{ActivityEditEvent, ActivityEntryForApiContract};
use crate::client::models::archived::ArchivedObjectVersionForApiContract;
use crate::client::models::artist::NicoPublisher;
use crate::client::models::releaseevent::{ReleaseEventForApiContractSimplified};

use crate::client::models::song::{SongForApiContract, SongType};
use crate::client::models::tag::{AssignableTag};
use crate::client::models::user::UserForApiContract;
use crate::client::nicomodels::{SongForApiContractWithThumbnailsAndMappedTags, TagBaseContractSimplified, TagBaseContractSimplifiedWithUsageCount};

#[derive(Deserialize, Debug)]
pub struct LoginRequest {
    pub username: String,
    pub password: String,
    pub database: Database,
}

#[derive(Serialize, Debug)]
pub struct LoginResponse {
    pub token: String,
}

#[derive(Serialize, Deserialize, Debug)]
pub struct Token {
    pub user_id: i32,
    pub database: Database,
    pub cookies: Vec<String>,
    pub exp: i64,
}

#[derive(Deserialize, Serialize, Clone, Debug)]
pub enum Database {
    VocaDb,
    VocaDbBeta,
    TouhouDb,
    UtaiteDb,
}

#[derive(Deserialize, Debug)]
pub struct TagFetchRequest {
    #[serde(rename = "startOffset")]
    pub start_offset: i32,
    #[serde(rename = "maxResults")]
    pub max_results: i32,
    pub tag: String,
    #[serde(rename = "scopeTag")]
    pub scope_tag: String,
    #[serde(rename = "orderBy")]
    pub order_by: String,
}

#[derive(Deserialize, Debug)]
pub struct EventByTagsFetchRequest {
    #[serde(rename = "startOffset")]
    pub start_offset: i32,
    #[serde(rename = "maxResults")]
    pub max_results: i32,
    pub tags: String,
    #[serde(rename = "scopeTag")]
    pub scope_tag: String,
    #[serde(rename = "orderBy")]
    pub order_by: String,
    #[serde(rename = "eventId")]
    pub event_id: i32,
    #[serde(rename = "startTime")]
    pub start_time: Option<String>,
    #[serde(rename = "endTime")]
    pub end_time: Option<String>,
}

#[derive(Deserialize, Debug)]
pub struct SongsByEventTagFetchRequest {
    #[serde(rename = "startOffset")]
    pub start_offset: i32,
    #[serde(rename = "maxResults")]
    pub max_results: i32,
    pub tag: String,
    #[serde(rename = "orderBy")]
    pub order_by: String,
}

#[derive(Deserialize, Debug)]
pub struct ReleaseEventWithNndTagsFetchRequest {
    #[serde(rename = "eventName")]
    pub event_name: String,
}

// #[derive(Deserialize, Debug)]
// pub struct ReleaseEventWithNndTagsFetchRequest {
//     #[serde(rename = "eventName")]
//     pub event_name: String,
//     #[serde(rename = "startOffset")]
//     pub start_offset: i32,
//     #[serde(rename = "maxResults")]
//     pub max_results: i32,
//     #[serde(rename = "orderBy")]
//     pub order_by: String,
// }

#[derive(Deserialize, Debug)]
pub struct AssignTagRequest {
    #[serde(rename = "songId")]
    pub song_id: i32,
    pub tags: Vec<AssignableTag>,
}

#[derive(Deserialize, Debug)]
pub struct LookupAndAssignTagRequest {
    #[serde(rename = "songId")]
    pub song_id: i32,
    pub tags: Vec<TagBaseContractSimplified>,
    pub disable: Vec<String>,
}

#[derive(Deserialize, Debug)]
pub struct CustomQueryPayload {
    pub query: String,
}

#[derive(Deserialize, Debug)]
pub struct TagsRemovalPayload {
    #[serde(rename = "songId")]
    pub song_id: i32,
    #[serde(rename = "tagIds")]
    pub tag_ids: Vec<i64>,
}

#[derive(Serialize, Deserialize, Debug)]
pub struct SongForApiContractSimplified {
    pub id: i32,
    pub name: String,
    #[serde(rename = "songType")]
    pub song_type: String,
    #[serde(rename = "artistString")]
    pub artist_string: String,
    #[serde(rename = "releaseEvent")]
    pub release_event: Option<ReleaseEventForApiContractSimplified>,
    #[serde(rename = "publishDate")]
    pub publish_date: Option<String>,
    pub tags: Vec<TagBaseContractSimplified>,
}

#[derive(Serialize, Deserialize, Debug)]
pub struct SongForApiContractSimplifiedWithTagUsageCounts {
    pub id: i32,
    pub name: String,
    #[serde(rename = "songType")]
    pub song_type: String,
    #[serde(rename = "artistString")]
    pub artist_string: String,
    #[serde(rename = "releaseEvent")]
    pub release_event: Option<ReleaseEventForApiContractSimplified>,
    #[serde(rename = "publishDate")]
    pub publish_date: Option<String>,
    pub tags: Vec<TagBaseContractSimplifiedWithUsageCount>,
}

#[derive(Serialize, Deserialize, Debug)]
pub struct SongForApiContractSimplifiedWithMultipleEventInfo {
    pub id: i32,
    pub name: String,
    #[serde(rename = "taggedWithMultipleEvents")]
    pub tagged_with_multiple_events: bool,
    #[serde(rename = "taggedWithEventParticipant")]
    pub tagged_with_event_participant: bool,
    #[serde(rename = "songType")]
    pub song_type: SongType,
    #[serde(rename = "artistString")]
    pub artist_string: String,
    #[serde(rename = "releaseEvent")]
    pub release_event: Option<ReleaseEventForApiContractSimplified>,
    #[serde(rename = "publishDate")]
    pub publish_date: Option<String>,
}

#[derive(Serialize)]
pub struct SongForApiContractSimplifiedWithMultipleEventInfoSearchResult {
    pub items: Vec<SongForApiContractSimplifiedWithMultipleEventInfo>,
    #[serde(rename = "totalCount")]
    pub total_count: i32,
}

#[derive(Serialize, Deserialize, Debug)]
pub struct DisplayableTag {
    pub name: String,
    pub variant: String,
}

#[derive(Serialize)]
pub struct VideoWithEntry {
    pub video: NicoVideoWithTidyTags,
    #[serde(rename = "songEntry")]
    pub song_entry: Option<SongForApiContractSimplifiedWithMultipleEventInfo>,
    pub publisher: Option<NicoPublisher>,
    pub processed: bool,
}

#[derive(Serialize)]
pub struct VideosWithEntries {
    pub items: Vec<VideoWithEntry>,
    #[serde(rename = "totalVideoCount")]
    pub total_video_count: i32,
    pub tags: Vec<AssignableTag>,
    #[serde(rename = "tagMappings")]
    pub tag_mappings: Vec<String>,
    #[serde(rename = "safeScope")]
    pub safe_scope: String,
}

#[derive(Serialize)]
pub struct VideosWithEntriesByVocaDbTag {
    pub items: Vec<VideoWithEntry>,
    #[serde(rename = "totalVideoCount")]
    pub total_video_count: i32,
    pub tags: Vec<AssignableTag>,
    #[serde(rename = "tagMappings")]
    pub tag_mappings: Vec<String>,
    #[serde(rename = "safeScope")]
    pub safe_scope: String,
}

#[derive(Serialize)]
pub struct TagFetchResponse {
    pub videos: Vec<Option<SongForApiContract>>,
    #[serde(rename = "totalCount")]
    pub total_count: i32,
}

#[derive(Serialize, Deserialize, Debug)]
pub struct NicoVideo {
    #[serde(rename = "contentId")]
    pub id: String,
    pub title: String,
    pub tags: String,
    #[serde(rename = "userId")]
    pub user_id: Option<i32>,
    #[serde(rename = "startTime")]
    pub start_time: String,
}

#[derive(Serialize, Deserialize)]
pub struct NicoVideoWithTidyTags {
    #[serde(rename = "contentId")]
    pub id: String,
    pub title: String,
    #[serde(rename = "startTime")]
    pub start_time: String,
    pub tags: Vec<DisplayableTag>,
}

#[derive(Deserialize)]
pub struct NicoMeta {
    pub id: String,
    #[serde(rename = "totalCount")]
    pub total_count: i32,
    pub status: i32,
}

#[derive(Deserialize)]
pub struct NicoResponse {
    pub data: Vec<NicoVideo>,
    pub meta: NicoMeta,
}

#[derive(Deserialize)]
pub struct NicoResponseWithScope {
    pub data: Vec<NicoVideo>,
    pub meta: NicoMeta,
    #[serde(rename = "safeScope")]
    pub safe_scope: String,
}

#[derive(Deserialize)]
pub struct SongWithMappedNicoTags {
    pub data: SongForApiContract,
    pub tags: Vec<TagMappingContract>,
}

#[derive(Deserialize)]
pub struct NicoVideoThumbInfo {
    pub data: SongForApiContract,
    pub tags: Vec<TagMappingContract>,
}

#[derive(Serialize, Deserialize, Clone)]
pub struct TagMappingContract {
    #[serde(rename = "sourceTag")]
    pub source_tag: String,
    pub tag: TagBaseContractSimplified,
}

#[derive(Serialize)]
pub struct DBFetchResponse {
    pub items: Vec<SongForApiContractWithThumbnailsAndMappedTags>,
    #[serde(rename = "totalCount")]
    pub total_count: i32,
}

#[derive(Serialize)]
pub struct DBFetchResponseWithTimestamps {
    pub items: Vec<SongForApiContractWithThumbnailsAndMappedTags>,
    #[serde(rename = "totalCount")]
    pub total_count: i32,
    #[serde(rename = "timestampFirst")]
    pub timestamp_first: String,
    #[serde(rename = "timestampLast")]
    pub timestamp_last: String,
}

#[derive(Deserialize)]
pub struct DBFetchRequest {
    #[serde(rename = "startOffset")]
    pub start_offset: i32,
    #[serde(rename = "maxResults")]
    pub max_results: i32,
    #[serde(rename = "orderBy")]
    pub order_by: String,
}

#[derive(Deserialize)]
pub struct DBBeforeSinceFetchRequest {
    #[serde(rename = "maxResults")]
    pub max_results: i32,
    pub mode: String,
    #[serde(rename = "dateTime")]
    pub date_time: String,
    #[serde(rename = "songId")]
    pub song_id: i32,
    #[serde(rename = "sortRule")]
    pub sort_rule: String,
}

#[derive(Serialize)]
pub struct DBBeforeSinceFetchResponse {
    pub response: DBFetchResponse,
    pub before: String,
    pub since: String,
    pub retry: bool,
}

#[derive(Serialize, Deserialize, Debug)]
pub struct SongActivityEntryForApiContract {
    #[serde(rename = "archivedVersion")]
    pub archived_version: Option<ArchivedObjectVersionForApiContract>,
    pub author: UserForApiContract,
    #[serde(rename = "createDate")]
    pub create_date: String,
    #[serde(rename = "editEvent")]
    pub edit_event: ActivityEditEvent,
    pub entry: ActivityEntryForApiContract,
}

#[derive(Serialize, Deserialize, Debug)]
pub struct SongsByEventTagFetchResponse {
    pub items: Vec<SongForApiContractSimplifiedWithMultipleEventInfo>,
    #[serde(rename = "totalCount")]
    pub total_count: i32,
    #[serde(rename = "releaseEvent")]
    pub release_event: ReleaseEventForApiContractSimplified,
    #[serde(rename = "eventTag")]
    pub event_tag: AssignableTag,
}

#[derive(Serialize, Deserialize, Debug)]
pub struct AssignEventAndRemoveTagPayload {
    #[serde(rename = "songId")]
    pub song_id: i32,
    pub event: MinimalEvent,
    #[serde(rename = "tagId")]
    pub tag_id: i64,
}

#[derive(Serialize, Deserialize, Debug)]
pub struct AssignEventPayload {
    #[serde(rename = "songId")]
    pub song_id: i32,
    pub event: MinimalEvent,
    #[serde(rename = "participatedOnUpload")]
    pub participated_on_upload: bool,
}

#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct MinimalEvent {
    pub id: i64,
    pub name: String,
    #[serde(rename = "urlSlug")]
    pub url_slug: String,
}

#[derive(Serialize, Deserialize, Debug)]
pub enum EventAssigningResult {
    Assigned,
    AlreadyAssigned,
    MultipleEvents,
    AlreadyTaggedWithMultipleEvents,
    Participated,
}

#[derive(Serialize, Deserialize, Debug)]
pub struct ReleaseEventWithNndTagsFetchResponse {
    pub event: ReleaseEventForApiContractSimplified,
    pub tags: Vec<String>,
}

#[derive(Serialize, Deserialize, Debug)]
pub struct EntriesForTagRemoval {
    pub items: Vec<EntryForTagRemoval>,
    #[serde(rename = "totalCount")]
    pub total_count: i32,
    #[serde(rename = "tagPool")]
    pub tag_pool: Vec<TagBaseContractSimplified>
}

#[derive(Serialize, Deserialize, Debug)]
pub struct EntryForTagRemoval {
    pub item: SongForApiContractSimplified,
    #[serde(rename = "toRemove")]
    pub to_remove: bool
}
