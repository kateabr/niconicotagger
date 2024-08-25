use actix_web::{get, post, Responder};
use actix_web::http::header::Header;
use actix_web::HttpRequest;
use actix_web::web::Json;
use actix_web_httpauth::headers::authorization::{Authorization, Bearer};
use anyhow::Context;
use chrono::{DateTime, Duration, FixedOffset};
use futures::future;
use log::debug;
use url::Url;

use crate::client::errors::VocadbClientError;
use crate::client::http_client::Client;
use crate::client::models::releaseevent::ReleaseEventForApiContractSimplifiedWithNndTags;
use crate::client::models::tag::{AssignableTag, TagBaseContract};
use crate::client::models::weblink::WebLinkForApiContract;
use crate::web::dto::{
    AssignEventAndRemoveTagPayload, AssignEventPayload, AssignTagRequest, CustomQueryPayload,
    Database, DBBeforeSinceFetchRequest, DBFetchRequest, EventByTagsFetchRequest,
    LoginRequest, LoginResponse, LookupAndAssignTagRequest, ReleaseEventWithNndTagsFetchRequest,
    SongsByEventTagFetchRequest, SongsByEventTagFetchResponse, TagFetchRequest, TagsRemovalPayload,
    Token, VideosWithEntries, VideosWithEntriesByVocaDbTag,
};
use crate::web::errors::AppResponseError;
use crate::web::errors::Result;
use crate::web::middleware::auth_token;

#[post("/users/current")]
pub async fn get_current_user(req: HttpRequest) -> Result<impl Responder> {
    let token = extract_token(&req)?;
    let client = client_from_token(&token)?;

    let user = client.current_user().await?;
    Ok(Json(user))
}

#[post("/api/login")]
pub async fn login(payload: Json<LoginRequest>) -> Result<impl Responder> {
    let mut client = create_client(&payload.database, &vec![])?;
    client.login(&payload.username, &payload.password).await?;

    let user = client.current_user().await?;

    let token = auth_token::encode(user.id, &payload.database, &client.cookies)?;
    Ok(Json(LoginResponse { token }))
}

#[post("/fetch")]
pub async fn fetch_videos(
    _req: HttpRequest,
    payload: Json<TagFetchRequest>,
) -> Result<impl Responder> {
    if payload.start_offset < 0 || payload.max_results < 0 || payload.max_results > 100 {
        return Err(AppResponseError::ConstraintViolationError(String::from(
            "Invalid arguments",
        )));
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
                .get_videos(
                    payload.tag.clone(),
                    payload.scope_tag.clone(),
                    payload.start_offset,
                    payload.max_results,
                    payload.order_by.clone(),
                    vec![],
                )
                .await?;

            let futures = response.data.iter().map(|video| {
                client.lookup_video(
                    video,
                    m.iter().map(|mm| mm.id).collect(),
                    vec![payload.tag.clone()],
                    &mappings,
                    payload.scope_tag.clone(),
                )
            });

            let video_entries = future::try_join_all(futures).await?;

            let mappings = m.iter().map(|mapping| mapping.name.clone()).collect();
            Ok(Json(VideosWithEntries {
                items: video_entries,
                total_video_count: response.meta.total_count,
                tags: tag_information,
                tag_mappings: mappings,
                safe_scope: response.safe_scope,
            }))
        }
        None => Err(AppResponseError::NotFoundError(format!(
            "tag \"{}\" is not mapped",
            payload.tag
        ))),
    }
}

