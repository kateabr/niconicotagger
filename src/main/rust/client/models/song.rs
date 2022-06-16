use serde::{Deserialize, Serialize};

use strum_macros::ToString;

use crate::client::models::entrythumb::{EntryThumbContract, EntryThumbForApiContract};
use crate::client::models::pv::PVContract;
use crate::client::models::releaseevent::{ReleaseEventForApiContract, ReleaseEventForApiContractSimplified};

use crate::client::models::status::Status;
use crate::client::models::tag::TagUsageForApiContract;
use crate::client::models::user::{UserForApiContract, UserWithEmailContract};


#[derive(Serialize, Deserialize, Debug)]
pub enum FeaturedCategory {
    Nothing,
    Concerts,
    VocaloidRanking,
    Pools,
    Other,
}

#[derive(Serialize, Deserialize, ToString, Debug)]
pub enum PvServices {
    Nothing,
    NicoNicoDouga,
    Youtube,
    SoundCloud,
    Vimeo,
    Piapro,
    Bilibili,
    File,
    LocalFile,
    Creofuga,
    Bandcamp,
}

#[derive(Serialize, Deserialize, Debug)]
pub struct RatedSongForUserForApiContract {
    date: String,
    song: SongForApiContract,
    user: UserForApiContract,
    rating: SongRating,
}

#[derive(Serialize, Deserialize, Debug)]
pub struct RelatedSongsContract {
    #[serde(rename = "artistMatches")]
    artist_matches: Vec<SongForApiContract>,
    #[serde(rename = "likeMatches")]
    like_matches: Vec<SongForApiContract>,
    #[serde(rename = "tagMatches")]
    tag_matches: Vec<SongForApiContract>,
}

#[derive(Serialize, Deserialize, Debug)]
pub struct SongContract {
    #[serde(rename = "additionalNames")]
    additional_names: String,
    #[serde(rename = "artistString")]
    artist_string: String,
    #[serde(rename = "createDate")]
    create_date: String,
    // deleted: bool,
    #[serde(rename = "favoritedTimes")]
    favorited_times: i32,
    id: i32,
    #[serde(rename = "lengthSeconds")]
    length_seconds: i32,
    name: String,
    #[serde(rename = "nicoId")]
    nico_id: String,
    #[serde(rename = "publishDate")]
    publish_date: String,
    #[serde(rename = "pvServices")]
    pv_services: PvServices,
    #[serde(rename = "ratingScore")]
    rating_score: i32,
    #[serde(rename = "songType")]
    song_type: SongType,
    status: Status,
    #[serde(rename = "thumbUrl")]
    thumb_url: String,
    version: i32,
}

#[derive(Serialize, Deserialize, Debug)]
pub enum SongFeaturedCategory {
    Nothing,
    Concerts,
    VocaloidRanking,
    Pools,
    Other,
}

#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct SongForApiContract {
    pub id: i32,
    pub name: String,
    pub tags: Vec<TagUsageForApiContract>,
    #[serde(rename = "songType")]
    pub song_type: SongType,
    #[serde(rename = "artistString")]
    pub artist_string: String,
    // @JsonDeserialize(using = ZonedStringDeserializer.class)
    #[serde(rename = "createDate")]
    pub create_date: String,
    pub pvs: Option<Vec<PVContract>>,
    #[serde(rename = "releaseEvent")]
    pub release_event: Option<ReleaseEventForApiContractSimplified>,
    #[serde(rename = "ratingScore")]
    pub rating_score: Option<i32>,
}

#[derive(Serialize, Deserialize, Debug)]
pub struct SongInAlbumForApiContract {
    #[serde(rename = "discNumber")]
    disc_number: i32,
    id: i32,
    name: String,
    song: SongForApiContract,
    #[serde(rename = "trackNumber")]
    track_number: i32,
}

#[derive(Serialize, Deserialize, Debug, ToString, Clone)]
pub enum SongType {
    Unspecified,
    Original,
    Remaster,
    Remix,
    Cover,
    Arrangement,
    Instrumental,
    Mashup,
    MusicPV,
    DramaPV,
    Live,
    Illustration,
    Other,
}

#[derive(Serialize, Deserialize, Debug)]
pub enum SongRating {
    Nothing,
    Dislike,
    Like,
    Favorite,
}

#[derive(Serialize, Deserialize, Debug)]
pub struct SongRatingContract {
    rating: SongRating,
}

#[derive(Serialize, Deserialize, Debug)]
pub struct SongListForEditContract {
    #[serde(rename = "songLinks")]
    song_links: Vec<SongInListEditContract>,
    #[serde(rename = "updateNotes")]
    update_notes: String,
    author: UserWithEmailContract,
    #[serde(rename = "canEdit")]
    can_edit: bool,
    // deleted: bool,
    description: String,
    #[serde(rename = "eventDate")]
    event_date: String,
    status: Status,
    thumb: EntryThumbContract,
    version: i32,
    #[serde(rename = "featuredCategory")]
    featured_category: FeaturedCategory,
    id: i32,
    name: String,
}

#[derive(Serialize, Deserialize, Debug)]
pub struct SongListBaseContract {
    id: i32,
    #[serde(rename = "featuredCategory")]
    featured_category: SongFeaturedCategory,
    name: String,
}

#[derive(Serialize, Deserialize, Debug)]
pub struct SongInListEditContract {
    #[serde(rename = "songInListId")]
    song_in_list_id: i32,
    notes: String,
    order: i32,
    song: SongForApiContract,
}

#[derive(Serialize, Deserialize, Debug)]
pub struct SongInListForApiContract {
    notes: String,
    order: i32,
    song: SongForApiContract,
}

#[derive(Serialize, Deserialize, Debug)]
pub struct SongListForApiContract {
    author: UserForApiContract,
    #[serde(rename = "eventDate")]
    event_date: String,
    #[serde(rename = "mainPicture")]
    main_picture: EntryThumbForApiContract,
    #[serde(rename = "featuredCategory")]
    featured_category: FeaturedCategory,
    id: i32,
    name: String,
}

#[derive(Serialize, Deserialize, Debug)]
pub struct SongSearchResult {
    pub items: Vec<SongForApiContract>,
    #[serde(rename = "totalCount")]
    pub total_count: i32,
}

#[derive(Serialize, Deserialize, Debug)]
pub struct SongSearchResult {
    pub items: Vec<SongForApiContract>,
    #[serde(rename = "totalCount")]
    pub total_count: i32,
}
