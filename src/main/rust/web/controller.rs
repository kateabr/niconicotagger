use actix_web::{get, post, Responder, web};
use actix_web::http::header::Header;
use actix_web::HttpRequest;
use actix_web::web::Json;
use actix_web_httpauth::headers::authorization::{Authorization, Bearer};
use anyhow::Context;
use futures::future;

use crate::client::client::Client;
use crate::web::errors::Result;
use crate::client::errors::VocadbClientError;
use crate::client::jputils::normalize;
use crate::client::models::tag::{AssignableTag, TagBaseContract};
use crate::client::nicomodels::{NicoTagWithVariant, SongForApiContractWithThumbnailsAndMappedTags, ThumbnailOk, ThumbnailOkWithMappedTags, ThumbnailTagMappedWithAssignAndLockInfo};
use crate::web::dto::{AssignTagRequest, LookupAndAssignTagRequest, Database, DBFetchRequest, DBFetchResponse, LoginRequest, LoginResponse, TagFetchRequest, TagMappingContract, Token, VideosWithEntries};
use crate::web::errors::AppResponseError;
use crate::web::middleware::auth_token;

#[post("/users/current")]
pub async fn get_current_user(req: HttpRequest) -> Result<impl Responder> {
    let token = extract_token(&req)?;
    let client = client_from_token(&token)?;

    let user = client.current_user().await?;
    return Ok(web::Json(user));
}

#[post("/api/login")]
pub async fn login(payload: web::Json<LoginRequest>) -> Result<impl Responder> {
    let mut client = create_client(&payload.database, &vec![])?;
    client.login(&payload.username, &payload.password).await?;

    let user = client.current_user().await?;

    let token = auth_token::encode(user.id, &payload.database, &client.cookies)?;
    return Ok(web::Json(LoginResponse { token }));
}

#[post("/fetch")]
pub async fn fetch_videos(_req: HttpRequest, payload: Json<TagFetchRequest>) -> Result<impl Responder> {
    if payload.start_offset < 0 || payload.max_results < 0 || payload.max_results > 100 {
        return Err(AppResponseError::ConstraintViolationError(String::from("Invalid ")));
    }

    let token = extract_token(&_req)?;
    let client = client_from_token(&token)?;

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
                safe_scope: response.safe_scope,
            }));
        }
        None => Err(AppResponseError::NotFoundError)
    }
}

#[post("/fetch_from_db")]
pub async fn fetch_videos_from_db(_req: HttpRequest, payload: Json<DBFetchRequest>) -> Result<impl Responder> {
    if payload.start_offset < 0 || payload.max_results < 0 || payload.max_results > 100 {
        return Err(AppResponseError::ConstraintViolationError(String::from("Invalid arguments")));
    }

    let token = extract_token(&_req)?;
    let client = client_from_token(&token)?;

    let mappings = client.get_mappings_raw().await?;
    let mapped_tags: Vec<String> = mappings.iter().map(|m| m.source_tag.clone()).collect();
    let songs = client.get_videos_from_db(payload.start_offset, payload.max_results, payload.order_by.clone()).await?;

    let mut song_tags_to_map = vec![];

    for song in songs.items {
        let th_ok = map_thumbnail_tags(&song.thumbnails_ok, &mappings, &mapped_tags, &song.song.tags.iter().map(|t| t.tag.id).collect());
        song_tags_to_map.push(SongForApiContractWithThumbnailsAndMappedTags {
            song: song.song,
            thumbnails_error: song.thumbnails_error,
            thumbnails_ok: th_ok,
        });
    }

    return Ok(Json(DBFetchResponse { items: song_tags_to_map, total_count: songs.total_count}));
}

#[post("/assign")]
pub async fn assign_tag(_req: HttpRequest, payload: Json<AssignTagRequest>) -> Result<impl Responder> {
    if payload.song_id < 1 || payload.tags.iter().any(|tag| tag.id < 1) {
        return Err(AppResponseError::ConstraintViolationError(String::from("Invalid arguments")));
    }

    let token = extract_token(&_req)?;
    let client = client_from_token(&token)?;


    let current_tags = client.get_current_tags(payload.song_id).await?;
    let mut new_tags: Vec<TagBaseContract> = payload.tags.iter()
        .map(|pt| TagBaseContract {
            id: pt.id.clone(),
            name: pt.name.clone(),
            category_name: Some(pt.category_name.clone()),
            url_slug: pt.url_slug.clone(),
            additional_names: Some(pt.additional_names.clone()),
        }).collect();
    new_tags.extend(current_tags.iter().filter(|ct| ct.selected)
        .map(|ct| ct.tag.clone()));
    let response = client.assign(new_tags, payload.song_id).await?;

    return if response {
        Ok(Json(()))
    } else {
        Err(AppResponseError::VocadbClientError(VocadbClientError::NotFoundError))
    };
}

