use serde::{Deserialize, Serialize};

use crate::client::models::song::SongForApiContract;

#[derive(Debug, Serialize)]
pub struct ThumbnailError {
    pub id: String,
    pub code: String,
    pub description: Option<String>,
    pub title: String,
    pub disabled: bool,
    pub community: bool,
}

#[derive(Debug, Clone, Serialize)]
pub struct ThumbnailOk {
    pub id: String,
    pub title: String,
    pub description: Option<String>,
    pub upload_date: String,
    pub length: String,
    pub views: i32,
    pub tags: Vec<Tag>,
    pub user_id: String,
    pub user_nickname: String
}

#[derive(Debug, Clone, Serialize)]
pub struct Tag {
    pub name: String,
    pub locked: bool,
}

#[derive(Serialize)]
pub struct SongsForApiContractWithThumbnailsAndTimestamp {
    pub items: Vec<SongForApiContractWithThumbnails>,
    pub total_count: i32,
    #[serde(rename = "timestampFirst")]
    pub timestamp_first: String,
    #[serde(rename = "timestampLast")]
    pub timestamp_last: String,
}

#[derive(Serialize)]
pub struct SongForApiContractWithThumbnails {
    pub song: SongForApiContract,
    pub thumbnails_ok: Vec<ThumbnailOk>,
    pub thumbnails_error: Vec<ThumbnailError>,
}

#[derive(Serialize)]
pub struct SongForApiContractWithThumbnailsAndMappedTags {
    pub song: SongForApiContract,
    #[serde(rename = "thumbnailsOk")]
    pub thumbnails_ok: Vec<ThumbnailOkWithMappedTags>,
    #[serde(rename = "thumbnailsErr")]
    pub thumbnails_error: Vec<ThumbnailError>,
}

#[derive(Serialize)]
pub struct ThumbnailOkWithMappedTags {
    pub thumbnail: ThumbnailOk,
    #[serde(rename = "mappedTags")]
    pub mapped_tags: Vec<ThumbnailTagMappedWithAssignAndLockInfo>,
    #[serde(rename = "nicoTags")]
    pub nico_tags: Vec<NicoTagWithVariant>,
}

#[derive(Serialize)]
pub struct NicoTagWithVariant {
    pub name: String,
    pub variant: String,
    pub locked: bool,
}

#[derive(Serialize)]
pub struct ThumbnailTagMappedWithAssignAndLockInfo {
    pub tag: TagBaseContractSimplified,
    pub assigned: bool,
    pub locked: bool,
}

#[derive(Serialize, Clone, Deserialize, Debug)]
pub struct TagBaseContractSimplified {
    pub id: i32,
    pub name: String,
    #[serde(rename = "urlSlug")]
    pub url_slug: String,
}

#[derive(Serialize, Clone, Deserialize, Debug)]
pub struct TagBaseContractSimplifiedWithUsageCount {
    count: i32,
    pub tag: TagBaseContractSimplified,
}
