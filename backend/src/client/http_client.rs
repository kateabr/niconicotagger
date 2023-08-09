extern crate kana;

use std::cmp::min;
use std::collections::HashMap;
use std::ops::Deref;
use std::str::FromStr;
use std::time::Duration;

use actix_web::cookie::Cookie;
use actix_web::http::Method;
use actix_web::web::Bytes;
use anyhow::Context;
use awc::ClientRequest;
use chrono::{DateTime, DurationRound, FixedOffset};
use itertools::Itertools;
use log::{debug, info};
use regex::Regex;
use roxmltree::Document;
use scraper::node::Element;
use scraper::{ElementRef, Node};
use serde::de::DeserializeOwned;
use serde::Serialize;
use serde_json::{Map, Value};

use crate::client::errors::Result;
use crate::client::errors::VocadbClientError;

use crate::client::jputils::normalize;
use crate::client::models::activity::{ActivityEditEvent, ActivityEntryForApiContract};
use crate::client::models::artist::{
    ArtistCategories, ArtistForSongContract, ArtistRoles, NicoArtistDuplicateResult, NicoPublisher,
};
use crate::client::models::entrythumb::EntryType;
use crate::client::models::misc::PartialFindResult;
use crate::client::models::pv::{PVContract, PvService, PvType};
use crate::client::models::query::OptionalFields;
use crate::client::models::releaseevent::{
    EventSearchResult, ReleaseEventForApiContractSimplified, ReleaseEventSeriesContract,
};
use crate::client::models::song::SongForApiContract;
use crate::client::models::tag::{
    AssignableTag, SelectedTag, TagBaseContract, TagForApiContract, TagSearchResult,
    TagUsageForApiContract,
};
use crate::client::models::user::UserForApiContract;
use crate::client::models::weblink::WebLinkForApiContract;
use crate::client::nicomodels::{
    NicoTagWithVariant, SongForApiContractWithThumbnails,
    SongForApiContractWithThumbnailsAndMappedTags, SongsForApiContractWithThumbnailsAndTimestamp,
    Tag, TagBaseContractSimplified, ThumbnailError, ThumbnailOk, ThumbnailOkWithMappedTags,
    ThumbnailTagMappedWithAssignAndLockInfo,
};
use crate::web::dto::{
    ArtistEntriesForTagRemoval, ArtistEntryForTagRemoval, ArtistForApiContractSimplified,
    ArtistForApiContractSimplifiedWithTagUsageCounts, DBFetchResponseWithTimestamps,
    DisplayableTag, MinimalEvent, NicoResponse, NicoResponseWithScope, NicoVideo,
    NicoVideoWithTidyTags, SongEntriesForTagRemoval, SongEntryForTagRemoval,
    SongForApiContractSimplified, SongForApiContractSimplifiedWithMultipleEventInfo,
    SongForApiContractSimplifiedWithMultipleEventInfoSearchResult,
    SongForApiContractSimplifiedWithTagUsageCounts, TagMappingContract, VideoWithEntry,
};

pub struct Client<'a> {
    pub client: awc::Client,
    pub base_url: &'a str,
    pub cookies: Vec<Cookie<'a>>,
}

