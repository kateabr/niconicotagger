use actix_web::http::StatusCode;

use crate::client::errors::VocadbClientError;

pub type Result<T, E = AppResponseError> = core::result::Result<T, E>;

#[derive(thiserror::Error, Debug)]
pub enum AppResponseError {
    #[error("Provided credentials are not valid")]
    BadCredentialsError,
    #[error("Resource not found")]
    NotFoundError,
    #[error("{0}")]
    ConstraintViolationError(String),
    #[error(transparent)]
    UnexpectedError(#[from] anyhow::Error),
    #[error(transparent)]
    VocadbClientError(#[from] VocadbClientError),
}

impl actix_web::ResponseError for AppResponseError {
    fn status_code(&self) -> StatusCode {
        fn vocadb_client_error(e: &VocadbClientError) -> StatusCode {
            match e {
                VocadbClientError::BadCredentialsError => StatusCode::UNAUTHORIZED,
                VocadbClientError::NotFoundError => StatusCode::NOT_FOUND,
                _ => StatusCode::INTERNAL_SERVER_ERROR,
            }
        }

        return match self {
            AppResponseError::BadCredentialsError => StatusCode::UNAUTHORIZED,
            AppResponseError::ConstraintViolationError(_) => StatusCode::BAD_REQUEST,
            AppResponseError::VocadbClientError(e) => vocadb_client_error(e),
            AppResponseError::UnexpectedError(_) => StatusCode::INTERNAL_SERVER_ERROR,
            AppResponseError::NotFoundError => StatusCode::NOT_FOUND
        };
    }
}
