extern crate kana;

use std::cmp::min;
use std::ptr::null;
use std::str::FromStr;
use std::time::Duration;

use actix_web::cookie::Cookie;
use actix_web::http::Method;
use anyhow::Context;
use log::{debug, info};
use roxmltree::Document;
use serde::de::DeserializeOwned;
use serde::de::Unexpected::Str;
use serde::Serialize;

use crate::client::errors::Result;
use crate::client::errors::VocadbClientError;
use crate::client::jputils::normalize;
use crate::client::models::activity::{ActivityEditEvent, ActivityEntryForApiContract};
use crate::client::models::artist::{NicoArtistDuplicateResult, NicoPublisher};
use crate::client::models::entrythumb::EntryType;
use crate::client::models::misc::PartialFindResult;
use crate::client::models::pv::{PVContract, PvService, PvType};
use crate::client::models::query::OptionalFields;
use crate::client::models::song::SongForApiContract;
use crate::client::models::tag::{AssignableTag, SelectedTag, TagBaseContract, TagForApiContract, TagSearchResult, TagUsageForApiContract};
use crate::client::models::user::UserForApiContract;
use crate::client::nicomodels::{NicoTagWithVariant, SongForApiContractWithThumbnails, SongForApiContractWithThumbnailsAndMappedTags, SongsForApiContractWithThumbnailsAndTimestamp, Tag, TagBaseContractSimplified, ThumbnailError, ThumbnailOk, ThumbnailOkWithMappedTags, ThumbnailTagMappedWithAssignAndLockInfo};
use crate::web::dto::{DBFetchResponseWithTimestamps, DisplayableTag, NicoResponse, NicoResponseWithScope, NicoVideo, NicoVideoWithTidyTags, SongForApiContractSimplified, TagMappingContract, VideoWithEntry};

pub struct Client<'a> {
    pub client: awc::Client,
    pub base_url: &'a str,
    pub cookies: Vec<Cookie<'a>>,
}

