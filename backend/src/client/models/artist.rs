use serde::{Deserialize, Deserializer, Serialize};

use crate::client::models::album::AlbumForApiContract;
use crate::client::models::entrythumb::EntryThumbForApiContract;
use crate::client::models::language::Language;
use crate::client::models::misc::LocalizedStringContract;
use crate::client::models::releaseevent::ReleaseEventForApiContract;
use crate::client::models::song::SongForApiContract;
use crate::client::models::status::Status;
use crate::client::models::tag::TagUsageForApiContract;
use crate::client::models::weblink::WebLinkForApiContract;
use std::str::FromStr;
use strum_macros::EnumString;

#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct ArtistContract {
    #[serde(rename = "additionalNames")]
    additional_names: String,
    #[serde(rename = "artistType")]
    artist_type: ArtistType,
    // deleted: bool,
    pub id: i32,
    name: String,
    #[serde(rename = "releaseDate")]
    release_date: Option<String>,
    status: Status,
    version: i32,
}

#[derive(Serialize, Deserialize, Debug)]
pub struct ArtistForAlbumForApiContract {
    artist: ArtistContract,
    categories: ArtistCategories,
    #[serde(rename = "effectiveRoles")]
    effective_roles: ArtistRoles,
    #[serde(rename = "isSupport")]
    is_support: bool,
    name: String,
    roles: ArtistRoles,
}

#[derive(Serialize, Deserialize, Debug)]
pub struct ArtistForApiContract {
    #[serde(rename = "additionalNames")]
    additional_names: String,
    #[serde(rename = "artistLinks")]
    artist_links: Vec<ArtistForArtistForApiContract>,
    #[serde(rename = "artistLinksReverse")]
    artist_links_reverse: Vec<ArtistForArtistForApiContract>,
    #[serde(rename = "artistType")]
    artist_type: ArtistType,
    #[serde(rename = "baseVoicebank")]
    base_voicebank: ArtistContract,
    #[serde(rename = "createDate")]
    create_date: String,
    #[serde(rename = "defaultName")]
    default_name: String,
    #[serde(rename = "defaultNameLanguage")]
    default_name_language: Language,
    // deleted: bool,
    description: String,
    id: i32,
    #[serde(rename = "mainPicture")]
    main_picture: EntryThumbForApiContract,
    #[serde(rename = "mergedTo")]
    merged_to: i32,
    name: String,
    names: Vec<LocalizedStringContract>,
    #[serde(rename = "pictureMime")]
    picture_mime: String,
    relations: ArtistRelationsForApi,
    #[serde(rename = "releaseDate")]
    release_date: String,
    status: Status,
    tags: Vec<TagUsageForApiContract>,
    version: i32,
    #[serde(rename = "webLinks")]
    web_links: Vec<WebLinkForApiContract>,
}

#[derive(Serialize, Deserialize, Debug)]
pub struct ArtistForArtistForApiContract {
    artist: ArtistContract,
    #[serde(rename = "linkType")]
    link_type: ArtistLinkType,
}

#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct ArtistForEventContract {
    artist: ArtistContract,
    #[serde(rename = "effectiveRoles")]
    effective_roles: ArtistEventRoles,
    id: i32,
    name: String,
    roles: ArtistEventRoles,
}

#[derive(Serialize, Deserialize, Debug)]
pub struct ArtistForSongContract {
    pub artist: Option<ArtistContract>,
    #[serde(deserialize_with = "artist_categories_to_enum_vec")]
    pub categories: Vec<ArtistCategories>,
    #[serde(rename = "effectiveRoles", deserialize_with = "roles_to_enum_vec")]
    pub effective_roles: Vec<ArtistRoles>,
    pub id: i32,
    #[serde(rename = "isCustomName")]
    is_custom_name: bool,
    #[serde(rename = "isSupport")]
    pub is_support: bool,
    name: String,
    #[serde(deserialize_with = "roles_to_enum_vec")]
    pub roles: Vec<ArtistRoles>,
}