impl<'a> Client<'a> {
    pub fn vocadb(cookies: &Vec<String>) -> Result<Client<'a>> {
        Client::new("https://vocadb.net", cookies)
    }

    pub fn vocadb_beta(cookies: &Vec<String>) -> Result<Client<'a>> {
        Client::new("https://beta.vocadb.net", cookies)
    }

    pub fn touhoudb(cookies: &Vec<String>) -> Result<Client<'a>> {
        Client::new("https://touhoudb.com", cookies)
    }

    pub fn utaitedb(cookies: &Vec<String>) -> Result<Client<'a>> {
        Client::new("https://utaitedb.net", cookies)
    }

    fn new(base_url: &'a str, cookies: &Vec<String>) -> Result<Client<'a>> {
        let cookies = cookies.clone();
        let mut parsed_cookies = vec![];
        for c in cookies {
            let cookie = Cookie::parse(c).context("Unable to parse a cookie")?;
            parsed_cookies.push(cookie)
        }

        let awc_client = awc::Client::builder()
            .connector(awc::Connector::new().timeout(Duration::from_secs(30)))
            .timeout(Duration::from_secs(30))
            .max_redirects(0)
            .finish();

        Ok(Client {
            client: awc_client,
            base_url,
            cookies: parsed_cookies,
        })
    }

    fn add_cookies(&mut self, cookies: &Vec<Cookie<'a>>) -> &Client {
        for cookie in cookies {
            self.cookies.push(cookie.clone());
        }
        self
    }

    fn clear_cookie(&mut self) -> &Client {
        self.cookies.clear();
        self
    }

    fn create_request(&self, url: &String, method: Method) -> awc::ClientRequest {
        let mut builder = match method {
            Method::GET => self.client.get(url),
            Method::POST => self.client.post(url),
            Method::DELETE => self.client.delete(url),
            Method::PUT => self.client.put(url),
            _ => panic!("Unsupported method"),
        };

        for cookie in &self.cookies {
            builder = builder.cookie(cookie.clone());
        }
        builder
    }

    async fn http_get<T, R>(&self, url: &String, query: &T) -> Result<R>
        where
            R: DeserializeOwned,
            T: Serialize,
    {
        let request = self.create_request(url, Method::GET);
        debug!("Sending GET request {:?}", request);
        let body = request
            .query(query)
            .context("Unable to construct a query")?
            .send()
            .await?
            .body()
            .await?;
        let body_string = String::from_utf8(body.to_vec()).unwrap();
        if body_string.is_empty() {
            return Err(VocadbClientError::BadCredentialsError);
        }
        let json = serde_json::from_slice(&body)
            .context(format!("Unable to deserialize a payload: {}", body_string))?;
        Ok(json)
    }

    async fn http_get_for_query_console<R>(
        &self,
        query: String,
        mode: String,
        db_address: String,
    ) -> Result<R>
        where
            R: DeserializeOwned,
    {
        let request = self.create_request(
            &format!("{}/api/{}?fields=Tags&{}", db_address, mode, query),
            Method::GET,
        );
        debug!("Sending GET request {:?}", request);
        let body = request.send().await?.body().await?;
        let body_string = String::from_utf8(body.to_vec()).unwrap();
        if body_string.is_empty() {
            return Err(VocadbClientError::BadCredentialsError);
        }
        debug!("{:?}", body_string);
        let json =
            serde_json::from_slice(&body).context("Unable to deserialize response".to_string())?;
        Ok(json)
    }

    async fn http_get_no_return_value<T>(&self, url: &String, query: &T) -> Result<()>
        where
            T: Serialize,
    {
        let request = self.create_request(url, Method::GET);
        debug!("Sending GET request {:?}", request);
        request
            .query(query)
            .context("Unable to construct a query")?
            .send()
            .await?;
        Ok(())
    }

    async fn http_post<T, R>(&self, url: &String, query: &T) -> Result<R>
        where
            R: DeserializeOwned,
            T: Serialize,
    {
        let request = self.create_request(url, Method::POST);
        debug!("Sending POST request {:?}", request);
        let body = request
            .query(query)
            .context("Unable to construct a query")?
            .send()
            .await?
            .body()
            .await?;

        let body_string = String::from_utf8(body.to_vec()).unwrap();
        let json = serde_json::from_slice(&body)
            .context(format!("Unable to deserialize a payload: {}", body_string))?;
        Ok(json)
    }

    async fn http_get_raw(&self, url: &String, query: &Vec<(&str, String)>) -> Result<Bytes> {
        let request = self.create_request(url, Method::GET);
        let body = request
            .query(query)
            .context("Unable to construct a query")?
            .send()
            .await?
            .body()
            .await?;
        if body.is_empty() {
            return Err(VocadbClientError::BadCredentialsError);
        }
        Ok(body)
    }

    async fn http_put<U, T>(&self, url: &String, query: &Vec<(&str, String)>, json: U) -> Result<T>
        where
            U: Serialize,
            T: DeserializeOwned,
    {
        let request = self.create_request(url, Method::PUT);
        let body = request
            .query(query)
            .context("Unable to construct a query")?
            .send_json(&json)
            .await?
            .body()
            .await?;
        let json = serde_json::from_slice(&body).context("Unable to deserialize a payload")?;
        Ok(json)
    }

    pub async fn login(&mut self, username: &str, password: &str) -> Result<()> {
        info!("Logging user {}", username);

        let login_payload: HashMap<String, Value> = HashMap::from([
            (String::from("keepLoggedIn"), Value::from(true)),
            (String::from("userName"), Value::from(username)),
            (String::from("password"), Value::from(password)),
        ]);

        let response = self
            .build_request_with_token(format!("{}/api/users/login", self.base_url), Method::POST)
            .await?
            .send_json(&login_payload)
            .await?;

        let cookies = response.cookies().context("Unable to parse a cookie")?;
        let auth_cookie = cookies.iter().find(|c| c.name() == ".AspNetCore.Cookies");
        return match auth_cookie {
            None => Err(VocadbClientError::BadCredentialsError),
            Some(_) => {
                self.clear_cookie();
                self.add_cookies(cookies.deref());
                Ok(())
            }
        };
    }

    pub async fn current_user(&self) -> Result<UserForApiContract> {
        self.http_get(
            &format!("{}/api/users/current", self.base_url),
            &vec![("fields", OptionalFields::MainPicture.as_ref().to_string())],
        )
            .await
    }

    pub async fn get_videos(
        &self,
        tag: String,
        scope_tag: String,
        start_offset: i32,
        max_results: i32,
        order_by: String,
        time_bounds: Vec<String>,
    ) -> Result<NicoResponseWithScope> {
        let mut new_scope = scope_tag.clone();
        loop {
            match new_scope.trim_start().strip_prefix("OR") {
                Some(trimmed) => new_scope = String::from(trimmed),
                None => break,
            }
        }

        new_scope = String::from(new_scope.trim_start());

        let q = if !scope_tag.is_empty() {
            format!("{} {}", tag, new_scope)
        } else {
            tag
        };
        let query = if time_bounds.is_empty() {
            vec![
                ("q", q),
                ("_offset", start_offset.to_string()),
                ("_limit", max_results.to_string()),
                ("_sort", order_by),
                ("targets", String::from("tagsExact")),
                (
                    "fields",
                    String::from("contentId,title,tags,userId,startTime"),
                ),
            ]
        } else {
            vec![
                ("q", q),
                ("_offset", start_offset.to_string()),
                ("_limit", max_results.to_string()),
                ("_sort", order_by),
                ("targets", String::from("tagsExact")),
                (
                    "fields",
                    String::from("contentId,title,tags,userId,startTime"),
                ),
                ("filters[startTime][gte]", String::from(&time_bounds[0])),
                ("filters[startTime][lte]", String::from(&time_bounds[1])),
            ]
        };
        let response: NicoResponse = self
            .http_get(
                &String::from(
                    "https://api.search.nicovideo.jp/api/v2/snapshot/video/contents/search",
                ),
                &query,
            )
            .await?;

        let mut escaped_data: Vec<NicoVideo> = vec![];

        for video in response.data {
            let start_time = (DateTime::parse_from_rfc3339(&video.start_time)
                .unwrap()
                .with_timezone(&FixedOffset::east(0))
                .duration_trunc(chrono::Duration::days(1))
                .unwrap())
                .to_rfc3339();
            escaped_data.push(NicoVideo {
                id: video.id.clone(),
                title: html_escape::decode_html_entities(&video.title).to_string(),
                tags: video.tags.clone(),
                user_id: video.user_id,
                start_time,
            });
        }

        Ok(NicoResponseWithScope {
            safe_scope: new_scope,
            data: escaped_data,
            meta: response.meta,
        })
    }

    pub async fn get_mappings_raw(&self) -> Result<Vec<TagMappingContract>> {
        let tags: PartialFindResult<TagMappingContract> = self
            .http_get(
                &format!("{}/api/tags/mappings", self.base_url),
                &vec![
                    ("start", String::from("0")),
                    ("maxEntries", String::from("10000")),
                    ("getTotalCount", String::from("false")),
                ],
            )
            .await?;

        let mut tags_final = vec![];
        for tag in tags.items {
            tags_final.push(TagMappingContract {
                source_tag: tag.source_tag,
                tag: tag.tag,
            })
        }

        Ok(tags_final)
    }

    pub async fn get_mappings_raw_normalized(&self) -> Result<Vec<TagMappingContract>> {
        let mappings: Vec<TagMappingContract> = self.get_mappings_raw().await?;
        let normalized = mappings
            .into_iter()
            .map(|m| TagMappingContract {
                source_tag: normalize(&m.source_tag),
                tag: m.tag,
            })
            .collect();

        Ok(normalized)
    }

    pub async fn get_vocadb_mapping(
        &self,
        tag: String,
    ) -> Result<Option<Vec<TagBaseContractSimplified>>> {
        let normalized_tag = normalize(&tag);
        let mappings = self.get_mappings_raw_normalized().await?;

        let tags: Vec<TagBaseContractSimplified> = mappings
            .into_iter()
            .filter(|t| t.source_tag == normalized_tag)
            .map(|t| t.tag)
            .collect();
        if tags.is_empty() {
            Ok(None)
        } else {
            Ok(Some(tags))
        }
    }

    pub async fn get_nico_mappings(&self, tag_id: i32) -> Result<Option<Vec<String>>> {
        let mappings = self.get_mappings_raw().await?;

        let tags: Vec<String> = mappings
            .into_iter()
            .filter(|t| t.tag.id == tag_id)
            .map(|t| t.source_tag)
            .collect();
        if tags.is_empty() {
            Ok(None)
        } else {
            Ok(Some(tags))
        }
    }

    pub async fn get_mapped_tags(&self) -> Result<Vec<String>> {
        let mappings = self.get_mappings_raw_normalized().await?;

        Ok(mappings.into_iter().map(|t| t.source_tag).collect())
    }

    pub async fn fetch_songs_by_custom_query(
        &self,
        query: String,
        db_address: String,
    ) -> Result<SongEntriesForTagRemoval> {
        let result: PartialFindResult<SongForApiContractSimplifiedWithTagUsageCounts> = self
            .http_get_for_query_console(query, String::from("songs"), db_address)
            .await?;
        let tag_pool: Vec<TagBaseContractSimplified> = result
            .items
            .iter()
            .flat_map(|song| &song.tags)
            .unique_by(|&tag| tag.tag.id)
            .map(|tag| TagBaseContractSimplified {
                id: tag.tag.id,
                name: tag.tag.name.clone(),
                url_slug: tag.tag.url_slug.clone(),
            })
            .collect();
        Ok(SongEntriesForTagRemoval {
            items: result
                .items
                .into_iter()
                .map(|song| SongEntryForTagRemoval {
                    item: SongForApiContractSimplified {
                        id: song.id,
                        name: song.name.clone(),
                        song_type: song.song_type.clone(),
                        artist_string: song.artist_string.clone(),
                        release_events: Vec::new(),
                        publish_date: None,
                        tags: song.tags.iter().map(|t| t.tag.clone()).collect(),
                    },
                    to_remove: false,
                })
                .collect(),
            total_count: result.total_count,
            tag_pool,
        })
    }

    pub async fn fetch_artists_by_custom_query(
        &self,
        query: String,
        db_address: String,
    ) -> Result<ArtistEntriesForTagRemoval> {
        let result: PartialFindResult<ArtistForApiContractSimplifiedWithTagUsageCounts> = self
            .http_get_for_query_console(query, String::from("artists"), db_address)
            .await?;
        let tag_pool: Vec<TagBaseContractSimplified> = result
            .items
            .iter()
            .flat_map(|song| &song.tags)
            .unique_by(|&tag| tag.tag.id)
            .map(|tag| TagBaseContractSimplified {
                id: tag.tag.id,
                name: tag.tag.name.clone(),
                url_slug: tag.tag.url_slug.clone(),
            })
            .collect();
        Ok(ArtistEntriesForTagRemoval {
            items: result
                .items
                .into_iter()
                .map(|artist| ArtistEntryForTagRemoval {
                    item: ArtistForApiContractSimplified {
                        id: artist.id,
                        artist_type: artist.artist_type,
                        name: artist.name.clone(),
                        tags: artist.tags.iter().map(|t| t.tag.clone()).collect(),
                    },
                    to_remove: false,
                })
                .collect(),
            total_count: result.total_count,
            tag_pool,
        })
    }

    pub async fn get_current_tags(&self, song_id: i32) -> Result<Vec<SelectedTag>> {
        let q: Vec<String> = vec![];
        let result: Vec<SelectedTag> = self
            .http_get(
                &format!("{}/api/users/current/songTags/{}", self.base_url, song_id),
                &q,
            )
            .await?;
        Ok(result)
    }

    pub async fn add_tags(&self, tags: Vec<TagBaseContract>, song_id: i32) -> Result<()> {
        let current_tags = self.get_current_tags(song_id).await?;
        let mut new_tags: Vec<TagBaseContract> = current_tags
            .iter()
            .filter(|tag| tag.selected)
            .map(|tag| tag.tag.clone())
            .collect();
        new_tags.extend(tags);
        self.http_put::<Vec<TagBaseContract>, Vec<TagUsageForApiContract>>(
            &format!("{}/api/users/current/songTags/{}", self.base_url, song_id),
            &vec![],
            new_tags,
        )
            .await?;
        Ok(())
    }

    pub async fn get_song_by_nico_pv(&self, pv_id: &str) -> Result<Option<SongForApiContract>> {
        self.http_get(
            &format!("{}/api/songs/byPv", self.base_url),
            &vec![
                ("pvService", String::from("NicoNicoDouga")),
                ("pvId", String::from(pv_id)),
                ("fields", String::from("Tags,ReleaseEvent,Albums,Artists")),
            ],
        )
            .await
    }

    pub async fn lookup_artist_by_nico_account_id(
        &self,
        user_id: i32,
    ) -> Result<Option<NicoPublisher>> {
        let lookup_result: Vec<NicoArtistDuplicateResult> = self
            .http_post(
                &format!("{}/Artist/FindDuplicate", self.base_url),
                &vec![
                    ("term1", ""),
                    ("term2", ""),
                    ("term3", ""),
                    (
                        "linkUrl",
                        &format!("https://www.nicovideo.jp/user/{}", &user_id.to_string()),
                    ),
                ],
            )
            .await?;

        if lookup_result.is_empty() {
            Ok(None)
        } else {
            Ok(Some(lookup_result[0].entry.clone()))
        }
    }
    pub fn assign_colors(
        &self,
        target_nico_tags: Vec<String>,
        normalized_target_mappings: Vec<String>,
        normalized_scope_tags: Vec<String>,
        non_target_mappings: &Vec<String>,
    ) -> Vec<DisplayableTag> {
        target_nico_tags
            .iter()
            .map(|t| {
                let normalized_t: String = normalize(t);
                let variant = if normalized_target_mappings.contains(&normalized_t) {
                    String::from("primary")
                } else if normalized_scope_tags.iter().any(|s| s == &normalized_t) {
                    String::from("info")
                } else if non_target_mappings.iter().any(|m| m == &normalized_t) {
                    String::from("dark")
                } else {
                    String::from("secondary")
                };

                DisplayableTag {
                    name: t.clone(),
                    variant,
                }
            })
            .collect()
    }

    pub async fn lookup_video(
        &self,
        video: &NicoVideo,
        src_tags: Vec<i32>,
        nico_tags: Vec<String>,
        mappings: &Vec<String>,
        scope: String,
    ) -> Result<VideoWithEntry> {
        let response = self.get_song_by_nico_pv(&video.id).await?;

        let normalized_nico_mappings: Vec<String> =
            nico_tags.into_iter().map(|t| normalize(&t)).collect();
        let normalized_scope = normalize(&scope.replace(" OR ", " "));
        let normalized_scope_tags = normalized_scope
            .split(' ')
            .map(String::from)
            .collect::<Vec<_>>();

        let publisher: Option<NicoPublisher> = match video.user_id {
            Some(id) => self.lookup_artist_by_nico_account_id(id).await?,
            None => None,
        };

        let src_nico_tags = video.tags.split(' ').map(String::from).collect::<Vec<_>>();

        let tags = self.assign_colors(
            src_nico_tags,
            normalized_nico_mappings,
            normalized_scope_tags,
            mappings,
        );

        let mut tag_in_tags = false;
        let entry = response.map(|res| {
            tag_in_tags = src_tags
                .iter()
                .all(|tag_id| res.tags.iter().any(|t| &t.tag.id == tag_id));

            SongForApiContractSimplifiedWithMultipleEventInfo {
                id: res.id,
                name: res.name.clone(),
                song_type: res.song_type,
                artist_string: res.artist_string.clone(),
                release_events: if res.release_events.is_some() { res.release_events.unwrap() } else { Vec::new() },
                publish_date: res.publish_date,
            }
        });

        Ok(VideoWithEntry {
            video: NicoVideoWithTidyTags {
                id: video.id.clone(),
                title: video.title.clone(),
                start_time: video.start_time.clone(),
                tags,
                description: self.get_formatted_description(video.id.clone()).await?,
            },
            song_entry: entry,
            publisher,
            processed: tag_in_tags,
        })
    }

    pub async fn lookup_video_by_event(
        &self,
        video: &NicoVideo,
        event_id: i32,
        nico_tags: Vec<String>,
        mappings: &Vec<String>,
        scope: String,
    ) -> Result<VideoWithEntry> {
        let response = self.get_song_by_nico_pv(&video.id).await?;

        let normalized_nico_mappings: Vec<String> = nico_tags
            .iter()
            .flat_map(|t| t.split(' '))
            .collect::<Vec<&str>>()
            .iter()
            .map(|&t| normalize(t))
            .collect();
        let normalized_scope = normalize(&scope.replace(" OR ", " "));
        let normalized_scope_tags = normalized_scope
            .split(' ')
            .map(String::from)
            .collect::<Vec<_>>();

        let publisher: Option<NicoPublisher> = match video.user_id {
            Some(id) => self.lookup_artist_by_nico_account_id(id).await?,
            None => None,
        };

        let src_nico_tags = video.tags.split(' ').map(String::from).collect::<Vec<_>>();

        let tags = self.assign_colors(
            src_nico_tags,
            normalized_nico_mappings,
            normalized_scope_tags,
            mappings,
        );
        let entry = response.map(|r| {
            let event = r.albums.as_ref().and_then(|albums| {
                albums
                    .iter()
                    .flat_map(|album| &album.release_event)
                    .find(|re| re.id == event_id)
            });

            let re = match event {
                None => if r.release_events.is_some() { r.release_events.unwrap() } else { Vec::new() },
                Some(e) => {
                    let mut re_total = match r.release_events.clone() {
                        Some(re) => re,
                        None => Vec::new()
                    };
                    re_total.push(ReleaseEventForApiContractSimplified {
                        date: e.date.clone(),
                        end_date: e.end_date.clone(),
                        id: e.id,
                        name: e.name.clone(),
                        url_slug: e.url_slug.clone(),
                        category: e.category.clone(),
                        web_links: e.web_links.clone(),
                    });
                    re_total
                }
            };

            SongForApiContractSimplifiedWithMultipleEventInfo {
                id: r.id,
                name: r.name.clone(),
                song_type: r.song_type.clone(),
                artist_string: r.artist_string.clone(),
                publish_date: r.publish_date.clone(),
                release_events: re,
            }
        });

        let processed = entry.as_ref().map_or(false, |e| {
            e.release_events.iter().any(|re| re.id == event_id)
        });

        Ok(VideoWithEntry {
            video: NicoVideoWithTidyTags {
                id: video.id.clone(),
                title: video.title.clone(),
                start_time: video.start_time.clone(),
                tags,
                description: self.get_formatted_description(video.id.clone()).await?,
            },
            song_entry: entry,
            publisher,
            processed,
        })
    }

    pub async fn lookup_tag(&self, tag_id: i32) -> Result<AssignableTag> {
        let response: Option<TagForApiContract> = self
            .http_get(
                &format!("{}/api/tags/{}", self.base_url, tag_id),
                &vec![("fields", String::from("AdditionalNames"))],
            )
            .await?;

        match response {
            Some(res) => Ok(AssignableTag {
                version: res.version,
                usage_count: res.usage_count,
                url_slug: res.url_slug.clone(),
                targets: res.targets,
                status: res.status.to_string(),
                name: res.name.clone(),
                id: res.id,
                additional_names: res.additional_names.clone(),
                category_name: res.category_name.clone(),
                create_date: res.create_date.clone(),
                default_name_language: res.default_name_language.to_string(),
            }),
            _ => Err(VocadbClientError::NotFoundError(format!(
                "tag with id=\"{}\" does not exist",
                tag_id
            ))),
        }
    }

    pub async fn lookup_tag_by_name(&self, tag_name: String) -> Result<AssignableTag> {
        let response: TagSearchResult = self
            .http_get(
                &format!("{}/api/tags/", self.base_url),
                &vec![
                    ("fields", String::from("AdditionalNames")),
                    ("query", tag_name.clone()),
                    ("maxResults", String::from("1")),
                ],
            )
            .await?;

        if !response.items.is_empty() {
            Ok(AssignableTag {
                version: response.items[0].version,
                usage_count: response.items[0].usage_count,
                url_slug: response.items[0].url_slug.clone(),
                targets: response.items[0].targets,
                status: response.items[0].status.to_string(),
                name: response.items[0].name.clone(),
                id: response.items[0].id,
                additional_names: response.items[0].additional_names.clone(),
                category_name: response.items[0].category_name.clone(),
                create_date: response.items[0].create_date.clone(),
                default_name_language: response.items[0].default_name_language.to_string(),
            })
        } else {
            Err(VocadbClientError::NotFoundError(format!(
                "tag \"{}\" does not exist",
                tag_name
            )))
        }
    }

    pub async fn disable_videos(&self, song_id: i32, video_ids: Vec<String>) -> Result<()> {
        let mut song_data = self.get_song_for_edit(song_id).await?;

        let mut pvs_temp = vec![];

        match song_data.get("pvs") {
            None => {
                return Err(VocadbClientError::NotFoundError(format!(
                    "song with id={} does not have any PVs",
                    song_id
                )));
            }
            Some(pvs) => {
                let pvs_parsed = pvs
                    .as_array()
                    .context(format!("failed to parse PVs for song with id={}", song_id))?;

                for pv in pvs_parsed {
                    let parsed_pv = pv.as_object().context(format!(
                        "failed to parse one of the PVs for song with id={}",
                        song_id
                    ))?;

                    let mut pv_temp = parsed_pv.clone();

                    let pv_id = parsed_pv.get("pvId");
                    match pv_id {
                        None => {}
                        Some(pv_id) => {
                            let str = pv_id.as_str().context("")?.to_string();
                            if video_ids.contains(&str) {
                                pv_temp.insert(String::from("disabled"), Value::from(true));
                            };
                        }
                    }
                    pvs_temp.push(pv_temp);
                }
            }
        }

        song_data.insert(String::from("pvs"), Value::from(pvs_temp));

        self.save_song_data(song_data, song_id).await
    }

    pub async fn get_event_series(&self, series_id: i32) -> Result<ReleaseEventSeriesContract> {
        self.http_get(
            &format!("{}/api/releaseEventSeries/{}", self.base_url, series_id),
            &vec![("fields", String::from("WebLinks"))],
        )
            .await
    }

    pub async fn get_event_by_name(
        &self,
        event_name: String,
    ) -> Result<ReleaseEventForApiContractSimplified> {
        let response: EventSearchResult = self
            .http_get(
                &format!("{}/api/releaseEvents", self.base_url),
                &vec![
                    ("query", event_name.clone()),
                    ("getTotalCount", String::from("true")),
                    ("lang", String::from("Default")),
                    ("fields", String::from("WebLinks,Series")),
                    ("nameMatchMode", String::from("Exact")),
                ],
            )
            .await?;

        match response.total_count {
            1 => {
                let mut web_links: Vec<WebLinkForApiContract> = match &response.items[0].series {
                    None => vec![],
                    Some(series) => {
                        let series_with_links = self.get_event_series(series.id).await;
                        match series_with_links {
                            Ok(series_with_links_unwrapped) => {
                                match series_with_links_unwrapped.web_links {
                                    None => vec![],
                                    Some(series_links) => series_links,
                                }
                            }
                            Err(_) => {
                                return Err(VocadbClientError::NotFoundError(format!(
                                    "event series \"{}\" does not exist",
                                    series.name
                                )));
                            }
                        }
                    }
                };
                if response.items[0].web_links.is_some() {
                    web_links.append(&mut response.items[0].web_links.clone().unwrap());
                }
                Ok(ReleaseEventForApiContractSimplified {
                    date: response.items[0].date.clone(),
                    end_date: response.items[0].end_date.clone(),
                    id: response.items[0].id,
                    name: response.items[0].name.clone(),
                    url_slug: response.items[0].url_slug.clone(),
                    category: match &response.items[0].series {
                        Some(series) => series.category.clone(),
                        None => response.items[0].category.clone(),
                    },
                    web_links: if web_links.is_empty() {
                        None
                    } else {
                        Some(web_links)
                    },
                })
            }
            0 => Err(VocadbClientError::NotFoundError(format!(
                "event \"{}\" does not exist",
                event_name.clone()
            ))),
            _ => Err(VocadbClientError::AmbiguousResponseError),
        }
    }

    pub async fn get_event_by_tag(
        &self,
        tag_id: i32,
        tag_name: &str,
    ) -> Result<ReleaseEventForApiContractSimplified> {
        let response: EventSearchResult = self
            .http_get(
                &format!("{}/api/releaseEvents", self.base_url),
                &vec![
                    ("tagId[]", tag_id.to_string()),
                    ("getTotalCount", String::from("true")),
                    ("lang", String::from("Default")),
                    ("fields", String::from("Series")),
                ],
            )
            .await?;

        match response.total_count {
            1 => Ok(ReleaseEventForApiContractSimplified {
                date: response.items[0].date.clone(),
                end_date: response.items[0].end_date.clone(),
                id: response.items[0].id,
                name: response.items[0].name.clone(),
                url_slug: response.items[0].url_slug.clone(),
                category: match &response.items[0].series {
                    Some(series) => series.category.clone(),
                    None => response.items[0].category.clone(),
                },
                web_links: None,
            }),
            0 => Err(VocadbClientError::NotFoundError(format!(
                "no events associated with tag \"{}\"",
                tag_name
            ))),
            _ => Err(VocadbClientError::AmbiguousResponseError),
        }
    }

    fn fill_in_event(
        &self,
        song_data: Map<String, Value>,
        event: MinimalEvent,
    ) -> Result<Map<String, Value>> {
        let mut song_data_edited = song_data;
        let mut new_event = Map::new();
        new_event.insert("id".to_string(), Value::from(event.id));
        let mut release_events: Vec<Value> = song_data_edited.get("releaseEvents")
            .unwrap()
            .as_array()
            .unwrap()
            .clone();
        release_events.push(Value::from(new_event));
        song_data_edited.insert("releaseEvents".to_string(), Value::Array(release_events));
        song_data_edited.insert(
            "updateNotes".to_string(),
            Value::from(format!(
                "Added event \"{}\" (via NicoNicoTagger)",
                event.name
            )),
        );
        Ok(song_data_edited)
    }

    pub async fn edit_entry(&self, song_id: i32, event: MinimalEvent) -> Result<()> {
        let mut song_data: Map<String, Value> = self.get_song_for_edit(song_id).await?;
        song_data = self.fill_in_event(song_data, event.clone())?;
        self.save_song_data(song_data, song_id).await
    }

    async fn build_request_with_token(
        &self,
        url: String,
        method: Method,
    ) -> Result<ClientRequest, VocadbClientError> {
        let atf_cookies = self
            .create_request(
                &format!("{}/api/antiforgery/token", self.base_url),
                Method::GET,
            )
            .send()
            .await?
            .cookies()
            .context("Failed to obtain token")?
            .to_vec();

        let antiforgery_token: String = match atf_cookies
            .deref()
            .iter()
            .find(|cookie| cookie.name() == "XSRF-TOKEN")
        {
            None => return Err(VocadbClientError::BadCredentialsError),
            Some(token) => token.value().to_string(),
        };

        let mut request = self.create_request(&url, method);
        for cookie in atf_cookies {
            request = request.cookie(cookie.clone());
        }

        Ok(request
            .insert_header(("requestVerificationToken", antiforgery_token.as_str()))
            .insert_header(("X-XSRF-TOKEN", antiforgery_token.as_str()))
            .insert_header(("Origin", self.base_url)))
    }

    async fn save_song_data(&self, song_data: Map<String, Value>, song_id: i32) -> Result<()> {
        #[derive(Serialize, Debug)]
        struct FormData {
            contract: String,
        }

        let edited_song = serde_json::to_string(&song_data).context("Unable to serialize")?;

        self.build_request_with_token(
            format!("{}/api/songs/{}", self.base_url, song_id),
            Method::POST,
        )
            .await?
            .content_type("application/x-www-form-urlencoded")
            .send_form(&FormData {
                contract: edited_song,
            })
            .await?
            .body()
            .await?;

        Ok(())
    }

    pub async fn remove_tag(&self, id: i32, entity: String, tag_id: i64) -> Result<()> {
        fn extract_href_link(e: &Element, pattern: &str) -> Option<String> {
            if e.name() != "a" {
                return None;
            }

            for (key, val) in e.attrs() {
                if key != "href" || !val.contains(pattern) {
                    continue;
                }
                return Some(val.to_string());
            }
            None
        }

        fn find_tag_id(tag_id: i64, tr: &ElementRef) -> bool {
            let id_str = format!("/T/{}/", tag_id);
            for td in tr.children() {
                for child in td.children() {
                    match child.value() {
                        Node::Element(e) => {
                            let tag_link = extract_href_link(e, id_str.as_str());
                            if tag_link.is_some() {
                                return true;
                            }
                        }
                        _ => continue,
                    }
                }
            }
            false
        }

        fn process_row(tag_id: i64, tr: &ElementRef, entity: String) -> Option<i64> {
            if !find_tag_id(tag_id, tr) {
                return None;
            }

            let link_pattern = format!("/{}/RemoveTagUsage/", entity);
            for td in tr.children() {
                for child in td.children() {
                    match child.value() {
                        Node::Element(e) => match extract_href_link(e, &link_pattern) {
                            Some(link) => {
                                let id_str = link.trim_start_matches(&link_pattern);
                                let id = id_str.parse().ok()?;
                                return Some(id);
                            }
                            None => continue,
                        },
                        _ => continue,
                    }
                }
            }
            None
        }

        fn extract_tag_usage_id(html: &str, tag_id: i64, entity: String) -> Option<i64> {
            let document = scraper::Html::parse_document(html);
            let selector = scraper::Selector::parse("table>tbody>tr").unwrap();

            let mut tag_usage_id: Option<i64> = None;
            for el in document.select(&selector) {
                tag_usage_id = process_row(tag_id, &el, entity.clone());
                if tag_usage_id.is_some() {
                    break;
                }
            }
            tag_usage_id
        }

        let query = vec![];
        let response_bytes = self
            .http_get_raw(
                &format!(
                    "{}/{}/ManageTagUsages/{}",
                    self.base_url,
                    entity.clone(),
                    id
                ),
                &query,
            )
            .await?;
        let html =
            String::from_utf8(response_bytes.to_vec()).context("Response is not a UTF-8 string")?;

        let tag_usage_id = extract_tag_usage_id(&html, tag_id, entity.clone()).context(format!(
            "Failed to extract tag usage id for tag (id={})",
            tag_id
        ))?;
        self.http_get_no_return_value(
            &format!(
                "{}/{}/RemoveTagUsage/{}",
                self.base_url,
                entity.clone(),
                tag_usage_id
            ),
            &query,
        )
            .await?;

        Ok(())
    }

    pub async fn get_songs_by_vocadb_event_tag(
        &self,
        tag_id: i32,
        start_offset: i32,
        max_results: i32,
        order_by: String,
    ) -> Result<SongForApiContractSimplifiedWithMultipleEventInfoSearchResult> {
        let response: PartialFindResult<SongForApiContract> = self
            .http_get(
                &format!("{}/api/songs", self.base_url),
                &vec![
                    ("tagId[]", tag_id.to_string()),
                    ("start", start_offset.to_string()),
                    ("maxResults", max_results.to_string()),
                    ("sort", order_by),
                    ("getTotalCount", String::from("true")),
                    ("lang", String::from("Default")),
                    ("fields", String::from("ReleaseEvent,Tags,Artists")),
                ],
            )
            .await?;

        if response.total_count > 0 {
            let mut entries = vec![];
            for ref item in response.items {
                entries.push(SongForApiContractSimplifiedWithMultipleEventInfo {
                    id: item.id,
                    name: item.name.clone(),
                    song_type: item.song_type.clone(),
                    artist_string: item.artist_string.clone(),
                    release_events: match item.release_events.clone() {
                        Some(re) => re,
                        None => Vec::new()
                    },
                    publish_date: item.publish_date.clone(),
                });
            }

            Ok(
                SongForApiContractSimplifiedWithMultipleEventInfoSearchResult {
                    items: entries,
                    total_count: response.total_count,
                },
            )
        } else {
            Err(VocadbClientError::NotFoundError(format!(
                "tag with id=\"{}\" is not attached to any songs",
                tag_id
            )))
        }
    }

    pub async fn get_videos_from_db_before_since(
        &self,
        max_results: i32,
        mode: String,
        date_time: String,
        song_id: i32,
        sort_rule: String,
    ) -> Result<SongsForApiContractWithThumbnailsAndTimestamp> {
        let mut response_entries: Vec<SongForApiContract> = vec![];

        let response: PartialFindResult<ActivityEntryForApiContract> = self
            .http_get(
                &format!("{}/api/activityEntries", self.base_url),
                &vec![
                    (mode.as_ref(), date_time),
                    ("maxResults", String::from("200")),
                    ("editEvent", String::from("Created")),
                    ("getTotalCount", String::from("true")),
                    ("fields", String::from("Entry")),
                    ("entryFields", String::from("PVs,Tags")),
                    ("lang", String::from("Default")),
                    ("sortRule", sort_rule),
                ],
            )
            .await?;

        let timestamp_first = response.items[0].entry.create_date.clone();
        let mut timestamp_last = response.items[response.items.len() - 1]
            .entry
            .create_date
            .clone();

        for response_item in response.items {
            if response_item.entry.entry_type == EntryType::Song
                && response_item.edit_event == ActivityEditEvent::Created
                && ((mode == *"since" && response_item.entry.id > song_id)
                || (mode == *"before" && response_item.entry.id < song_id))
            {
                if let Some(ref pvs) = response_item.entry.pvs {
                    let nico_pvs: Vec<&PVContract> = pvs
                        .iter()
                        .filter(|pv| pv.service == PvService::NicoNicoDouga)
                        .collect();
                    if !nico_pvs.is_empty() {
                        if nico_pvs.iter().any(|pv| !pv.disabled) {
                            response_entries.push(SongForApiContract {
                                id: response_item.entry.id,
                                name: response_item.entry.name,
                                tags: match response_item.entry.tags {
                                    Some(tags) => tags,
                                    None => vec![],
                                },
                                song_type: response_item.entry.song_type.unwrap(),
                                artist_string: response_item.entry.artist_string.unwrap(),
                                create_date: response_item.entry.create_date,
                                pvs: response_item.entry.pvs,
                                rating_score: Some(0),
                                release_events: None,
                                publish_date: None,
                                albums: None,
                                artists: vec![],
                            });
                        } else {
                            response_entries.push(SongForApiContract {
                                id: response_item.entry.id,
                                name: response_item.entry.name,
                                tags: vec![],
                                song_type: response_item.entry.song_type.unwrap(),
                                artist_string: response_item.entry.artist_string.unwrap(),
                                create_date: response_item.entry.create_date,
                                pvs: Some(vec![]),
                                rating_score: Some(0),
                                release_events: None,
                                publish_date: None,
                                albums: None,
                                artists: vec![],
                            });
                        };
                    }
                }
            }
            if response_entries.len() == max_results as usize {
                break;
            }
        }

        let response_entries_small: &[SongForApiContract] = response_entries
            [0..min(max_results as usize, response_entries.len() as usize)]
            .as_ref();
        let mut mapped_response = vec![];

        let res_len = response_entries_small.len() as i32;

        if res_len > 0 {
            if res_len == max_results {
                timestamp_last = response_entries_small[res_len as usize - 1]
                    .create_date
                    .clone();
            }
            mapped_response = self.process_mapped_response(response_entries_small).await?;
        }

        Ok(SongsForApiContractWithThumbnailsAndTimestamp {
            items: mapped_response,
            total_count: response.total_count,
            timestamp_first,
            timestamp_last,
        })
    }

    pub async fn process_mapped_response(
        &self,
        songs: &[SongForApiContract],
    ) -> Result<Vec<SongForApiContractWithThumbnails>> {
        pub fn parse_thumbnail(
            xml: &str,
            thumnail_id: &str,
            pv: &PVContract,
            community: bool,
        ) -> Result<ThumbnailOk, ThumbnailError> {
            let doc = Document::parse(xml).unwrap();
            let status = doc
                .descendants()
                .find(|node| node.has_tag_name("nicovideo_thumb_response"))
                .unwrap()
                .attribute("status")
                .unwrap();

            if status.eq("ok") {
                let ok = ThumbnailOk {
                    id: thumnail_id.to_string(),
                    title: get_text(&doc, "title"),
                    description: get_text(&doc, "description"),
                    length: get_text(&doc, "length"),
                    upload_date: get_text(&doc, "first_retrieve"),
                    views: get_i32(&doc, "view_counter"),
                    tags: get_tags(&doc),
                };
                Ok(ok)
            } else {
                let err = ThumbnailError {
                    id: thumnail_id.to_string(),
                    code: get_text(&doc, "code"),
                    description: get_text(&doc, "description"),
                    disabled: pv.disabled,
                    title: String::from(&pv.name),
                    community,
                };
                Err(err)
            }
        }

        fn get_i32(doc: &Document, param: &str) -> i32 {
            let text = get_text(doc, param);
            return i32::from_str(text.as_str()).unwrap();
        }

        fn get_text(doc: &Document, param: &str) -> String {
            doc.descendants()
                .filter(|node| node.has_tag_name(param))
                .map(|node| match node.text() {
                    Some(text) => text.trim(),
                    None => "",
                })
                .collect::<Vec<_>>()
                .first()
                .unwrap()
                .to_string()
        }

        fn get_tags(doc: &Document) -> Vec<Tag> {
            doc.descendants()
                .filter(|node| node.has_tag_name("tag"))
                .map(|node| Tag {
                    name: node.text().unwrap().to_string(),
                    locked: node.has_attribute("lock"),
                })
                .collect::<Vec<_>>()
        }

        let mut mapped_response = vec![];

        for song in songs {
            let mut ok = vec![];
            let mut err = vec![];

            let nico_pvs: Vec<PVContract> = match song.pvs {
                Some(ref new_song_pvs) => {
                    let mut new_pvs: Vec<PVContract> = vec![];
                    for pv in new_song_pvs {
                        if pv.pv_type == PvType::Original
                            && !pv.disabled
                            && pv.service == PvService::NicoNicoDouga
                        {
                            new_pvs.push(pv.clone());

                            let thumnail_id = pv.pv_id.as_ref().unwrap();
                            let thumbnail = self.get_thumbinfo(thumnail_id).await?;
                            let thumbnail_parse_result = parse_thumbnail(
                                String::from_utf8(thumbnail).unwrap().as_str(),
                                thumnail_id,
                                pv,
                                song.tags
                                    .iter()
                                    .map(|tag| tag.tag.id)
                                    .any(|tag_id| tag_id == 7446),
                            );

                            match thumbnail_parse_result {
                                Ok(mut thumb) => {
                                    thumb.description = self.get_formatted_description(thumb.id.clone()).await?;
                                    ok.push(thumb)
                                }
                                Err(thumb) => err.push(thumb),
                            }
                        }
                    }
                    new_pvs
                }
                None => vec![],
            };

            mapped_response.push(SongForApiContractWithThumbnails {
                song: SongForApiContract {
                    id: song.id,
                    name: song.name.clone(),
                    tags: song.tags.clone(),
                    song_type: song.song_type.clone(),
                    artist_string: song.artist_string.clone(),
                    create_date: song.create_date.clone(),
                    rating_score: song.rating_score,
                    pvs: Some(nico_pvs),
                    release_events: song.release_events.clone(),
                    publish_date: song.publish_date.clone(),
                    albums: None,
                    artists: vec![],
                },
                thumbnails_ok: ok,
                thumbnails_error: err,
            });
        }

        Ok(mapped_response)
    }

    async fn get_formatted_description(&self, video_id: String) -> Result<String> {
        let response_bytes = self.http_get_raw(&format!("https://embed.nicovideo.jp/watch/{}", video_id), &vec![]).await?;
        let html = String::from_utf8(response_bytes.to_vec()).context("Response is not a UTF-8 string")?;
        let document = scraper::Html::parse_document(&html);
        let selector = scraper::Selector::parse("html>body>div").unwrap();
        for el in document.select(&selector) {
            if el.value().classes().any(|class| class.to_string() == "f1l9igf4") {
                let data_props = el.value().attr("data-props").unwrap();
                let description_regex =
                    Regex::new(r"description.{3}(.+?).{3}thumbnailUrl").unwrap();
                match description_regex.captures(data_props) {
                    Some(capture) => return Ok(String::from(html_escape::decode_html_entities(&capture[1]).replace("\\\"", "\""))),
                    _ => continue
                }
            }
        }
        Ok(String::from(""))
    }

    pub async fn get_videos_from_db(
        &self,
        start_offset: i32,
        max_results: i32,
        order_by: String,
    ) -> Result<SongsForApiContractWithThumbnailsAndTimestamp> {
        let response: PartialFindResult<SongForApiContract> = self
            .http_get(
                &format!("{}/api/songs", self.base_url),
                &vec![
                    ("onlyWithPvs", String::from("true")),
                    ("start", start_offset.to_string()),
                    ("pvServices", String::from("NicoNicoDouga")),
                    ("maxResults", max_results.to_string()),
                    ("sort", order_by),
                    ("getTotalCount", String::from("true")),
                    ("fields", String::from("PVs,Tags,Artists")),
                    ("lang", String::from("Default")),
                ],
            )
            .await?;

        let mapped_response = self
            .process_mapped_response(response.items.as_slice())
            .await?;

        Ok(SongsForApiContractWithThumbnailsAndTimestamp {
            items: mapped_response,
            total_count: response.total_count,
            timestamp_first: String::from(""),
            timestamp_last: String::from(""),
        })
    }

    pub async fn get_thumbinfo(&self, id: &String) -> Result<Vec<u8>> {
        let vec = self
            .http_get_raw(
                &format!("https://ext.nicovideo.jp/api/getthumbinfo/{}", id),
                &vec![],
            )
            .await?
            .to_vec();
        Ok(vec)
    }

    async fn possibly_first_work(&self, song_id: i32) -> Result<bool> {
        let effective_creator_id = self.get_effective_creator_id(song_id).await?;
        let song_publish_date = self.get_song_release_date(song_id).await?;
        let is_not_first_work = match song_publish_date {
            None => false,
            Some(song_publish_date) => {
                self.is_not_first_work(song_publish_date, effective_creator_id)
                    .await?
            }
        };
        Ok(effective_creator_id != -1 && !is_not_first_work)
    }

    pub async fn process_songs_with_thumbnails(
        &self,
        songs: SongsForApiContractWithThumbnailsAndTimestamp,
    ) -> Result<DBFetchResponseWithTimestamps> {
        let mappings = self.get_mappings_raw_normalized().await?;
        let mapped_tags: Vec<String> = mappings.iter().map(|m| m.source_tag.clone()).collect();
        let mut song_tags_to_map = vec![];

        for song in songs.items {
            let th_ok = self.map_thumbnail_tags(
                &song.thumbnails_ok,
                &mappings,
                &mapped_tags,
                &song.song.tags.iter().map(|t| t.tag.id).collect(),
            );

            let mut th_ok_final: Vec<ThumbnailOkWithMappedTags> = vec![];

            for thumbnail in th_ok {
                let mut mapped_tags_final: Vec<ThumbnailTagMappedWithAssignAndLockInfo> = vec![];
                for tag in thumbnail.mapped_tags {
                    if tag.tag.id == 158 && !tag.assigned {
                        if self.possibly_first_work(song.song.id).await? {
                            mapped_tags_final.push(tag);
                        }
                    } else {
                        mapped_tags_final.push(tag);
                    }
                }
                th_ok_final.push(ThumbnailOkWithMappedTags {
                    thumbnail: thumbnail.thumbnail,
                    mapped_tags: mapped_tags_final,
                    nico_tags: thumbnail.nico_tags,
                })
            }

            song_tags_to_map.push(SongForApiContractWithThumbnailsAndMappedTags {
                song: song.song,
                thumbnails_error: song.thumbnails_error,
                thumbnails_ok: th_ok_final,
            });
        }

        Ok(DBFetchResponseWithTimestamps {
            items: song_tags_to_map,
            total_count: songs.total_count,
            timestamp_first: songs.timestamp_first,
            timestamp_last: songs.timestamp_last,
        })
    }

    async fn is_not_first_work(&self, publish_date: String, artist_id: i32) -> Result<bool> {
        let response: PartialFindResult<SongForApiContract> = self
            .http_get(
                &format!("{}/api/songs", self.base_url),
                &vec![
                    ("artistId[]", artist_id.to_string()),
                    ("beforeDate", publish_date),
                    ("fields", String::from("Artists,Tags")),
                ],
            )
            .await?;

        Ok(!response.items.is_empty())
    }

    async fn get_effective_creator_id(&self, song_id: i32) -> Result<i32> {
        let song_data: SongForApiContract = self
            .http_get(
                &format!("{}/api/songs/{}", self.base_url, song_id),
                &vec![("fields", String::from("Artists,Tags"))],
            )
            .await?;
        let effective_creators: Vec<&ArtistForSongContract> = song_data
            .artists
            .iter()
            .filter(|&artist| {
                !artist.is_support
                    && (artist.categories.contains(&ArtistCategories::Producer)
                    && artist.roles.contains(&ArtistRoles::Default))
                    || (artist.roles.contains(&ArtistRoles::Composer))
            })
            .collect();
        if effective_creators.len() == 1 {
            return match &effective_creators.get(0).unwrap().artist {
                None => Ok(-1),
                Some(artist) => Ok(artist.id),
            };
        }
        Ok(-1)
    }

    async fn get_song_release_date(&self, song_id: i32) -> Result<Option<String>> {
        let song_data: SongForApiContract = self
            .http_get(
                &format!("{}/api/songs/{}", self.base_url, song_id),
                &vec![("fields", String::from("Artists,Tags"))],
            )
            .await?;
        Ok(song_data.publish_date)
    }

    fn map_thumbnail_tags(
        &self,
        thumbnails: &Vec<ThumbnailOk>,
        mappings: &Vec<TagMappingContract>,
        mapped_tags: &Vec<String>,
        song_tag_ids: &Vec<i32>,
    ) -> Vec<ThumbnailOkWithMappedTags> {
        let mut res = vec![];
        for thumbnail in thumbnails {
            let mut tag_mappings = vec![];
            let mut mapped_nico_tags = vec![];
            for tag in &thumbnail.tags {
                if mapped_tags.contains(&normalize(tag.name.as_str())) {
                    let tag_mappings_temp = mappings
                        .iter()
                        .filter(|m| m.source_tag == normalize(tag.name.as_str()))
                        .collect::<Vec<_>>();
                    mapped_nico_tags.push(NicoTagWithVariant {
                        name: tag.name.clone(),
                        variant: String::from("dark"),
                        locked: tag.locked,
                    });
                    for m in tag_mappings_temp {
                        tag_mappings.push(ThumbnailTagMappedWithAssignAndLockInfo {
                            tag: m.tag.clone(),
                            locked: tag.locked,
                            assigned: song_tag_ids.contains(&m.tag.id),
                        });
                    }
                } else {
                    mapped_nico_tags.push(NicoTagWithVariant {
                        name: tag.name.clone(),
                        variant: String::from("secondary"),
                        locked: tag.locked,
                    });
                }
            }
            res.push(ThumbnailOkWithMappedTags {
                thumbnail: thumbnail.clone(),
                mapped_tags: tag_mappings,
                nico_tags: mapped_nico_tags,
            });
        }

        res
    }

    pub async fn get_song_for_edit(&self, song_id: i32) -> Result<Map<String, Value>> {
        let q: Vec<String> = vec![];
        let response: Value = self
            .http_get(
                &format!("{}/api/songs/{}/for-edit", self.base_url, song_id),
                &q,
            )
            .await?;
        let map = response.as_object().context("Response is not a map")?;

        Ok(map.clone())
    }
}
