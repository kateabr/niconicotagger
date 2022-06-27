use actix_web::{get, post, Responder};
use actix_web::http::header::Header;
use actix_web::HttpRequest;
use actix_web::web::Json;
use actix_web_httpauth::headers::authorization::{Authorization, Bearer};
use anyhow::Context;
use futures::future;
use url::Url;

use crate::client::client::Client;
use crate::client::errors::VocadbClientError;
use crate::client::models::releaseevent::ReleaseEventForApiContractSimplifiedWithNndTags;
use crate::client::models::tag::{AssignableTag, TagBaseContract};
use crate::client::models::weblink::WebLinkForApiContract;
use crate::web::dto::{AssignEventAndRemoveTagPayload, AssignTagRequest, Database, DBBeforeSinceFetchRequest, DBFetchRequest, ReleaseEventWithNndTagsFetchRequest, EventAssigningResult, LoginRequest, LoginResponse, LookupAndAssignTagRequest, SongsByEventTagFetchRequest, SongsByEventTagFetchResponse, TagFetchRequest, Token, VideosWithEntries, VideosWithEntriesByVocaDbTag, EventByTagsFetchRequest, AssignEventPayload};
use crate::web::errors::AppResponseError;
use crate::web::errors::Result;
use crate::web::middleware::auth_token;

#[post("/users/current")]
pub async fn get_current_user(req: HttpRequest) -> Result<impl Responder> {
    let token = extract_token(&req)?;
    let client = client_from_token(&token)?;

    let user = client.current_user().await?;
    return Ok(Json(user));
}

#[post("/api/login")]
pub async fn login(payload: Json<LoginRequest>) -> Result<impl Responder> {
    let mut client = create_client(&payload.database, &vec![])?;
    client.login(&payload.username, &payload.password).await?;

    let user = client.current_user().await?;

    let token = auth_token::encode(user.id, &payload.database, &client.cookies)?;
    return Ok(Json(LoginResponse { token }));
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
        None => Err(AppResponseError::NotFoundError(format!("tag \"{}\" is not mapped", payload.tag)))
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
        None => Err(AppResponseError::NotFoundError(format!("tag \"{}\" is not mapped", payload.tag.clone())))
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
    if vocadb_tag.category_name != "Event" {
        return Err(AppResponseError::BadRequestError(format!("\"{}\" is not an event", vocadb_tag.name)));
    }
    let vocadb_event = client.get_event_by_tag(vocadb_tag.id).await?;

    let songs = client.get_songs_by_vocadb_event_tag(vocadb_tag.id, payload.start_offset, payload.max_results, payload.order_by.clone()).await?;

    return Ok(Json(SongsByEventTagFetchResponse {
        items: songs.items,
        total_count: songs.total_count,
        release_event: vocadb_event,
        event_tag: vocadb_tag,
    }));
}

#[post("/assign_event_and_remove_tag")]
pub async fn assign_event_and_remove_tag(_req: HttpRequest, payload: Json<AssignEventAndRemoveTagPayload>) -> Result<impl Responder> {
    if payload.song_id < 0 || payload.event.id < 0 || payload.event.name.is_empty() || payload.event.url_slug.is_empty() || payload.tag_id < 0 {
        return Err(AppResponseError::ConstraintViolationError(String::from("Invalid arguments")));
    }

    let token = extract_token(&_req)?;
    let client = client_from_token(&token)?;

    let result = client.fill_in_event(payload.song_id, payload.event.clone()).await?;
    match result {
        EventAssigningResult::MultipleEvents => {
            let response_code = client.assign(vec![TagBaseContract {
                id: 8275,
                name: String::from("multiple events"),
                category_name: Some(String::from("Editor notes")),
                additional_names: Some(String::from("")),
                url_slug: String::from("multiple-events")
            }], payload.song_id).await;
            if response_code.is_err() {
                return Err(
                    AppResponseError::VocadbClientError(
                        VocadbClientError::NotFoundError(
                            format!("Could not find tag \"multiple events\" (id={})", 8275)
                        )
                    )
                );
            }
        }
        EventAssigningResult::AlreadyTaggedWithMultipleEvents => {
            // did nothing, but need to tell the user to check whether song description mentions the event
        }
        _ => {
            // EventAssigningResult::Assigned => filled the event
            // EventAssigningResult::AlreadyAssigned => the event is already filled
        }
    }
    client.remove_tag(payload.song_id, payload.tag_id).await?;

    return Ok(Json(result));
}

