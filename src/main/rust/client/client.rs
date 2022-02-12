extern crate kana;

use std::str::FromStr;
use std::time::Duration;

use actix_web::cookie::Cookie;
use actix_web::http::Method;
use anyhow::Context;
use log::{debug, info};
use roxmltree::Document;
use serde::de::DeserializeOwned;
use serde::Serialize;

use crate::client::errors::Result;
use crate::client::errors::VocadbClientError;
use crate::client::jputils::normalize;
use crate::client::models::misc::PartialFindResult;
use crate::client::models::pv::{PvService, PvType};
use crate::client::models::query::OptionalFields;
use crate::client::models::song::SongForApiContract;
use crate::client::models::tag::{AssignableTag, SelectedTag, TagBaseContract, TagForApiContract, TagUsageForApiContract};
use crate::client::models::user::UserForApiContract;
use crate::client::nicomodels::{SongForApiContractWithThumbnails, SongsForApiContractWithThumbnails, Tag, TagBaseContractSimplified, ThumbnailError, ThumbnailOk};
use crate::web::dto::{DisplayableTag, NicoResponse, NicoResponseWithScope, NicoVideo, NicoVideoWithTidyTags, SongForApiContractSimplified, TagMappingContract, VideoWithEntry};

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
        debug!("Response: {}", String::from_utf8(body.to_vec()).unwrap());
        let json = serde_json::from_slice(&body).context("Unable to deserialize a payload")?;
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
            None => Err(VocadbClientError::BadCredentialsError),
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
        let mut safe_scope = scope_tag.clone();
        loop {
            match safe_scope.trim_start().strip_prefix("OR") {
                Some(trimmed) => safe_scope = String::from(trimmed),
                None => break
            }
        }

        safe_scope = String::from(safe_scope.trim_start());

        let response: NicoResponse = self.http_get(
            &String::from("https://api.search.nicovideo.jp/api/v2/snapshot/video/contents/search"),
            &vec![
                ("q", if scope_tag != "" { format!("{} {}", tag, safe_scope) } else { tag }),
                ("_offset", start_offset.to_string()),
                ("_limit", max_results.to_string()),
                ("_sort", order_by),
                ("targets", String::from("tagsExact")),
                ("fields", String::from("contentId,title,tags")),
            ],
        )
            .await?;

        Ok(NicoResponseWithScope {
            safe_scope,
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

    pub async fn get_mapping(&self, tag: String) -> Result<Option<Vec<TagBaseContractSimplified>>> {
        let mappings = self.get_mappings_raw().await?;
        let normalized_tag = normalize(&tag);

        let tags: Vec<TagBaseContractSimplified> = mappings.into_iter()
            .filter(|t| t.source_tag == normalized_tag)
            .map(|t| t.tag).collect();
        return if tags.is_empty() { Ok(None) } else { Ok(Some(tags)) };
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

    pub async fn lookup_video(&self, video: &NicoVideo, src_tags: Vec<String>, nico_tag: String, mappings: &Vec<String>, scope: String) -> Result<VideoWithEntry> {
        let response = self.get_song_by_nico_pv(&video.id).await?;

        let normalized_nico_tag = normalize(&nico_tag);
        let normalized_scope = normalize(&scope.replace(" OR ", " "));
        let normalized_scope_tags = normalized_scope.split(" ").map(|s| String::from(s)).collect::<Vec<_>>();

        let nico_tags = video.tags.split(" ").map(|s| String::from(normalize(s))).collect::<Vec<_>>();

        let tags = nico_tags.iter().map(|t| {
            let variant = if t == &normalized_nico_tag {
                String::from("primary")
            } else if normalized_scope_tags.iter().any(|s| s == t) {
                String::from("info")
            } else if mappings.iter().any(|m| m == t) {
                String::from("dark")
            } else {
                String::from("secondary")
            };

            DisplayableTag {
                name: t.clone(),
                variant: variant,
            }
        }).collect();

        let entry = response.map(|res| {
            let tag_in_tags = src_tags.iter().all(|tag| res.tags.iter().any(|t| &t.tag.name == tag));

            SongForApiContractSimplified {
                id: res.id,
                name: res.name.clone(),
                tag_in_tags: tag_in_tags,
                song_type: res.song_type.to_string(),
                artist_string: res.artist_string.clone(),
            }
        });

        return Ok(VideoWithEntry {
            video: NicoVideoWithTidyTags {
                id: video.id.clone(),
                title: video.title.clone(),
                tags: tags,
            },
            song_entry: entry,
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

    pub async fn get_videos_from_db(
        &self,
        start_offset: i32,
        max_results: i32,
        order_by: String,
    ) -> Result<SongsForApiContractWithThumbnails> {
        pub fn parse_thumbnail(xml: &str, thumnail_id: &str) -> Result<ThumbnailOk, ThumbnailError> {
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
                .map(|node| node.text().unwrap().trim()).collect::<Vec<_>>().first()
                .unwrap()
                .to_string()
        }

        fn get_tags(doc: &Document) -> Vec<Tag> {
            doc.descendants()
                .filter(|node| node.has_tag_name("tag"))
                .map(|node| Tag { name: node.text().unwrap().to_string(), locked: node.has_attribute("lock") })
                .collect::<Vec<_>>()
        }

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
            ],
        ).await?;

        let mut mapped_response = vec![];

        for song in response.items {
            let mut ok = vec![];
            let mut err = vec![];

            let mut new_song = song.clone();
            new_song.pvs.clear();
            for pv in &song.pvs {
                if pv.pv_type == PvType::Original && !pv.disabled && pv.service == PvService::NicoNicoDouga {
                    new_song.pvs.push(pv.clone());

                    let thumnail_id = pv.pv_id.as_ref().unwrap();
                    let thumbnail = self.get_thumbinfo(thumnail_id).await?;
                    let thumbnail_parse_result = parse_thumbnail(String::from_utf8(thumbnail).unwrap().as_str(), thumnail_id);

                    match thumbnail_parse_result {
                        Ok(thumb) => ok.push(thumb),
                        Err(thumb) => err.push(thumb)
                    }
                }
            }

            mapped_response.push(SongForApiContractWithThumbnails {
                song: new_song,
                thumbnails_ok: ok,
                thumbnails_error: err,
            });
        }

        Ok(SongsForApiContractWithThumbnails{items: mapped_response, total_count: response.total_count})
    }

    pub async fn get_thumbinfo(
        &self,
        id: &String,
    ) -> Result<Vec<u8>> {
        return self.http_get_raw(
            &format!("https://ext.nicovideo.jp/api/getthumbinfo/{}", id), &vec![])
            .await;
    }
}

