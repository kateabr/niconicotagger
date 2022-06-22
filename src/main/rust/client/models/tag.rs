use serde::{Deserialize, Serialize};


use crate::client::models::language::Language;
use crate::client::models::status::Status;


#[derive(Serialize, Deserialize, Debug)]
pub struct EnglishTranslatedStringContract {
    english: String,
    original: String,
}

#[derive(Serialize, Deserialize, Debug)]
pub struct LocalizedStringWithIdContract {
    id: i32,
    language: Language,
    value: String,
}

#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct TagBaseContract {
    pub id: i32,
    pub name: String,
    #[serde(rename = "categoryName")]
    pub category_name: Option<String>,
    #[serde(rename = "additionalNames")]
    pub additional_names: Option<String>,
    #[serde(rename = "urlSlug")]
    pub url_slug: String,
}

#[derive(Serialize, Deserialize, Debug)]
pub struct TagForApiContract {
    #[serde(rename = "additionalNames")]
    pub additional_names: String,
    #[serde(rename = "categoryName")]
    pub category_name: String,
    #[serde(rename = "createDate")]
    pub create_date: String,
    #[serde(rename = "defaultNameLanguage")]
    pub default_name_language: Language,
    pub id: i32,
    pub name: String,
    pub status: Status,
    pub targets: i32,
    #[serde(rename = "urlSlug")]
    pub url_slug: String,
    #[serde(rename = "usageCount")]
    pub usage_count: i32,
    pub version: i32,
}

#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct AssignableTag {
    #[serde(rename = "additionalNames")]
    pub additional_names: String,
    #[serde(rename = "categoryName")]
    pub category_name: String,
    #[serde(rename = "createDate")]
    pub create_date: String,
    #[serde(rename = "defaultNameLanguage")]
    pub default_name_language: String,
    pub id: i32,
    pub name: String,
    pub status: String,
    pub targets: i32,
    #[serde(rename = "urlSlug")]
    pub url_slug: String,
    #[serde(rename = "usageCount")]
    pub usage_count: i32,
    pub version: i32
}

#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct TagUsageForApiContract {
    pub tag: TagBaseContract,
    pub count: i32,
}

#[derive(Serialize, Deserialize, Debug)]
pub struct SelectedTag {
    pub tag: TagBaseContract,
    pub selected: bool,
}

#[derive(Serialize, Deserialize, Debug)]
pub struct TagSearchResult {
    pub items: Vec<TagForApiContract>,
    pub term: String,
    #[serde(rename = "totalCount")]
    pub total_count: i32,
}
