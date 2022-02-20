use serde::{Deserialize, Serialize};

use crate::client::models::song::SongForApiContract;
use crate::web::dto::DBFetchResponse;

#[derive(Debug, Serialize)]
pub struct ThumbnailError {
    pub id: String,
    pub code: String,
    pub description: String,
    pub title: String,
    pub disabled: bool,
}

#[derive(Debug, Clone, Serialize)]
pub struct ThumbnailOk {
    pub id: String,
    pub title: String,
    pub description: String,
    pub upload_date: String,
    pub length: String,
    pub views: i32,
    pub tags: Vec<Tag>,
}

#[derive(Debug, Clone, Serialize)]
pub struct Tag {
    pub name: String,
    pub locked: bool,
}

pub struct SongsForApiContractWithThumbnails {
    pub items: Vec<SongForApiContractWithThumbnails>,
    pub(crate) total_count: i32,
}

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
    pub locked: bool
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