impl<'a> Client<'a> {
    pub fn vocadb(cookies: &Vec<String>) -> Result<Client<'a>> {
        return Client::new("https://vocadb.net", cookies);
    }

    pub fn touhoudb(cookies: &Vec<String>) -> Result<Client<'a>> {
        return Client::new("https://touhoudb.com", cookies);
    }

    pub fn utaitedb(cookies: &Vec<String>) -> Result<Client<'a>> {
        return Client::new("https://utaitedb.net", cookies);
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

        return Ok(Client {
            client: awc_client,
            base_url,
            cookies: parsed_cookies,
        });
    }

    fn add_cookie(&mut self, cookie: &Cookie<'a>) -> &Client {
        self.cookies.push(cookie.clone());
        return self;
    }

    fn clear_cookie(&mut self) -> &Client {
        self.cookies.clear();
        return self;
    }

    fn create_request(&self, url: &String, method: actix_web::http::Method) -> awc::ClientRequest {
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
        return builder;
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
        let json = serde_json::from_slice(&body).context(format!("Unable to deserialize a payload: {}", body_string))?;
        return Ok(json);
    }

    async fn http_post_nullable<T, R>(&self, url: &String, query: &T) -> Result<Option<R>>
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

        if body.is_empty() {
            return Ok(Option::None);
        }

        let body_string = String::from_utf8(body.to_vec()).unwrap();
        let json = serde_json::from_slice(&body).context(format!("Unable to deserialize a payload: {}", body_string))?;
        return Ok(json);
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
        let json = serde_json::from_slice(&body).context(format!("Unable to deserialize a payload: {}", body_string))?;
        return Ok(json);
    }

    async fn http_get_raw(&self, url: &String, query: &Vec<(&str, String)>) -> Result<Vec<u8>>
    {
        let request = self.create_request(url, Method::GET);
        let body = request.query(query).context("Unable to construct a query")?.send().await?.body().await?;
        return Ok(body.to_vec());
    }

    async fn http_put<U, T>(&self, url: &String, query: &Vec<(&str, String)>, json: U) -> Result<T>
        where
            U: Serialize,
            T: DeserializeOwned,
    {
        let request = self.create_request(url, Method::PUT);
        let body = request.query(query).context("Unable to construct a query")?.send_json(&json).await?.body().await?;
        let json = serde_json::from_slice(&body).context("Unable to deserialize a payload")?;
        return Ok(json);
    }

    pub async fn login(&mut self, username: &str, password: &str) -> Result<()> {
        #[derive(Serialize, Debug)]
        struct LoginFormData<'a> {
            #[serde(rename = "UserName")]
            username: &'a str,
            #[serde(rename = "Password")]
            password: &'a str,
            #[serde(rename = "KeepLoggedIn")]
            keep_logged_in: bool,
        }

        let login_data = LoginFormData {
            username,
            password,
            keep_logged_in: true,
        };

        info!("Logging user {}", username);

        let response = self
            .client
            .post(format!("{}/User/Login", self.base_url))
            .send_form(&login_data)
            .await?;

        let cookies = response.cookies().context("Unable to parse a cookie")?;
        let auth_cookie = cookies.iter().find(|c| c.name() == ".AspNetCore.Cookies");
        return match auth_cookie {
            Option::None => Err(VocadbClientError::BadCredentialsError),
            Some(cookie) => {
                self.clear_cookie();
                self.add_cookie(cookie);
                Ok(())
            }
        };
    }

    pub async fn current_user(&self) -> Result<UserForApiContract> {
        return self
            .http_get(
                &format!("{}/api/users/current", self.base_url),
                &vec![("fields", OptionalFields::MainPicture.as_ref().to_string())],
            )
            .await;
    }

    pub async fn get_videos(
        &self,
        tag: String,
        scope_tag: String,
        start_offset: i32,
        max_results: i32,
        order_by: String,
    ) -> Result<NicoResponseWithScope> {
        let mut new_scope = scope_tag.clone();
        loop {
            match new_scope.trim_start().strip_prefix("OR") {
                Some(trimmed) => new_scope = String::from(trimmed),
                Option::None => break
            }
        }

        new_scope = String::from(new_scope.trim_start());

        let q = if scope_tag != "" { format!("{} {}", tag, new_scope) } else { tag };
        let response: NicoResponse = self.http_get(
            &String::from("https://api.search.nicovideo.jp/api/v2/snapshot/video/contents/search"),
            &vec![
                ("q", q),
                ("_offset", start_offset.to_string()),
                ("_limit", max_results.to_string()),
                ("_sort", order_by),
                ("targets", String::from("tagsExact")),
                ("fields", String::from("contentId,title,tags,userId")),
            ],
        )
            .await?;

        Ok(NicoResponseWithScope {
            safe_scope: new_scope,
            data: response.data,
            meta: response.meta,
        })
    }


    pub async fn get_mappings_raw(&self) -> Result<Vec<TagMappingContract>> {
        let tags: PartialFindResult<TagMappingContract> = self.http_get(
            &String::from("https://vocadb.net/api/tags/mappings"),
            &vec![
                ("start", String::from("0")),
                ("maxEntries", String::from("10000")),
                ("getTotalCount", String::from("false")),
            ],
        ).await?;

        let mut normalized = vec![];
        for tag in tags.items {
            normalized.push(TagMappingContract {
                source_tag: normalize(&tag.source_tag),
                tag: tag.tag,
            })
        }

        return Ok(normalized);
    }

    pub async fn get_vocadb_mapping(&self, tag: String) -> Result<Option<Vec<TagBaseContractSimplified>>> {
        let normalized_tag = normalize(&tag);
        let mappings = self.get_mappings_raw().await?;

        let tags: Vec<TagBaseContractSimplified> = mappings.into_iter()
            .filter(|t| t.source_tag == normalized_tag)
            .map(|t| t.tag).collect();
        return if tags.is_empty() { Ok(Option::None) } else { Ok(Some(tags)) };
    }

    pub async fn get_nico_mappings(&self, tag_id: i32) -> Result<Option<Vec<String>>> {
        let mappings = self.get_mappings_raw().await?;

        let tags: Vec<String> = mappings.into_iter()
            .filter(|t| t.tag.id == tag_id)
            .map(|t| t.source_tag).collect();
        return if tags.is_empty() { Ok(Option::None)} else { Ok(Some(tags)) };
    }

    pub async fn get_mapped_tags(&self) -> Result<Vec<String>> {
        let mappings = self.get_mappings_raw().await?;

        return Ok(mappings.into_iter().map(|t| t.source_tag).collect());
    }

    pub async fn get_current_tags(&self, song_id: i32) -> Result<Vec<SelectedTag>> {
        let q: Vec<String> = vec![];
        let result: Vec<SelectedTag> = self.http_get(&format!("https://vocadb.net/api/users/current/songTags/{}", song_id.clone()), &q).await?;
        return Ok(result);
    }

    pub async fn assign(&self, tags: Vec<TagBaseContract>, song_id: i32) -> Result<bool> {
        let _response = self.http_put::<Vec<TagBaseContract>, Vec<TagUsageForApiContract>>(
            &format!("https://vocadb.net/api/users/current/songTags/{}", song_id), &vec![], tags)
            .await?;
        return Ok(true);
    }

    pub async fn get_song_by_nico_pv(&self, pv_id: &str) -> Result<Option<SongForApiContract>> {
        return self.http_get(
            &String::from("https://vocadb.net/api/songs/byPv"),
            &vec![
                ("pvService", String::from("NicoNicoDouga")),
                ("pvId", String::from(pv_id)),
                ("fields", String::from("Tags")),
            ],
        ).await;
    }

    pub async fn lookup_artist_by_nico_account_id(&self, user_id: i32) -> Result<Option<NicoPublisher>> {
        let lookup_result: Vec<NicoArtistDuplicateResult> = self.http_post(
            &String::from("https://vocadb.net/Artist/FindDuplicate"),
            &vec![
                ("term1", ""),
                ("term2", ""),
                ("term3", ""),
                ("linkUrl", &format!("https://www.nicovideo.jp/user/{}", &user_id.to_string())),
            ],
        ).await?;

        return if lookup_result.is_empty() { Ok(Option::None) } else { Ok(Some(lookup_result[0].entry.clone())) };
    }

    pub async fn lookup_video(&self, video: &NicoVideo, src_tags: Vec<i32>, nico_tags: Vec<String>, mappings: &Vec<String>, scope: String) -> Result<VideoWithEntry> {
        let response = self.get_song_by_nico_pv(&video.id).await?;

        let normalized_nico_tags: Vec<String> = nico_tags.into_iter().map(|t| normalize(&t)).collect();
        let normalized_scope = normalize(&scope.replace(" OR ", " "));
        let normalized_scope_tags = normalized_scope.split(" ").map(|s| String::from(s)).collect::<Vec<_>>();

        let publisher: Option<NicoPublisher> = match video.user_id {
            Some(id) => self.lookup_artist_by_nico_account_id(id.clone()).await?,
            Option::None => Option::None
        };

        let nico_tags = video.tags.split(" ").map(|s| String::from(s)).collect::<Vec<_>>();

        let tags = nico_tags.iter().map(|t| {
            let normalized_t: String = normalize(t);
            let variant = if normalized_nico_tags.contains(&normalized_t) {
                String::from("primary")
            } else if normalized_scope_tags.iter().any(|s| s == &normalized_t) {
                String::from("info")
            } else if mappings.iter().any(|m| m == &normalized_t) {
                String::from("dark")
            } else {
                String::from("secondary")
            };

            DisplayableTag {
                name: t.clone(),
                variant,
            }
        }).collect();

        let entry = response.map(|res| {
            let tag_in_tags = src_tags.iter().all(|tag_id| res.tags.iter().any(|t| &t.tag.id == tag_id));

            SongForApiContractSimplified {
                id: res.id,
                name: res.name.clone(),
                tag_in_tags,
                song_type: res.song_type.to_string(),
                artist_string: res.artist_string.clone(),
            }
        });

        return Ok(VideoWithEntry {
            video: NicoVideoWithTidyTags {
                id: video.id.clone(),
                title: video.title.clone(),
                tags,
            },
            song_entry: entry,
            publisher,
        });
    }

    pub async fn lookup_tag(&self, tag_id: i32) -> Result<AssignableTag> {
        let response: Option<TagForApiContract> = self.http_get(
            &format!("https://vocadb.net/api/tags/{}", tag_id),
            &vec![
                ("fields", String::from("AdditionalNames"))
            ],
        ).await?;

        match response {
            Some(res) => {
                return Ok(AssignableTag {
                    version: res.version.clone(),
                    usage_count: res.usage_count.clone(),
                    url_slug: res.url_slug.clone(),
                    targets: res.targets.clone(),
                    status: res.status.to_string(),
                    name: res.name.clone(),
                    id: res.id.clone(),
                    additional_names: res.additional_names.clone(),
                    category_name: res.category_name.clone(),
                    create_date: res.create_date.clone(),
                    default_name_language: res.default_name_language.to_string(),
                });
            }
            _ => Err(VocadbClientError::NotFoundError)
        }
    }

    pub async fn lookup_tag_by_name(&self, tag_name: String) -> Result<AssignableTag> {
        let response: TagSearchResult = self.http_get(
            &String::from("https://vocadb.net/api/tags/"),
            &vec![
                ("fields", String::from("AdditionalNames")),
                ("query", tag_name),
                ("maxResults", String::from("1"))
            ],
        ).await?;

        if !response.items.is_empty() {
            return Ok(AssignableTag {
                version: response.items[0].version.clone(),
                usage_count: response.items[0].usage_count.clone(),
                url_slug: response.items[0].url_slug.clone(),
                targets: response.items[0].targets.clone(),
                status: response.items[0].status.to_string(),
                name: response.items[0].name.clone(),
                id: response.items[0].id.clone(),
                additional_names: response.items[0].additional_names.clone(),
                category_name: response.items[0].category_name.clone(),
                create_date: response.items[0].create_date.clone(),
                default_name_language: response.items[0].default_name_language.to_string(),
            })
        } else {
            return Err(VocadbClientError::NotFoundError);
        }
    }

    pub async fn get_videos_from_db_before_since(&self, max_results: i32, mode: String, date_time: String, song_id: i32, sort_rule: String) -> Result<SongsForApiContractWithThumbnailsAndTimestamp> {
        let mut response_entries: Vec<SongForApiContract> = vec![];
        info!("fetching...");
        let response: PartialFindResult<ActivityEntryForApiContract> = self.http_get(
            &String::from("https://vocadb.net/api/activityEntries"),
            &vec![
                (mode.as_ref(), date_time),
                ("maxResults", String::from("200")),
                ("editEvent", String::from("Created")),
                ("getTotalCount", String::from("true")),
                ("fields", String::from("Entry")),
                ("entryFields", String::from("PVs,Tags")),
                ("lang", String::from("Default")),
                ("sortRule", String::from(sort_rule)),
            ],
        ).await?;

        info!("fetched.");

        let timestamp_first = response.items[0].entry.create_date.clone();
        let mut timestamp_last = response.items[response.items.len() - 1].entry.create_date.clone();

        for response_item in response.items {
            if response_item.entry.entry_type == EntryType::Song && response_item.edit_event == ActivityEditEvent::Created {
                if (mode == String::from("since") && response_item.entry.id > song_id) || (mode == String::from("before") && response_item.entry.id < song_id) {
                    match response_item.entry.pvs {
                        Some(ref pvs) => {
                            if pvs.iter().any(|pv| pv.service == PvService::NicoNicoDouga && !pv.disabled) {
                                response_entries.push(SongForApiContract {
                                    id: response_item.entry.id,
                                    name: response_item.entry.name,
                                    tags: match response_item.entry.tags {
                                        Some(tags) => tags,
                                        None => vec![]
                                    },
                                    song_type: response_item.entry.song_type.unwrap(),
                                    artist_string: response_item.entry.artist_string.unwrap(),
                                    create_date: response_item.entry.create_date,
                                    pvs: response_item.entry.pvs,
                                    rating_score: Some(0),
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
                                });
                            };
                        }
                        None => {}
                    }
                }
            }
            if response_entries.len() == max_results as usize {
                break;
            }
        }

        let response_entries_small: Vec<SongForApiContract> = response_entries[0..min(max_results as usize, response_entries.len() as usize)].to_vec();
        let mut mapped_response = vec![];

        info!("fetched {:?} songs ({:?}-{:?}), with id {:?} than {:?}; gathering thumbnail data...", response_entries_small.len(), timestamp_first, timestamp_last, if mode == "since" { "greater" } else { "less" }, song_id);

        let res_len = i32::from(response_entries_small.len() as i32);

        if i32::from(res_len) > 0 {
            if i32::from(res_len) == max_results {
                timestamp_last = response_entries_small[res_len as usize - 1].create_date.clone();
            }
            mapped_response = self.process_mapped_response(response_entries_small).await?;
        }


        info!("done.");

        Ok(SongsForApiContractWithThumbnailsAndTimestamp { items: mapped_response, total_count: response.total_count, timestamp_first, timestamp_last })
    }

    pub async fn process_mapped_response(&self, songs: Vec<SongForApiContract>) -> Result<Vec<SongForApiContractWithThumbnails>> {
        pub fn parse_thumbnail(xml: &str, thumnail_id: &str, pv: &PVContract, community: bool) -> Result<ThumbnailOk, ThumbnailError> {
            let doc = roxmltree::Document::parse(xml).unwrap();
            let status = doc.descendants()
                .find(|node| node.has_tag_name("nicovideo_thumb_response"))
                .unwrap()
                .attribute("status")
                .unwrap();

            return if status.eq("ok") {
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
                    community
                };
                Err(err)
            };
        }

        fn get_i32(doc: &Document, param: &str) -> i32 {
            let text = get_text(doc, param);
            return i32::from_str(text.as_str()).unwrap();
        }

        fn get_text(doc: &Document, param: &str) -> String {
            doc.descendants()
                .filter(|node| node.has_tag_name(param))
                .map(|node| {
                    match node.text() {
                        Some(text) => text.trim(),
                        Option::None => ""
                    }
                }).collect::<Vec<_>>().first()
                .unwrap()
                .to_string()
        }

        fn get_tags(doc: &Document) -> Vec<Tag> {
            doc.descendants()
                .filter(|node| node.has_tag_name("tag"))
                .map(|node| Tag { name: node.text().unwrap().to_string(), locked: node.has_attribute("lock") })
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
                        if pv.pv_type == PvType::Original && !pv.disabled && pv.service == PvService::NicoNicoDouga {
                            new_pvs.push(pv.clone());

                            let thumnail_id = pv.pv_id.as_ref().unwrap();
                            let thumbnail = self.get_thumbinfo(thumnail_id).await?;
                            let thumbnail_parse_result = parse_thumbnail(String::from_utf8(thumbnail).unwrap().as_str(), thumnail_id, pv, song.tags.iter().map(|tag| tag.tag.id).any(|tag_id| tag_id == 7446));

                            match thumbnail_parse_result {
                                Ok(thumb) => ok.push(thumb),
                                Err(thumb) => err.push(thumb)
                            }
                        }
                    }
                    new_pvs
                }
                Option::None => vec![]
            };

            mapped_response.push(SongForApiContractWithThumbnails {
                song: SongForApiContract {
                    id: song.id,
                    name: song.name,
                    tags: song.tags,
                    song_type: song.song_type,
                    artist_string: song.artist_string,
                    create_date: song.create_date,
                    rating_score: song.rating_score,
                    pvs: Some(nico_pvs),
                },
                thumbnails_ok: ok,
                thumbnails_error: err,
            });
        }

        return Ok(mapped_response);
    }

    pub async fn get_videos_from_db(
        &self,
        start_offset: i32,
        max_results: i32,
        order_by: String,
    ) -> Result<SongsForApiContractWithThumbnailsAndTimestamp> {
        let response: PartialFindResult<SongForApiContract> = self.http_get(
            &String::from("https://vocadb.net/api/songs"),
            &vec![
                ("onlyWithPvs", String::from("true")),
                ("start", start_offset.to_string()),
                ("pvServices", String::from("NicoNicoDouga")),
                ("maxResults", max_results.to_string()),
                ("sort", order_by),
                ("getTotalCount", String::from("true")),
                ("fields", String::from("PVs,Tags")),
                ("lang", String::from("Default")),
            ],
        ).await?;

        let mapped_response = self.process_mapped_response(response.items).await?;

        Ok(SongsForApiContractWithThumbnailsAndTimestamp { items: mapped_response, total_count: response.total_count, timestamp_first: String::from(""), timestamp_last: String::from("") })
    }

    pub async fn get_thumbinfo(
        &self,
        id: &String,
    ) -> Result<Vec<u8>> {
        return self.http_get_raw(
            &format!("https://ext.nicovideo.jp/api/getthumbinfo/{}", id), &vec![])
            .await;
    }

    pub async fn process_songs_with_thumbnails(&self, songs: SongsForApiContractWithThumbnailsAndTimestamp) -> Result<DBFetchResponseWithTimestamps> {
        let mappings = self.get_mappings_raw().await?;
        let mapped_tags: Vec<String> = mappings.iter().map(|m| m.source_tag.clone()).collect();
        let mut song_tags_to_map = vec![];

        for song in songs.items {
            let th_ok = self.map_thumbnail_tags(&song.thumbnails_ok, &mappings, &mapped_tags, &song.song.tags.iter().map(|t| t.tag.id).collect());
            song_tags_to_map.push(SongForApiContractWithThumbnailsAndMappedTags {
                song: song.song,
                thumbnails_error: song.thumbnails_error,
                thumbnails_ok: th_ok,
            });
        }

        return Ok(DBFetchResponseWithTimestamps { items: song_tags_to_map, total_count: songs.total_count, timestamp_first: songs.timestamp_first, timestamp_last: songs.timestamp_last });
    }

    fn map_thumbnail_tags(&self, thumbnails: &Vec<ThumbnailOk>, mappings: &Vec<TagMappingContract>, mapped_tags: &Vec<String>, song_tag_ids: &Vec<i32>) -> Vec<ThumbnailOkWithMappedTags> {
        let mut res = vec![];
        for thumbnail in thumbnails {
            let mut tag_mappings = vec![];
            let mut mapped_nico_tags = vec![];
            for tag in &thumbnail.tags {
                if mapped_tags.contains(&normalize(tag.name.as_str())) {
                    mapped_nico_tags.push(NicoTagWithVariant { name: tag.name.clone(), variant: String::from("dark"), locked: tag.locked });
                    let tag_mappings_temp = mappings.iter().filter(|m| m.source_tag == normalize(tag.name.as_str())).collect::<Vec<_>>();
                    for m in tag_mappings_temp {
                        tag_mappings.push(ThumbnailTagMappedWithAssignAndLockInfo {
                            tag: m.tag.clone(),
                            locked: tag.locked,
                            assigned: song_tag_ids.contains(&m.tag.id),
                        });
                    }
                } else {
                    mapped_nico_tags.push(NicoTagWithVariant { name: tag.name.clone(), variant: String::from("secondary"), locked: tag.locked });
                }
            }
            res.push(ThumbnailOkWithMappedTags {
                thumbnail: thumbnail.clone(),
                mapped_tags: tag_mappings,
                nico_tags: mapped_nico_tags,
            });
        }

        return res;
    }
}