#[post("/fetch_by_tag")]
pub async fn fetch_videos_by_tag(
    _req: HttpRequest,
    payload: Json<TagFetchRequest>,
) -> Result<impl Responder> {
    if payload.start_offset < 0 || payload.max_results < 0 || payload.max_results > 100 {
        return Err(AppResponseError::ConstraintViolationError(String::from(
            "Invalid arguments",
        )));
    }

    let token = extract_token(&_req)?;
    let client = client_from_token(&token)?;

    let vocadb_tag = client.lookup_tag_by_name(payload.tag.clone()).await?;
    let tag_nico_mappings = client.get_nico_mappings(vocadb_tag.id).await?;
    match tag_nico_mappings {
        Some(nico_mappings) => {
            let scope_tag = nico_mappings.join(" OR ");
            let response = client
                .get_videos(
                    scope_tag.clone(),
                    payload.scope_tag.clone(),
                    payload.start_offset,
                    payload.max_results,
                    payload.order_by.clone(),
                    vec![],
                )
                .await?;

            let mappings = client.get_mapped_tags().await?;

            let futures = response.data.iter().map(|video| {
                client.lookup_video(
                    video,
                    vec![vocadb_tag.id],
                    nico_mappings.clone(),
                    &mappings,
                    payload.scope_tag.clone(),
                )
            });

            let video_entries = future::try_join_all(futures).await?;

            Ok(Json(VideosWithEntriesByVocaDbTag {
                items: video_entries,
                total_video_count: response.meta.total_count,
                tags: vec![vocadb_tag.clone()],
                tag_mappings: nico_mappings.clone(),
                safe_scope: payload.scope_tag.clone(),
            }))
        }
        None => Err(AppResponseError::NotFoundError(format!(
            "tag \"{}\" is not mapped",
            payload.tag.clone()
        ))),
    }
}

#[post("/fetch_from_db_before_since")]
pub async fn fetch_videos_from_db_before_since(
    _req: HttpRequest,
    payload: Json<DBBeforeSinceFetchRequest>,
) -> Result<impl Responder> {
    if payload.mode.is_empty() && payload.date_time.is_empty() || payload.max_results > 100 {
        return Err(AppResponseError::ConstraintViolationError(String::from(
            "Invalid arguments",
        )));
    }

    let token = extract_token(&_req)?;
    let client = client_from_token(&token)?;
    let songs = client
        .get_videos_from_db_before_since(
            payload.max_results,
            payload.mode.clone(),
            payload.date_time.clone(),
            payload.song_id,
            payload.sort_rule.clone(),
        )
        .await?;

    let processed_entries = client.process_songs_with_thumbnails(songs).await?;

    Ok(Json(processed_entries))
}

#[post("/fetch_from_db")]
pub async fn fetch_videos_from_db(
    _req: HttpRequest,
    payload: Json<DBFetchRequest>,
) -> Result<impl Responder> {
    if payload.start_offset < 0 || payload.max_results < 0 || payload.max_results > 100 {
        return Err(AppResponseError::ConstraintViolationError(String::from(
            "Invalid arguments",
        )));
    }

    let token = extract_token(&_req)?;
    let client = client_from_token(&token)?;

    let songs = client
        .get_videos_from_db(
            payload.start_offset,
            payload.max_results,
            payload.order_by.clone(),
        )
        .await?;

    let processed_entries = client.process_songs_with_thumbnails(songs).await?;

    Ok(Json(processed_entries))
}

#[post("/fetch_from_db_by_event_tag")]
pub async fn fetch_from_db_by_event_tag(
    _req: HttpRequest,
    payload: Json<SongsByEventTagFetchRequest>,
) -> Result<impl Responder> {
    if payload.start_offset < 0 || payload.max_results < 0 || payload.max_results > 100 {
        return Err(AppResponseError::ConstraintViolationError(String::from(
            "Invalid arguments",
        )));
    }

    let token = extract_token(&_req)?;
    let client = client_from_token(&token)?;

    let vocadb_tag = client.lookup_tag_by_name(payload.tag.clone()).await?;
    if vocadb_tag.category_name != "Event" {
        return Err(AppResponseError::BadRequestError(format!(
            "\"{}\" is not an event",
            vocadb_tag.name
        )));
    }
    let vocadb_event = client
        .get_event_by_tag(vocadb_tag.id, vocadb_tag.name.as_str())
        .await?;

    let songs = client
        .get_songs_by_vocadb_event_tag(
            vocadb_tag.id,
            payload.start_offset,
            payload.max_results,
            payload.order_by.clone(),
        )
        .await?;

    Ok(Json(SongsByEventTagFetchResponse {
        items: songs.items,
        total_count: songs.total_count,
        release_event: vocadb_event,
        event_tag: vocadb_tag,
    }))
}

