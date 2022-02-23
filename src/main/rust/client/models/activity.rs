use serde::{Deserialize, Serialize};

use crate::client::models::archived::ArchivedObjectVersionForApiContract;
use crate::client::models::entrythumb::EntryForApiContract;
use crate::client::models::user::UserForApiContract;

#[derive(Serialize, Deserialize, Debug, PartialEq)]
pub enum ActivityEditEvent {
    Created,
    Updated,
    Deleted,
    Restored,
}

#[derive(Serialize, Deserialize, Debug)]
pub struct ActivityEntryForApiContract {
    #[serde(rename = "archivedVersion")]
    pub archived_version: Option<ArchivedObjectVersionForApiContract>,
    pub author: UserForApiContract,
    #[serde(rename = "createDate")]
    pub create_date: String,
    #[serde(rename = "editEvent")]
    pub edit_event: ActivityEditEvent,
    pub entry: EntryForApiContract,
}
