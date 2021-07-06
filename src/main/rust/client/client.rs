extern crate kana;

use std::borrow::Borrow;
use std::cell::{Ref, RefCell};
use std::ops::Deref;
use std::time::Duration;

use actix_web::client::ClientRequest;
use actix_web::cookie::Cookie;
use actix_web::http::Method;
use actix_web::HttpMessage;
use kana::*;
use kana::{half2kana, wide2ascii};
use serde::de::DeserializeOwned;
use serde::Serialize;

use crate::client::errors::ClientError;
use crate::client::models::misc::PartialFindResult;
use crate::client::models::query::OptionalFields;
use crate::client::models::song::SongForApiContract;
use crate::client::models::tag::{AssignableTag, SelectedTag, TagBaseContract, TagForApiContract, TagUsageForApiContract};
use crate::client::models::user::UserForApiContract;
use crate::controller::{DisplayableTag, NicoResponse, NicoVideo, NicoVideoWithTidyTags, SongForApiContractSimplified, TagMappingContract, VideoWithEntry, NicoResponseWithScope};

pub type ClientCookies<'a> = Option<Vec<Cookie<'a>>>;
pub type Result<T, E = ClientError> = core::result::Result<T, E>;

pub struct Client<'a> {
    pub base_url: &'a str,
    pub cookies: RefCell<ClientCookies<'a>>,
}

impl From<&'static str> for Client<'_> {
    fn from(value: &'static str) -> Self {
        return Client {
            base_url: value,
            cookies: RefCell::new(None),
        };
    }
}

impl<'a> Client<'a> {
    pub fn new(base_url: &'a str, cookies: Vec<String>) -> Result<Client<'a>> {
        let mut cookies_ = vec![];
        for c in cookies {
            let cookie = Cookie::parse(c)?;
            cookies_.push(cookie)
        }

        return Ok(Client {
            base_url,
            cookies: RefCell::new(Some(cookies_)),
        });
    }

    fn base_url(&self) -> &'a str {
        return self.base_url;
    }

    fn set_cookies(&self, cookies: Ref<Vec<Cookie<'a>>>) -> ClientCookies<'a> {
        return self.cookies.replace(Some(cookies.to_owned()));
    }

    pub fn default_client(&self) -> actix_web::client::Client {
        return actix_web::client::Client::builder()
            .connector(actix_web::client::Connector::new()
                .timeout(Duration::from_secs(30))
                .finish())
            .timeout(Duration::from_secs(30))
            .finish();
    }

    pub async fn login(&self, username: &str, password: &str) -> Result<()> {
        let client = self.default_client();

        #[derive(Serialize)]
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

        let response = client
            .post(format!("{}/User/Login", self.base_url()))
            .send_form(&login_data)
            .await;

        return match response {
            Ok(response) => {
                let cookies = response.cookies();
                match cookies {
                    Ok(cookies) => {
                        let result = cookies.borrow().len() != 0;
                        if result {
                            self.set_cookies(cookies);
                            Ok(())
                        } else {
                            Err(ClientError::BadCredentialsError)
                        }
                    }
                    Err(_) => Err(ClientError::BadCredentialsError)
                }
            }
            Err(_) => Err(ClientError::BadCredentialsError)
        };
    }

    fn create_request(&self, url: &String, method: actix_web::http::Method) -> ClientRequest {
        let client = self.default_client();
        let mut builder = match method {
            Method::GET => client.get(url),
            Method::POST => client.post(url),
            Method::DELETE => client.delete(url),
            Method::PUT => client.put(url),
            _ => panic!("Unsupported method"),
        };

        let cookies_ref = self.cookies.borrow();
        if let Some(cookies) = cookies_ref.deref() {
            for c in cookies {
                builder = builder.cookie(c.clone());
            }
        }
        return builder;
    }

    async fn http_get<T>(&self, url: &String, query: &Vec<(&str, String)>) -> Result<T>
        where
            T: DeserializeOwned,
    {
        let request = self.create_request(url, Method::GET);
        let body = request.query(query)?.send().await?.body().await?;
        let json = serde_json::from_slice(&body)?;
        return Ok(json);
    }

    async fn http_put<U, T>(&self, url: &String, query: &Vec<(&str, String)>, json: U) -> Result<T>
        where
            U: Serialize,
            T: DeserializeOwned,
    {
        let request = self.create_request(url, Method::PUT);
        let body = request.query(query)?.send_json(&json).await?.body().await?;
        let json = serde_json::from_slice(&body)?;
        return Ok(json);
    }

    async fn http_delete<T>(&self, url: &String, query: &Vec<(&str, String)>) -> Result<T>
        where
            T: DeserializeOwned,
    {
        let request = self.create_request(url, Method::DELETE);
        let body = request.query(query)?.send().await?.body().await?;
        let json = serde_json::from_slice(&body)?;
        return Ok(json);
    }

    async fn http_delete_void(&self, url: &String, query: &Vec<(&str, String)>) -> Result<()>
    {
        let request = self.create_request(url, Method::DELETE);
        let body = request.query(query)?.send().await?.status();
        return if body.is_success() {
            Ok(())
        } else {
            Err(ClientError::ResponseError)
        };
    }

    pub async fn current_user(&self) -> Result<UserForApiContract> {
        self.http_get(
            &format!("{}/api/users/current", self.base_url),
            &vec![("fields", OptionalFields::MainPicture.to_string())],
        )
            .await
    }

    pub async fn get_videos(
        &self,
        tag: String,
        scope_tag: String,
        start_offset: i32,
        max_results: i32,
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
                ("_sort", String::from("startTime")),
                ("targets", String::from("tagsExact")),
                ("fields", String::from("contentId,title,tags"))
            ],
        )
            .await?;