#[post("/assign_event_and_remove_tag")]
pub async fn assign_event_and_remove_tag(
    _req: HttpRequest,
    payload: Json<AssignEventAndRemoveTagPayload>,
) -> Result<impl Responder> {
    if payload.song_id < 0
        || payload.event.id < 0
        || payload.event.name.is_empty()
        || payload.tag_id < 0
    {
        return Err(AppResponseError::ConstraintViolationError(String::from(
            "Invalid arguments",
        )));
    }

    let token = extract_token(&_req)?;
    let client = client_from_token(&token)?;

    client.edit_entry(payload.song_id, payload.event.clone()).await?;
    client
        .remove_tag(payload.song_id, String::from("Song"), payload.tag_id)
        .await?;

    Ok(Json(()))
}

#[post("/assign_event")]
pub async fn assign_event(
    _req: HttpRequest,
    payload: Json<AssignEventPayload>,
) -> Result<impl Responder> {
    if payload.song_id < 0
        || payload.event.id < 0
        || payload.event.name.is_empty()
    {
        return Err(AppResponseError::ConstraintViolationError(String::from(
            "Invalid arguments",
        )));
    }

    let token = extract_token(&_req)?;
    let client = client_from_token(&token)?;

    client.edit_entry(payload.song_id, payload.event.clone()).await?;

    Ok(Json(()))
}

#[post("/fetch_release_event_with_nnd_tags")]
pub async fn fetch_release_event_with_nnd_tags(
    _req: HttpRequest,
    payload: Json<ReleaseEventWithNndTagsFetchRequest>,
) -> Result<impl Responder> {
    pub fn clean_nnd_links(links: Vec<WebLinkForApiContract>) -> Result<Vec<String>> {
        let mut result: Vec<String> = vec![];
        for link in links {
            let parsed_link =
                Url::parse(&link.url).context(format!("invalid url: \"{}\"", &link.url))?;
            let host = parsed_link
                .host_str()
                .context(format!("invalid url: \"{}\"", &link.url))?;
            if host != "nicovideo.jp" && host != "www.nicovideo.jp" {
                continue;
            }
            let path_segments = parsed_link
                .path_segments()
                .map(|c| c.collect::<Vec<_>>())
                .context(format!("invalid url: \"{}\"", &link.url))?;
            if path_segments[0] != "tag" {
                continue;
            }
            if path_segments.len() < 2 {
                continue;
            }
            let tag = url_escape::decode(path_segments[1])
                .replace(
                    String::from("%23").as_str(),
                    String::from("#").as_str()).to_string();
            result.push(tag);
        }
        Ok(result)
    }

    if payload.event_name.is_empty() {
        return Err(AppResponseError::ConstraintViolationError(String::from(
            "Invalid arguments",
        )));
    }

    let token = extract_token(&_req)?;
    let client = client_from_token(&token)?;

    let event = client.get_event_by_name(payload.event_name.clone()).await?;
    let links = event
        .web_links
        .clone()
        .ok_or(AppResponseError::NotFoundError(format!(
            "event \"{}\" does not have any associated NND tags",
            payload.event_name.clone()
        )))?;
    let clean_tags = clean_nnd_links(links)?;
    if !clean_tags.is_empty() {
        Ok(Json(ReleaseEventForApiContractSimplifiedWithNndTags {
            event,
            tags: clean_tags,
        }))
    } else {
        Err(AppResponseError::BadRequestError(format!(
            "event \"{}\" has no associated NND tags",
            payload.event_name
        )))
    }
}