#[post("/assign_event")]
pub async fn assign_event(_req: HttpRequest, payload: Json<AssignEventPayload>) -> Result<impl Responder> {
    if payload.song_id < 0 || payload.event.id < 0 || payload.event.name.is_empty() || payload.event.url_slug.is_empty() {
        return Err(AppResponseError::ConstraintViolationError(String::from("Invalid arguments")));
    }

    let token = extract_token(&_req)?;
    let client = client_from_token(&token)?;

    let result = client.fill_in_event(payload.song_id, payload.event.clone()).await?;
    match result {
        EventAssigningResult::MultipleEvents => {
            let response_code = client.assign(vec![TagBaseContract {
                id: 8275,
                name: String::from("multiple events"),
                category_name: Some(String::from("Editor notes")),
                additional_names: Some(String::from("")),
                url_slug: String::from("multiple-events")
            }], payload.song_id).await;
            if response_code.is_err() {
                return Err(
                    AppResponseError::VocadbClientError(
                        VocadbClientError::NotFoundError(
                            format!("Could not find tag \"multiple events\" (id={})", 8275)
                        )
                    )
                );
            }
        }
        EventAssigningResult::AlreadyTaggedWithMultipleEvents => {
            // did nothing, but need to tell the user to check whether song description mentions the event
        }
        _ => {
            // EventAssigningResult::Assigned => filled the event
            // EventAssigningResult::AlreadyAssigned => the event is already filled
        }
    }

    return Ok(Json(result));
}

#[post("/fetch_release_event_with_nnd_tags")]
pub async fn fetch_release_event_with_nnd_tags(_req: HttpRequest, payload: Json<ReleaseEventWithNndTagsFetchRequest>) -> Result<impl Responder> {
    pub fn clean_nnd_links(links: Vec<WebLinkForApiContract>) -> Result<Vec<String>> {
        let mut result: Vec<String> = vec![];
        for link in links {
            let parsed_link = Url::parse(&link.url).context(format!("invalid url: \"{}\"", &link.url))?;
            let host = parsed_link.host_str().context(format!("invalid url: \"{}\"", &link.url))?;
            if host != "nicovideo.jp" && host != "www.nicovideo.jp" { continue }
            let path_segments = parsed_link.path_segments().map(|c| c.collect::<Vec<_>>()).context(format!("invalid url: \"{}\"", &link.url))?;
            if path_segments[0] != "tag" { continue }
            if path_segments.len() < 2 { continue }
            let tag = url_escape::decode(path_segments[1]).to_string();
            result.push(tag);
        }
        return Ok(result);
    }

    if payload.event_name.is_empty() {
        return Err(AppResponseError::ConstraintViolationError(String::from("Invalid arguments")));
    }

    let token = extract_token(&_req)?;
    let client = client_from_token(&token)?;

    let event = client.get_event_by_name(payload.event_name.clone()).await?;
    let links = event.web_links.clone()
        .ok_or(AppResponseError::NotFoundError(
            format!(
                "event \"{}\" does not have any associated NND tags",
                payload.event_name.clone()
            )
        ))?;
    let clean_tags = clean_nnd_links(links)?;
    if !clean_tags.is_empty() {
        Ok(Json(ReleaseEventForApiContractSimplifiedWithNndTags {
            event,
            tags: clean_tags.clone()
        }))
    } else {
        Err(AppResponseError::BadRequestError(format!("event \"{}\" has no associated NND tags", payload.event_name)))
    }
}

#[post("/fetch_videos_by_event_nnd_tags")]
pub async fn fetch_videos_by_event_nnd_tags(_req: HttpRequest, payload: Json<EventByTagsFetchRequest>) -> Result<impl Responder> {
    if payload.start_offset < 0 || payload.max_results < 0 || payload.max_results > 100 || payload.event_id < 0 || payload.tags.is_empty() || payload.order_by.is_empty() {
        return Err(AppResponseError::ConstraintViolationError(String::from("Invalid arguments")));
    }

    let token = extract_token(&_req)?;
    let client = client_from_token(&token)?;

    let mappings = client.get_mapped_tags().await?;
    let response = client
                .get_videos(payload.tags.clone(), payload.scope_tag.clone(), payload.start_offset, payload.max_results, payload.order_by.clone()).await?;

    let futures = response.data.iter()
                .map(|video|
                    client.lookup_video_by_event(video,
                                        payload.event_id,
                                        payload.tags.split(" OR ").map(|tag| String::from(tag)).collect(),
                                        &mappings, payload.scope_tag.clone()));

            let video_entries = future::try_join_all(futures).await?;

            return Ok(Json(VideosWithEntries {
                items: video_entries,
                total_video_count: response.meta.total_count,
                tags: vec![],
                tag_mappings: payload.tags.split(" ").map(|spl| spl.to_string()).collect(),
                safe_scope: response.safe_scope,
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
    let response = client.assign(new_tags, payload.song_id).await;

    return if response.is_ok() {
        Ok(Json(()))
    } else {
        Err(AppResponseError::VocadbClientError(VocadbClientError::NotFoundError(format!("song with id=\"{}\" does not exist", payload.song_id))))
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

    let response = client.assign(new_tags, payload.song_id).await;

    return if response.is_ok() {
        Ok(Json(()))
    } else {
        Err(AppResponseError::VocadbClientError(VocadbClientError::NotFoundError(format!("song with id=\"{}\" does not exist", payload.song_id))))
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