        Ok(NicoResponseWithScope{
            safe_scope,
            data: response.data,
            meta: response.meta
        })
    }

    pub async fn get_mapping(&self, tag: String) -> Result<Option<Vec<TagBaseContract>>> {
        let mappings: PartialFindResult<TagMappingContract> = self.http_get(
            &String::from("https://vocadb.net/api/tags/mappings"),
            &vec![
                ("start", String::from("0")),
                ("maxEntries", String::from("10000")),
                ("getTotalCount", String::from("false"))
            ],
        )
            .await?;

        let tags: Vec<TagBaseContract> = mappings.items.iter()
            .filter(|item| item.source_tag.to_lowercase() == tag.to_lowercase())
            .map(|mapped_tags| mapped_tags.tag.clone()).collect();
        return if tags.is_empty() { Ok(None) } else { Ok(Some(tags)) };
    }

    pub async fn get_current_tags(&self, song_id: i32) -> Result<Vec<SelectedTag>> {
        let result = self.http_get(&format!("https://vocadb.net/api/users/current/songTags/{}", song_id.clone()), &vec![]).await?;
        return Ok(result);
    }

    pub async fn get_mapped_tags(&self) -> Result<Vec<String>> {
        let mappings: PartialFindResult<TagMappingContract> = self.http_get(
            &String::from("https://vocadb.net/api/tags/mappings"),
            &vec![
                ("start", String::from("0")),
                ("maxEntries", String::from("10000")),
                ("getTotalCount", String::from("false"))
            ],
        )
            .await?;

        return Ok(mappings.items.iter().map(|m| kata2hira(&half2kana(&wide2ascii(&m.source_tag).clone()))).collect());
    }

    pub async fn assign(&self, tags: Vec<TagBaseContract>, song_id: i32) -> Result<bool> {
        let _response = self.http_put::<Vec<TagBaseContract>, Vec<TagUsageForApiContract>>(
            &format!("https://vocadb.net/api/users/current/songTags/{}", song_id), &vec![], tags)
            .await?;
        return Ok(true);
    }

    pub async fn lookup_video(&self, video: &NicoVideo, src_tags: Vec<String>, nico_tag: String, mappings: &Vec<String>, scope: String) -> Result<VideoWithEntry> {
        let response: Option<SongForApiContract> = self.http_get(
            &String::from("https://vocadb.net/api/songs/byPv"),
            &vec![
                ("pvService", String::from("NicoNicoDouga")),
                ("pvId", video.id.clone()),
                ("fields", String::from("Tags"))
            ],
        )
            .await?;

        let normalized_nico_tag = kata2hira(&half2kana(&wide2ascii(&nico_tag).to_lowercase()));
        let normalized_scope_tags: Vec<String> = kata2hira(&half2kana(&wide2ascii(&scope).to_lowercase()))
            .split(" or ")
            .map(|s| String::from(s)).collect();

        return Ok(VideoWithEntry {
            video: NicoVideoWithTidyTags {
                id: video.id.clone(),
                title: video.title.clone(),
                tags: video.tags.split(" ").map(|t| DisplayableTag {
                    name: String::from(t),
                    variant: if String::from(kata2hira(&half2kana(&wide2ascii(t).to_lowercase()))) == normalized_nico_tag
                    { String::from("primary") } else if normalized_scope_tags.iter()
                        .any(|s| s == &kata2hira(&half2kana(&wide2ascii(t).to_lowercase())))
                    { String::from("info") } else if mappings.iter().any(|m| m.to_lowercase() ==
                        kata2hira(&half2kana(&wide2ascii(t).to_lowercase())))
                    { String::from("dark") } else { String::from("secondary") }
                }).collect()
            },
            song_entry: response.map(|res| SongForApiContractSimplified {
                id: res.id,
                name: res.name.clone(),
                tag_in_tags: src_tags.iter()
                    .all(|tag| res.tags.iter().any(|t| t.tag.name == tag.clone())),
                song_type: res.song_type.to_string(),
                artist_string: res.artist_string.clone()
            }),
        });
    }

    pub async fn lookup_tag(&self, tag_id: i32) -> Result<AssignableTag> {
        let response: Option<TagForApiContract> = self.http_get(
            &format!("https://vocadb.net/api/tags/{}", tag_id),
            &vec![
                ("fields", String::from("AdditionalNames"))
            ],
        )
            .await?;

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
            _ => Err(ClientError::ResponseError)
        }
    }
}