#[post("/fetch_videos_by_event_nnd_tags")]
pub async fn fetch_videos_by_event_nnd_tags(
    _req: HttpRequest,
    payload: Json<EventByTagsFetchRequest>,
) -> Result<impl Responder> {
    if payload.start_offset < 0
        || payload.max_results < 0
        || payload.max_results > 100
        || payload.event_id < 0
        || payload.tags.is_empty()
        || payload.order_by.is_empty()
    {
        return Err(AppResponseError::ConstraintViolationError(String::from(
            "Invalid arguments",
        )));
    }

    let token = extract_token(&_req)?;
    let client = client_from_token(&token)?;

    let mappings = client.get_mapped_tags().await?;
    let time_bound: Vec<String> = match &payload.start_time {
        None => vec![],
        Some(start_time) => match &payload.end_time {
            None => {
                let ref_time = DateTime::parse_from_rfc3339(start_time)
                    .context(format!("Unable to parse {}", &start_time))?;

                vec![
                    (ref_time.with_timezone(&FixedOffset::east(9 * 3600)) - Duration::days(7))
                        .to_rfc3339(),
                    (ref_time.with_timezone(&FixedOffset::east(9 * 3600)) + Duration::days(7))
                        .to_rfc3339(),
                ]
            }
            Some(end_time) => {
                let ref_time_start = DateTime::parse_from_rfc3339(start_time)
                    .context(format!("Unable to parse {}", &start_time))?;
                let ref_time_end = DateTime::parse_from_rfc3339(end_time)
                    .context(format!("Unable to parse {}", &end_time))?;

                vec![
                    (ref_time_start.with_timezone(&FixedOffset::east(9 * 3600))
                        - Duration::days(1))
                    .to_rfc3339(),
                    (ref_time_end.with_timezone(&FixedOffset::east(9 * 3600)) + Duration::days(1))
                        .to_rfc3339(),
                ]
            }
        },
    };
    let response = client
        .get_videos(
            payload.tags.clone(),
            payload.scope_tag.clone(),
            payload.start_offset,
            payload.max_results,
            payload.order_by.clone(),
            time_bound,
        )
        .await?;

    let futures = response.data.iter().map(|video| {
        client.lookup_video_by_event(
            video,
            payload.event_id,
            payload.tags.split(" OR ").map(String::from).collect(),
            &mappings,
            payload.scope_tag.clone(),
        )
    });

    let video_entries = future::try_join_all(futures).await?;

    return Ok(Json(VideosWithEntries {
        items: video_entries,
        total_video_count: response.meta.total_count,
        tags: vec![],
        tag_mappings: payload.tags.split(' ').map(|spl| spl.to_string()).collect(),
        safe_scope: response.safe_scope,
    }));
}

#[post("/assign")]
pub async fn assign_tag(
    _req: HttpRequest,
    payload: Json<AssignTagRequest>,
) -> Result<impl Responder> {
    if payload.song_id < 1 || payload.tags.iter().any(|tag| tag.id < 1) {
        return Err(AppResponseError::ConstraintViolationError(String::from(
            "Invalid arguments",
        )));
    }

    let token = extract_token(&_req)?;
    let client = client_from_token(&token)?;

    let new_tags: Vec<TagBaseContract> = payload
        .tags
        .iter()
        .map(|pt| TagBaseContract {
            id: pt.id,
            name: pt.name.clone(),
            category_name: Some(pt.category_name.clone()),
            url_slug: pt.url_slug.clone(),
            additional_names: Some(pt.additional_names.clone()),
        })
        .collect();
    let response = client.add_tags(new_tags, payload.song_id).await;

    if response.is_ok() {
        Ok(Json(()))
    } else {
        Err(AppResponseError::VocadbClientError(
            VocadbClientError::NotFoundError(format!(
                "song with id=\"{}\" does not exist",
                payload.song_id
            )),
        ))
    }
}

