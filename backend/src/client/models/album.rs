use serde::{Deserialize, Serialize};

use crate::client::models::artist::ArtistForAlbumForApiContract;
use crate::client::models::entrythumb::EntryThumbForApiContract;
use crate::client::models::language::Language;
use crate::client::models::misc::{LocalizedStringContract, OptionalDateTimeContract};
use crate::client::models::pv::PVContract;
use crate::client::models::releaseevent::ReleaseEventForApiContract;
use crate::client::models::song::SongInAlbumForApiContract;
use crate::client::models::status::Status;
use crate::client::models::tag::TagUsageForApiContract;
use crate::client::models::user::UserForApiContract;
use crate::client::models::weblink::WebLinkForApiContract;

#[derive(Serialize, Deserialize, Debug)]
pub struct AlbumContract {
    #[serde(rename = "additionalNames")]
    additional_names: String,
    #[serde(rename = "artistString")]
    artist_string: String,
    #[serde(rename = "coverPictureMime")]
    cover_picture_mime: String,
    #[serde(rename = "createDate")]
    create_date: String,
    // deleted: bool,
    #[serde(rename = "discType")]
    disc_type: DiscType,
    id: i32,
    name: String,
    #[serde(rename = "ratingAverage")]
    rating_average: f64,
    #[serde(rename = "ratingCount")]
    rating_count: i32,
    #[serde(rename = "releaseDate")]
    release_date: OptionalDateTimeContract,
    #[serde(rename = "releaseEvent")]
    release_event: Option<ReleaseEventForApiContract>,
    status: Status,
    version: i32,
}

#[derive(Serialize, Deserialize, Debug)]
pub struct AlbumDiscPropertiesContract {
    #[serde(rename = "discNumber")]
    disc_number: i32,
    id: i32,
    #[serde(rename = "mediaType")]
    media_type: DiscMediaType,
    name: String,
}

#[derive(Serialize, Deserialize, Debug)]
pub struct AlbumForApiContract {
    #[serde(rename = "additionalNames")]
    additional_names: String,
    artists: Option<Vec<ArtistForAlbumForApiContract>>,
    #[serde(rename = "artistString")]
    artist_string: String,
    barcode: Option<String>,
    #[serde(rename = "catalogNumber")]
    catalog_number: Option<String>,
    #[serde(rename = "createDate")]
    create_date: String,
    #[serde(rename = "defaultName")]
    default_name: Option<String>,
    #[serde(rename = "defaultNameLanguage")]
    default_name_language: Option<Language>,
    // deleted: bool,
    description: Option<String>,
    discs: Option<Vec<AlbumDiscPropertiesContract>>,
    #[serde(rename = "discType")]
    disc_type: DiscType,
    id: i32,
    identifiers: Option<Vec<AlbumIdentifierContract>>,
    #[serde(rename = "mainPicture")]
    main_picture: Option<EntryThumbForApiContract>,
    #[serde(rename = "mergedTo")]
    merged_to: Option<i32>,
    name: String,
    names: Option<Vec<LocalizedStringContract>>,
    pvs: Option<Vec<PVContract>>,
    #[serde(rename = "ratingAverage")]
    rating_average: f64,
    #[serde(rename = "ratingCount")]
    rating_count: i32,
    #[serde(rename = "releaseDate")]
    pub release_date: OptionalDateTimeContract,
    #[serde(rename = "releaseEvent")]
    pub release_event: Option<ReleaseEventForApiContract>,
    status: Status,
    tags: Option<Vec<TagUsageForApiContract>>,
    tracks: Option<Vec<SongInAlbumForApiContract>>,
    version: i32,
    #[serde(rename = "webLinks")]
    web_links: Option<Vec<WebLinkForApiContract>>,
}

#[derive(Serialize, Deserialize, Debug)]
pub struct AlbumForUserForApiContract {
    album: AlbumForApiContract,
    #[serde(rename = "mediaType")]
    media_type: AlbumMediaType,
    #[serde(rename = "purchaseStatus")]
    purchase_status: AlbumPurchaseStatus,
    rating: i32,
    user: UserForApiContract,
}

#[derive(Serialize, Deserialize, Debug)]
pub struct AlbumIdentifierContract {
    value: String,
}

#[derive(Serialize, Deserialize, Debug)]
pub enum AlbumMediaType {
    PhysicalDisc,
    DigitalDownload,
    Other,
}

#[derive(Serialize, Deserialize, Debug)]
pub enum AlbumPurchaseStatus {
    Nothing,
    Wishlisted,
    Ordered,
    Owned,
}

#[derive(Serialize, Deserialize, Debug)]
pub struct AlbumReviewContract {
    id: i32,
    #[serde(rename = "albumId")]
    album_id: i32,
    date: String,
    #[serde(rename = "languageCode")]
    language_code: String,
    text: String,
    title: String,
    user: UserForApiContract,
}

#[derive(Serialize, Deserialize, Debug)]
pub enum DiscMediaType {
    Audio,
    Video,
}

#[derive(Serialize, Deserialize, Debug)]
pub enum DiscType {
    Unknown,
    Album,
    Single,
    EP,
    SplitAlbum,
    Compilation,
    Video,
    Artbook,
    Game,
    Fanmade,
    Instrumental,
    Other,
}
