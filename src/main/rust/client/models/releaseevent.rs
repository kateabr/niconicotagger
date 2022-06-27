use serde::{Deserialize, Serialize};

use crate::client::models::artist::ArtistForEventContract;
use crate::client::models::entrythumb::EntryThumbForApiContract;
use crate::client::models::misc::LocalizedStringContract;
use crate::client::models::song::SongListBaseContract;
use crate::client::models::status::Status;
use crate::client::models::weblink::{WebLinkContract, WebLinkForApiContract};

#[derive(Serialize, Deserialize, Debug, Clone)]
pub enum ReleaseEventCategory {
    Unspecified,
    AlbumRelease,
    Anniversary,
    Club,
    Concert,
    Contest,
    Convention,
    Other,
}

#[derive(Serialize, Deserialize, Debug)]
pub struct ReleaseEventForApiContract {
    #[serde(rename = "additionalNames")]
    additional_names: Option<String>,
    artists: Option<Vec<ArtistForEventContract>>,
    pub category: ReleaseEventCategory,
    pub date: Option<String>,
    description: Option<String>,
    #[serde(rename = "endDate")]
    pub end_date: Option<String>,
    pub id: i32,
    #[serde(rename = "mainPicture")]
    main_picture: Option<EntryThumbForApiContract>,
    pub name: String,
    names: Option<Vec<LocalizedStringContract>>,
    pub series: Option<ReleaseEventSeriesContract>,
    #[serde(rename = "seriesId")]
    series_id: Option<i32>,
    #[serde(rename = "seriesNumber")]
    series_number: i32,
    #[serde(rename = "seriesSuffix")]
    series_suffix: String,
    #[serde(rename = "songList")]
    song_list: Option<SongListBaseContract>,
    status: Status,
    #[serde(rename = "urlSlug")]
    pub url_slug: String,
    #[serde(rename = "venueName")]
    venue_name: Option<String>,
    version: i32,
    #[serde(rename = "webLinks")]
    pub web_links: Option<Vec<WebLinkForApiContract>>,
}

#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct ReleaseEventForApiContractSimplified {
    pub date: Option<String>,
    #[serde(rename = "endDate")]
    pub end_date: Option<String>,
    pub id: i32,
    pub name: String,
    #[serde(rename = "urlSlug")]
    pub url_slug: String,
    pub category: ReleaseEventCategory,
    #[serde(rename = "webLinks")]
    pub web_links: Option<Vec<WebLinkForApiContract>>,
}

#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct ReleaseEventForApiContractSimplifiedWithNndTags {
    pub(crate) event: ReleaseEventForApiContractSimplified,
    pub(crate) tags: Vec<String>
}

#[derive(Serialize, Deserialize, Debug)]
pub struct ReleaseEventSeriesContract {
    #[serde(rename = "additionalNames")]
    additional_names: String,
    pub category: ReleaseEventCategory,
    // deleted: bool,
    description: String,
    id: i32,
    name: String,
    #[serde(rename = "pictureMime")]
    picture_mime: String,
    status: Status,
    #[serde(rename = "urlSlug")]
    url_slug: String,
    version: i32,
    #[serde(rename = "webLinks")]
    web_links: Option<Vec<WebLinkContract>>,
}

#[derive(Serialize, Deserialize, Debug)]
pub struct EventSearchResult {
    pub items: Vec<ReleaseEventForApiContract>,
    #[serde(rename = "totalCount")]
    pub total_count: i32,
}
