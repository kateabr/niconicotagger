use serde::{Deserialize, Serialize};

use crate::client::models::entrythumb::EntryType;
use crate::client::models::language::Language;

#[derive(Serialize, Deserialize, Debug)]
pub struct AdvancedSearchFilter {
    #[serde(rename = "filterType")]
    filter_type: FilterType,
    negate: bool,
    param: String,
}

#[derive(Serialize, Deserialize, Debug)]
pub enum FilterType {
    Nothing,
    ArtistType,
    WebLink,
    HasUserAccount,
    RootVoicebank,
    VoiceProvider,
    HasStoreLink,
    HasTracks,
    NoCoverPicture,
    HasAlbum,
    HasOriginalMedia,
    HasMedia,
    HasMultipleVoicebanks,
    HasPublishDate,
    Lyrics,
    LyricsContent,
}

#[derive(Serialize, Deserialize, Debug)]
pub struct LocalizedStringContract {
    language: Language,
    value: String,
}

#[derive(Serialize, Deserialize, Debug)]
pub struct LyricsForSongContract {
    #[serde(rename = "cultureCode")]
    culture_code: String,
    id: i32,
    source: String,
    #[serde(rename = "translationType")]
    translation_type: TranslationType,
    url: String,
    value: String,
}

#[derive(Serialize, Deserialize, Debug)]
pub struct OldUsernameContract {
    date: String,
    #[serde(rename = "oldName")]
    old_name: String,
}

#[derive(Serialize, Deserialize, Debug)]
pub struct OptionalDateTimeContract {
    day: Option<i32>,
    formatted: Option<String>,
    #[serde(rename = "isEmpty")]
    is_empty: bool,
    month: Option<i32>,
    year: Option<i32>,
}

#[derive(Serialize, Deserialize, Debug)]
pub struct PartialFindResult<T> {
    pub items: Vec<T>,
    #[serde(rename = "totalCount")]
    pub total_count: i32,
    pub term: Option<String>,
}

#[derive(Serialize, Deserialize, Debug)]
pub enum TranslationType {
    Original,
    Romanized,
    Translation,
}

#[derive(Serialize, Deserialize, Debug)]
pub enum PublisherType {
    USER,
    CHANNEL,
}

#[derive(Serialize, Deserialize, Debug)]
pub struct DuplicateEntry {
    #[serde(rename = "artistString")]
    artist_string: Option<String>,
    #[serde(rename = "entryTypeName")]
    entry_type_name: String,
    pub name: ArtistDuplicateNames,
    #[serde(rename = "entryType")]
    pub entry_type: EntryType,
    pub id: i32,
}

#[derive(Serialize, Deserialize, Debug)]
pub struct ArtistDuplicateNames {
    #[serde(rename = "additionalNames")]
    additional_names: Option<String>,
    #[serde(rename = "displayName")]
    pub display_name: String,
}
