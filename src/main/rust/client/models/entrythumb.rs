use serde::{Deserialize, Serialize};

use crate::client::models::album::DiscType;
use crate::client::models::archived::ArchivedWebLinkContract;
use crate::client::models::artist::ArtistType;
use crate::client::models::language::Language;
use crate::client::models::misc::LocalizedStringContract;
use crate::client::models::pv::PVContract;
use crate::client::models::song::{SongFeaturedCategory, SongType};
use crate::client::models::status::Status;
use crate::client::models::tag::TagUsageForApiContract;

#[derive(Serialize, Deserialize, Debug)]
pub struct EntryThumbForApiContract {
    mime: Option<String>,
    #[serde(rename = "urlSmallThumb")]
    url_small_thumb: Option<String>,
    #[serde(rename = "urlThumb")]
    url_thumb: Option<String>,
    #[serde(rename = "urlTinyThumb")]
    url_tiny_thumb: Option<String>,
}

#[derive(Serialize, Deserialize, Debug)]
pub struct EntryForApiContract {
    #[serde(rename = "activityDate")]
    pub activity_date: String,
    #[serde(rename = "additionalNames")]
    pub additional_names: String,
    #[serde(rename = "artistString")]
    pub artist_string: String,
    #[serde(rename = "artistType")]
    pub artist_type: ArtistType,
    #[serde(rename = "createDate")]
    pub create_date: String,
    #[serde(rename = "defaultName")]
    pub default_name: String,
    #[serde(rename = "defaultNameLanguage")]
    pub default_name_language: Language,
    pub description: String,
    #[serde(rename = "discType")]
    pub disc_type: DiscType,
    #[serde(rename = "entryType")]
    pub entry_type: EntryType,
    #[serde(rename = "eventCategory")]
    pub event_category: EventCategory,
    pub id: i32,
    #[serde(rename = "mainPicture")]
    pub main_picture: EntryThumbForApiContract,
    pub name: String,
    pub names: Vec<LocalizedStringContract>,
    #[serde(rename = "pVs")]
    pub pvs: Vec<PVContract>,
    #[serde(rename = "songListFeaturedCategory")]
    pub song_list_featured_category: SongFeaturedCategory,
    #[serde(rename = "songType")]
    pub song_type: SongType,
    pub status: Status,
    #[serde(rename = "releaseEventSeriesName")]
    pub release_event_series_name: String,
    #[serde(rename = "tagCategoryName")]
    pub tag_category_name: String,
    pub tags: Vec<TagUsageForApiContract>,
    #[serde(rename = "urlSlug")]
    pub url_slug: String,
    pub version: i32,
    #[serde(rename = "webLinks")]
    pub web_links: Vec<ArchivedWebLinkContract>,
}

#[derive(Serialize, Deserialize, Debug)]
pub struct EntryThumbContract {
    #[serde(rename = "entryType")]
    entry_type: Option<EntryType>,
    id: Option<i32>,
    mime: Option<String>,
    version: Option<i32>,
}

#[derive(Serialize, Deserialize, Debug)]
pub enum EntryType {
    Undefined,
    Album,
    Artist,
    DiscussionTopic,
    PV,
    ReleaseEvent,
    ReleaseEventSeries,
    Song,
    SongList,
    Tag,
    User,
}

#[derive(Serialize, Deserialize, Debug)]
pub enum EventCategory {
    Unspecified,
    AlbumRelease,
    Anniversary,
    Club,
    Concert,
    Contest,
    Convention,
    Other,
}