fn artist_categories_to_enum_vec<'de, D>(deserializer: D) -> Result<Vec<ArtistCategories>, D::Error>
where
    D: Deserializer<'de>,
{
    let string: String = String::deserialize(deserializer)?;
    let v = string
        .split(", ")
        .map(|string| ArtistCategories::from_str(string).unwrap())
        .collect();
    Ok(v)
}

fn roles_to_enum_vec<'de, D>(deserializer: D) -> Result<Vec<ArtistRoles>, D::Error>
where
    D: Deserializer<'de>,
{
    let string: String = String::deserialize(deserializer)?;
    let v = string
        .split(", ")
        .map(|string| ArtistRoles::from_str(string).unwrap())
        .collect();
    Ok(v)
}

#[derive(Serialize, Deserialize, Debug)]
pub struct ArtistForUserForApiContract {
    artist: ArtistForApiContract,
}

#[derive(Serialize, Deserialize, Debug)]
pub struct ArtistRelationsForApi {
    #[serde(rename = "latestAlbums")]
    latest_albums: Vec<AlbumForApiContract>,
    #[serde(rename = "latestEvents")]
    latest_events: Vec<ReleaseEventForApiContract>,
    #[serde(rename = "latestSongs")]
    latest_songs: Vec<SongForApiContract>,
    #[serde(rename = "popularAlbums")]
    popular_albums: Vec<AlbumForApiContract>,
    #[serde(rename = "popularSongs")]
    popular_songs: Vec<SongForApiContract>,
}

#[derive(Serialize, Deserialize, Debug, PartialEq, Eq, EnumString)]
pub enum ArtistRoles {
    Default,
    Animator,
    Arranger,
    Composer,
    Distributor,
    Illustrator,
    Instrumentalist,
    Lyricist,
    Mastering,
    Publisher,
    Vocalist,
    VoiceManipulator,
    Other,
    Mixer,
    Chorus,
    Encoder,
    VocalDataProvider,
}

#[derive(Serialize, Deserialize, Debug, Clone)]
#[allow(clippy::upper_case_acronyms)]
pub enum ArtistType {
    Unknown,
    Circle,
    Label,
    Producer,
    CoverArtist,
    Animator,
    Illustrator,
    Lyricist,
    Vocaloid,
    UTAU,
    CeVIO,
    SynthesizerV,
    OtherVoiceSynthesizer,
    OtherVocalist,
    OtherGroup,
    OtherIndividual,
    Utaite,
    Band,
    Vocalist,
    Character,
    NEUTRINO,
    VoiSona,
    NewType,
    Voiceroid
}

#[derive(Serialize, Deserialize, Debug, PartialEq, Eq, EnumString)]
pub enum ArtistCategories {
    Nothing,
    Vocalist,
    Producer,
    Animator,
    Label,
    Circle,
    Other,
    Band,
    Illustrator,
    Subject,
}

#[derive(Serialize, Deserialize, Debug, Clone)]
pub enum ArtistEventRoles {
    Default,
    Dancer,
    DJ,
    Instrumentalist,
    Organizer,
    Promoter,
    VJ,
    Vocalist,
    VoiceManipulator,
    OtherPerformer,
    Other,
}

#[derive(Serialize, Deserialize, Debug)]
pub enum ArtistLinkType {
    CharacterDesigner,
    Group,
    Illustrator,
    Manager,
    VoiceProvider,
}

#[derive(Serialize, Deserialize, Debug)]
pub struct NicoArtistDuplicateResult {
    #[serde(rename = "matchProperty")]
    match_property: String,
    pub entry: NicoPublisher,
}

#[derive(Serialize, Deserialize, Clone, Debug)]
pub struct NicoPublisher {
    #[serde(rename = "entryTypeName")]
    pub entry_type_name: String,
    pub name: NicoPublisherNames,
    pub id: i32,
    #[serde(rename = "artistString")]
    artist_string: Option<String>,
}

#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct NicoPublisherNames {
    #[serde(rename = "additionalNames")]
    additional_names: String,
    #[serde(rename = "displayName")]
    display_name: String,
}