#[post("/lookup_and_assign")]
pub async fn lookup_and_assign_tag(_req: HttpRequest, payload: Json<LookupAndAssignTagRequest>) -> Result<impl Responder> {
    if payload.song_id < 1 || payload.tags.iter().any(|tag| tag.id < 1) {
        return Err(AppResponseError::ConstraintViolationError(String::from("Invalid arguments")));
    }

    let token = extract_token(&_req)?;
    let client = client_from_token(&token)?;

    let mut assignable_tags = vec![];
    for tag in &payload.tags {
        let assignable_tag = client.lookup_tag(tag.id).await?;
        assignable_tags.push(assignable_tag);
    }

    let current_tags = client.get_current_tags(payload.song_id).await?;
    let mut new_tags: Vec<TagBaseContract> = assignable_tags.iter()
        .map(|pt| TagBaseContract {
            id: pt.id.clone(),
            name: pt.name.clone(),
            category_name: Some(pt.category_name.clone()),
            url_slug: pt.url_slug.clone(),
            additional_names: Some(pt.additional_names.clone()),
        }).collect();
    new_tags.extend(current_tags.iter().filter(|ct| ct.selected)
        .map(|ct| ct.tag.clone()));

    let response = client.assign(new_tags, payload.song_id).await?;

    return if response {
        Ok(Json(()))
    } else {
        Err(AppResponseError::VocadbClientError(VocadbClientError::NotFoundError))
    };
}

#[get("/get_mapped_tags")]
pub async fn get_mapped_tags(_req: HttpRequest) -> Result<impl Responder> {
    let token = extract_token(&_req)?;
    let client = client_from_token(&token)?;

    let mapped_tags = client.get_mapped_tags().await?;

    return Ok(Json(mapped_tags));
}

fn map_thumbnail_tags(thumbnails: &Vec<ThumbnailOk>, mappings: &Vec<TagMappingContract>, mapped_tags: &Vec<String>, song_tag_ids: &Vec<i32>) -> Vec<ThumbnailOkWithMappedTags> {
    let mut res = vec![];
    for thumbnail in thumbnails {
        let mut tag_mappings = vec![];
        let mut mapped_nico_tags = vec![];
        for tag in &thumbnail.tags {
            if mapped_tags.contains(&normalize(tag.name.as_str())) {
                mapped_nico_tags.push(NicoTagWithVariant{name: tag.name.clone(), variant: String::from("dark")});
                let tag_mappings_temp = mappings.iter().filter(|m| m.source_tag == normalize(tag.name.as_str())).collect::<Vec<_>>();
                for m in tag_mappings_temp {
                    tag_mappings.push(ThumbnailTagMappedWithAssignAndLockInfo {
                        tag: m.tag.clone(),
                        locked: tag.locked,
                        assigned: song_tag_ids.contains(&m.tag.id),
                    });
                }
            } else {
                mapped_nico_tags.push(NicoTagWithVariant{name: tag.name.clone(), variant: String::from("secondary")});
            }
        }
        res.push(ThumbnailOkWithMappedTags {
            thumbnail: thumbnail.clone(),
            mapped_tags: tag_mappings,
            nico_tags: mapped_nico_tags
        });
    }

    return res;
}


fn extract_token(req: &HttpRequest) -> Result<Token> {
    let auth = Authorization::<Bearer>::parse(req).context("Unable to parse Bearer header")?;
    let token = auth_token::parse(auth.as_ref().token())?;
    return Ok(token);
}

fn client_from_token(token: &Token) -> Result<Client> {
    return Ok(create_client(&token.database, &token.cookies)?);
}

fn create_client<'a>(
    database: &Database,
    cookies: &Vec<String>,
) -> Result<Client<'a>, VocadbClientError> {
    return match database {
        Database::VocaDb => Client::vocadb(&cookies),
        Database::TouhouDb => Client::touhoudb(&cookies),
        Database::UtaiteDb => Client::utaitedb(&cookies),
    };
}