use std::convert::TryFrom;


use actix_web::{web};

use actix_web::http::header::Header;
use actix_web::HttpRequest;
use actix_web::web::Json;
use actix_web_httpauth::headers::authorization::{Authorization, Bearer};
use futures::future;
use serde::Deserialize;
use serde::Serialize;

use crate::client::client::Client;
use crate::client::errors::ClientError;


use crate::client::models::song::SongForApiContract;
use crate::client::models::tag::{AssignableTag, TagBaseContract};

use crate::errors::AppError;
use crate::service::auth::Claims;
use crate::service::auth::create_token;
use crate::service::auth::Database;

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
    pub order_by: String
}

#[derive(Deserialize, Debug)]
pub struct AssignTagRequest {
    #[serde(rename = "songId")]
    pub song_id: i32,
    pub tags: Vec<AssignableTag>
}

#[derive(Serialize)]
pub struct SongForApiContractSimplified {
    pub id: i32,
    pub name: String,
    #[serde(rename = "tagInTags")]
    pub tag_in_tags: bool,
    #[serde(rename = "songType")]
    pub song_type: String,
    #[serde(rename = "artistString")]
    pub artist_string: String
}

#[derive(Serialize, Deserialize, Debug)]
pub struct DisplayableTag {
    pub(crate) name: String,
    pub(crate) variant: String
}

#[derive(Serialize)]
pub struct VideoWithEntry {
    pub video: NicoVideoWithTidyTags,
    #[serde(rename = "songEntry")]
    pub song_entry: Option<SongForApiContractSimplified>
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
    pub safe_scope: String
}

#[derive(Serialize)]
pub struct TagFetchResponse {
    pub videos: Vec<Option<SongForApiContract>>,
    #[serde(rename = "totalCount")]
    pub total_count: i32,
}

#[derive(Serialize, Deserialize)]
pub struct NicoVideo {
    #[serde(rename = "contentId")]
    pub id: String,
    pub title: String,
    pub tags: String
}

#[derive(Serialize, Deserialize)]
pub struct NicoVideoWithTidyTags {
    #[serde(rename = "contentId")]
    pub id: String,
    pub title: String,
    pub tags: Vec<DisplayableTag>
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
    pub safe_scope: String
}

#[derive(Serialize, Deserialize)]
pub struct TagMappingContract {
    #[serde(rename = "sourceTag")]
    pub source_tag: String,
    pub tag: TagBaseContract,
}

#[derive(Deserialize, Debug)]
pub struct LoginRequest {
    pub username: String,
    pub password: String
    // pub database: Database,
}

#[derive(Serialize, Debug)]
pub struct LoginResponse {
    pub token: String,
}

pub async fn login(payload: web::Json<LoginRequest>) -> Result<Json<LoginResponse>, AppError> {
    let token = create_token(&payload.username, &payload.password, &Database::VocaDb).await?;
    return Ok(Json(LoginResponse { token }));
}

pub async fn fetch_videos(_req: HttpRequest, payload: Json<TagFetchRequest>) -> Result<Json<VideosWithEntries>, AppError> {
    if payload.start_offset < 0 || payload.max_results < 0 || payload.max_results > 100 {
        return Err(AppError::ConstraintViolationError);
    }

    let client = Client::new("", vec![])?;

    let mapping = client.get_mapping(payload.tag.clone()).await?;
    match mapping {
        Some(m) => {
            let mappings = client.get_mapped_tags().await?;

            let mut tag_information: Vec<AssignableTag> = Vec::new();

            for mapping in &m {
                tag_information.push(client.lookup_tag(mapping.id).await?);
            }

            let response = client
                .get_videos(payload.tag.clone(), payload.scope_tag.clone(), payload.start_offset, payload.max_results, payload.order_by.clone()).await?;

            let futures = response.data.iter()
                .map(|video|
                    client.lookup_video(video,
                                        m.iter().map(|mm| mm.name.clone()).collect(),
                                        payload.tag.clone(),
                                        &mappings, payload.scope_tag.clone()));

            let video_entries = future::try_join_all(futures).await?;

            let mappings = m.iter().map(|mapping| mapping.name.clone()).collect();
            return Ok(Json(VideosWithEntries {
                items: video_entries,
                total_video_count: response.meta.total_count,
                tags: tag_information,
                tag_mappings: mappings,
                safe_scope: response.safe_scope
            }));
        }
        None => Err(AppError::NoMappingError)
    }
}

pub async fn assign_tag(req: HttpRequest, payload: Json<AssignTagRequest>) -> Result<Json<()>, AppError> {
    if payload.song_id < 1 || payload.tags.iter().any(|tag| tag.id < 1) {
        return Err(AppError::ConstraintViolationError);
    }

    let claims = Claims::try_from(&req)?;
    let client = Client::new("", claims.cookies)?;
    let current_tags = client.get_current_tags(payload.song_id).await?;
    let mut new_tags: Vec<TagBaseContract> = payload.tags.iter()
        .map(|pt| TagBaseContract {
            id: pt.id.clone(),
            name: pt.name.clone(),
            category_name: Some(pt.category_name.clone()),
            url_slug: pt.url_slug.clone(),
            additional_names: Some(pt.additional_names.clone())
    }).collect();
    new_tags.extend(current_tags.iter().filter(|ct| ct.selected)
        .map(|ct| ct.tag.clone()));
    let response = client.assign(new_tags, payload.song_id).await?;

    return if response {
        Ok(Json(()))
    } else {
        Err(AppError::from(ClientError::ResponseError))
    }
}

pub async fn get_mapped_tags() -> Result<Json<Vec<String>>, AppError> {
    let client = Client::new("", vec![])?;

    let mapped_tags = client.get_mapped_tags().await?;

    return Ok(Json(mapped_tags));
}

impl TryFrom<&HttpRequest> for Client<'_> {
    type Error = AppError;

    fn try_from(req: &HttpRequest) -> Result<Self, Self::Error> {
        let claims = Claims::try_from(req)?;

        let client = Client::new(claims.database.url(), claims.cookies.clone())?;
        return Ok(client);
    }
}

impl TryFrom<&HttpRequest> for Claims {
    type Error = AppError;

    fn try_from(req: &HttpRequest) -> Result<Self, Self::Error> {
        let authorization = Authorization::<Bearer>::parse(req)?;
        let token = crate::service::auth::decode_token(authorization.as_ref().token())?;
        return Ok(token.claims);
    }
}
