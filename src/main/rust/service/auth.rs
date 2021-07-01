use std::ops::Add;


use actix_web::cookie::Cookie;
use actix_web::dev::ServiceRequest;
use actix_web_httpauth::extractors::bearer::BearerAuth;
use jsonwebtoken::{DecodingKey, EncodingKey, Header, TokenData, Validation};
use serde::{Deserialize, Serialize};

use crate::client::client::Client;

use crate::client::models::user::GroupId;
use crate::errors::AppError;

const JWT_SECRET: &[u8] = b"secret";

#[derive(Deserialize, Serialize, Clone, Debug)]
pub enum Database {
    VocaDb,
    TouhouDb,
    UtaiteDb,
}

impl Database {
    pub fn url(&self) -> &'static str {
        return match self {
            Database::VocaDb => "https://vocadb.net",
            Database::TouhouDb => "https://touhoudb.com",
            Database::UtaiteDb => "https://utaitedb.net",
        };
    }

    pub fn domain(&self) -> &'static str {
        return match self {
            Database::VocaDb => "vocadb.net",
            Database::TouhouDb => "touhoudb.com",
            Database::UtaiteDb => "utaitedb.net",
        };
    }
}

#[derive(Deserialize, Serialize, Debug)]
pub struct Claims {
    pub user_id: i32,
    pub database: Database,
    pub cookies: Vec<String>,
    pub exp: i64,
}

pub async fn validate_token(
    req: ServiceRequest,
    credentials: BearerAuth,
) -> Result<ServiceRequest, actix_web::Error> {
    decode_token(credentials.token())?;
    Ok(req)
}

pub async fn create_token(
    username: &String,
    password: &String,
    database: &Database,
) -> Result<String, AppError> {
    let client = Client::from(database.url());
    match client.login(username, password).await {
        Ok(_) => {}
        Err(_) => return Err(AppError::BadCredentialsError),
    }

    let user = client.current_user().await;
    return match user {
        Ok(user) => {
            if !(matches!(user.group_id, GroupId::Trusted) || matches!(user.group_id, GroupId::Moderator) ||
                matches!(user.group_id, GroupId::Admin)) {
                return Err(AppError::NoPermissionError);
            }
            let cookies = client.cookies.borrow();

            match cookies.as_ref() {
                Some(cookies) => {
                    let token = create_jwt(user.id, database, cookies)?;
                    return Ok(token);
                }
                None => Err(AppError::BadCredentialsError),
            }
        }
        Err(_) => { Err(AppError::BadCredentialsError) }
    };

}

fn create_jwt(
    user_id: i32,
    database: &Database,
    cookies: &Vec<Cookie>,
) -> Result<String, AppError> {
    let exp_date = chrono::Utc::now().add(chrono::Duration::weeks(1));

    let claims = Claims {
        user_id,
        database: database.clone(),
        cookies: cookies.iter().map(|c| c.to_string()).collect(),
        exp: exp_date.timestamp(),
    };

    let header = Header::default();
    let encoding_key = EncodingKey::from_secret(JWT_SECRET);

    let token = jsonwebtoken::encode(&header, &claims, &encoding_key)?;
    return Ok(token);
}

pub fn decode_token(token: &str) -> Result<TokenData<Claims>, AppError> {
    let decoding_key = DecodingKey::from_secret(JWT_SECRET);
    let validation = Validation::default();

    let claims = jsonwebtoken::decode(token, &decoding_key, &validation)?;
    return Ok(claims);
}
