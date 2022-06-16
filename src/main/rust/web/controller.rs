use actix_web::{get, post, Responder, web};
use actix_web::http::header::Header;
use actix_web::HttpRequest;
use actix_web::web::Json;
use actix_web_httpauth::headers::authorization::{Authorization, Bearer};
use anyhow::Context;
use futures::future;
use log::debug;

use crate::client::client::Client;
use crate::web::errors::Result;
use crate::client::errors::VocadbClientError;

use crate::client::models::tag::{AssignableTag, TagBaseContract};

use crate::web::dto::{AssignTagRequest, LookupAndAssignTagRequest, Database, DBFetchRequest, DBBeforeSinceFetchRequest, LoginRequest, LoginResponse, TagFetchRequest, Token, VideosWithEntries, VideosWithEntriesByVocaDbTag, SongsByEventTagFetchRequest, SongsByEventTagFetchResponse};
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
        return Err(AppResponseError::ConstraintViolationError(String::from("Invalid arguments")));
    }

    let token = extract_token(&_req)?;
    let client = client_from_token(&token)?;

    let mapping = client.get_vocadb_mapping(payload.tag.clone()).await?;
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
                                        m.iter().map(|mm| mm.id.clone()).collect(),
                                        vec![payload.tag.clone()],
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

#[post("/fetch_by_tag")]
pub async fn fetch_videos_by_tag(_req: HttpRequest, payload: Json<TagFetchRequest>) -> Result<impl Responder> {
    if payload.start_offset < 0 || payload.max_results < 0 || payload.max_results > 100 {
        return Err(AppResponseError::ConstraintViolationError(String::from("Invalid arguments")));
    }

    let token = extract_token(&_req)?;
    let client = client_from_token(&token)?;

    let vocadb_tag = client.lookup_tag_by_name(payload.tag.clone()).await?;
    let tag_nico_mappings = client.get_nico_mappings(vocadb_tag.id.clone()).await?;
    match tag_nico_mappings {
        Some(nico_mappings) => {
            let scope_tag = nico_mappings.join(" OR ");
            let response = client
                .get_videos(scope_tag.clone(), payload.scope_tag.clone(), payload.start_offset, payload.max_results, payload.order_by.clone()).await?;

            let mappings = client.get_mapped_tags().await?;

            let futures = response.data.iter()
                .map(|video|
                    client.lookup_video(video,
                                        Vec::from(vec![vocadb_tag.id.clone()]),
                                        nico_mappings.clone(),
                                        &mappings, payload.scope_tag.clone()));

            let video_entries = future::try_join_all(futures).await?;

            return Ok(Json(VideosWithEntriesByVocaDbTag {
                items: video_entries,
                total_video_count: response.meta.total_count,
                tags: vec![vocadb_tag.clone()],
                tag_mappings: nico_mappings.clone(),
                safe_scope: payload.scope_tag.clone(),
            }));
        }
        None => Err(AppResponseError::NotFoundError)
    }
}

#[post("/fetch_from_db_before_since")]
pub async fn fetch_videos_from_db_before_since(_req: HttpRequest, payload: Json<DBBeforeSinceFetchRequest>) -> Result<impl Responder> {
    if payload.mode == "" && payload.date_time == "" || payload.max_results > 100 {
        return Err(AppResponseError::ConstraintViolationError(String::from("Invalid arguments")));
    }

    let token = extract_token(&_req)?;
    let client = client_from_token(&token)?;
    let songs = client.get_videos_from_db_before_since(payload.max_results, payload.mode.clone(), payload.date_time.clone(), payload.song_id.clone(), payload.sort_rule.clone()).await?;

    let processed_entries = client.process_songs_with_thumbnails(songs).await?;

    return Ok(Json(processed_entries));
}

#[post("/fetch_from_db")]
pub async fn fetch_videos_from_db(_req: HttpRequest, payload: Json<DBFetchRequest>) -> Result<impl Responder> {
    if payload.start_offset < 0 || payload.max_results < 0 || payload.max_results > 100 {
        return Err(AppResponseError::ConstraintViolationError(String::from("Invalid arguments")));
    }

    let token = extract_token(&_req)?;
    let client = client_from_token(&token)?;

    let songs = client.get_videos_from_db(payload.start_offset, payload.max_results, payload.order_by.clone()).await?;

    let processed_entries = client.process_songs_with_thumbnails(songs).await?;

    return Ok(Json(processed_entries));
}

#[post("/fetch_from_db_by_event_tag")]
pub async fn fetch_from_db_by_event_tag(_req: HttpRequest, payload: Json<SongsByEventTagFetchRequest>) -> Result<impl Responder> {
    if payload.start_offset < 0 || payload.max_results < 0 || payload.max_results > 100 {
        return Err(AppResponseError::ConstraintViolationError(String::from("Invalid arguments")));
    }

    let token = extract_token(&_req)?;
    let client = client_from_token(&token)?;

    let vocadb_tag = client.lookup_tag_by_name(payload.tag.clone()).await?;
    debug!("{}", vocadb_tag.id);
    let vocadb_event = client.get_event_by_tag(vocadb_tag.id).await?;
    debug!("got event");

    let songs = client.get_songs_by_vocadb_event_tag(vocadb_tag.id, payload.start_offset, payload.max_results, payload.order_by.clone()).await?;

    return Ok(Json(SongsByEventTagFetchResponse {
        items: songs.items,
        total_count: songs.total_count,
        release_event: vocadb_event,
        event_tag: vocadb_tag,
    }));
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