#[post("/lookup_and_assign")]
pub async fn lookup_and_assign_tag(
    _req: HttpRequest,
    payload: Json<LookupAndAssignTagRequest>,
) -> Result<impl Responder> {
    if payload.song_id < 1 || payload.tags.iter().any(|tag| tag.id < 1) {
        return Err(AppResponseError::ConstraintViolationError(String::from(
            "Invalid arguments",
        )));
    }

    let token = extract_token(&_req)?;
    let client = client_from_token(&token)?;

    let mut assignable_tags = vec![];
    for tag in &payload.tags {
        let assignable_tag = client.lookup_tag(tag.id).await?;
        assignable_tags.push(assignable_tag);
    }

    let new_tags: Vec<TagBaseContract> = assignable_tags
        .iter()
        .map(|pt| TagBaseContract {
            id: pt.id,
            name: pt.name.clone(),
            category_name: Some(pt.category_name.clone()),
            url_slug: pt.url_slug.clone(),
            additional_names: Some(pt.additional_names.clone()),
        })
        .collect();
    let response = client.add_tags(new_tags, payload.song_id).await;

    if response.is_ok() {
        if !payload.disable.is_empty() {
            client
                .disable_videos(payload.song_id, payload.disable.clone())
                .await?;
        }
        Ok(Json(()))
    } else {
        Err(AppResponseError::VocadbClientError(
            VocadbClientError::NotFoundError(format!(
                "song with id=\"{}\" does not exist",
                payload.song_id
            )),
        ))
    }
}

#[get("/get_mapped_tags")]
pub async fn get_mapped_tags(_req: HttpRequest) -> Result<impl Responder> {
    let token = extract_token(&_req)?;
    let client = client_from_token(&token)?;

    let mapped_tags = client.get_mapped_tags().await?;

    Ok(Json(mapped_tags))
}

#[post("/fetch_songs_for_tag_removal")]
pub async fn fetch_songs_for_tag_removal(
    _req: HttpRequest,
    payload: Json<CustomQueryPayload>,
) -> Result<impl Responder> {
    let token = extract_token(&_req)?;
    let client = client_from_token(&token)?;
    Ok(Json(
        client
            .fetch_songs_by_custom_query(payload.query.clone(), payload.db_address.clone())
            .await?,
    ))
}

#[post("/fetch_artists_for_tag_removal")]
pub async fn fetch_artists_for_tag_removal(
    _req: HttpRequest,
    payload: Json<CustomQueryPayload>,
) -> Result<impl Responder> {
    let token = extract_token(&_req)?;
    let client = client_from_token(&token)?;
    let json = Json(
        client
            .fetch_artists_by_custom_query(payload.query.clone(), payload.db_address.clone())
            .await?,
    );
    debug!("{:?}", json);
    Ok(json)
}

#[post("/remove_tags")]
pub async fn remove_tags(
    _req: HttpRequest,
    payload: Json<TagsRemovalPayload>,
) -> Result<impl Responder> {
    let token = extract_token(&_req)?;
    let client = client_from_token(&token)?;

    return if payload.mode == "songs" {
        let futures = payload
            .tag_ids
            .iter()
            .map(|&tag_id| client.remove_tag(payload.id, String::from("Song"), tag_id));
        future::try_join_all(futures).await?;
        Ok(Json(()))
    } else if payload.mode == "artists" {
        let futures = payload
            .tag_ids
            .iter()
            .map(|&tag_id| client.remove_tag(payload.id, String::from("Artist"), tag_id));
        future::try_join_all(futures).await?;
        Ok(Json(()))
    } else {
        Err(AppResponseError::BadRequestError(format!(
            "Unknown tag removal mode: {}",
            payload.mode
        )))
    };
}

fn extract_token(req: &HttpRequest) -> Result<Token> {
    let auth = Authorization::<Bearer>::parse(req).context("Unable to parse Bearer header")?;
    let token = auth_token::parse(auth.as_ref().token())?;
    Ok(token)
}

fn client_from_token(token: &Token) -> Result<Client> {
    Ok(create_client(&token.database, &token.cookies)?)
}

fn create_client<'a>(
    database: &Database,
    cookies: &Vec<String>,
) -> Result<Client<'a>, VocadbClientError> {
    return match database {
        Database::VocaDb => Client::vocadb(cookies),
        Database::VocaDbBeta => Client::vocadb_beta(cookies),
        Database::TouhouDb => Client::touhoudb(cookies),
        Database::UtaiteDb => Client::utaitedb(cookies),
    };
}
