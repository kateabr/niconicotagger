use std::error::Error;
use actix_web::body::BoxBody;
use actix_web::http::{header, StatusCode};
use actix_web::HttpResponse;
use serde::Serialize;

use crate::client::errors::VocadbClientError;

pub type Result<T, E = AppResponseError> = core::result::Result<T, E>;

#[derive(thiserror::Error, Debug)]
pub enum AppResponseError {
    #[error("{0}")]
    BadRequestError(String),
    #[error("{0}")]
    NotFoundError(String),
    #[error("{0}")]
    ConstraintViolationError(String),
    #[error(transparent)]
    UnexpectedError(#[from] anyhow::Error),
    #[error(transparent)]
    VocadbClientError(#[from] VocadbClientError),
}

#[derive(Serialize, Debug)]
pub struct ErrorResponse {
    pub code: u16,
    pub message: String,
    pub stacktrace: Vec<String>,
}

impl actix_web::ResponseError for AppResponseError {
    fn status_code(&self) -> StatusCode {
        fn vocadb_client_error(e: &VocadbClientError) -> StatusCode {
            match e {
                VocadbClientError::BadCredentialsError => StatusCode::UNAUTHORIZED,
                VocadbClientError::NotFoundError(_) => StatusCode::NOT_FOUND,
                VocadbClientError::AmbiguousResponseError => StatusCode::BAD_REQUEST,
                _ => StatusCode::INTERNAL_SERVER_ERROR,
            }
        }

        return match self {
            AppResponseError::ConstraintViolationError(_) => StatusCode::BAD_REQUEST,
            AppResponseError::VocadbClientError(e) => vocadb_client_error(e),
            AppResponseError::UnexpectedError(_) => StatusCode::INTERNAL_SERVER_ERROR,
            AppResponseError::NotFoundError(_) => StatusCode::NOT_FOUND,
            AppResponseError::BadRequestError(_) => StatusCode::BAD_REQUEST
        };
    }

    fn error_response(&self) -> HttpResponse<BoxBody> {
        let message = match self {
            AppResponseError::ConstraintViolationError(e) => {
                format!("Constraint violation: {}", e.to_string())
            }
            AppResponseError::UnexpectedError(e) => format!("Unexpected error: {}", e.to_string()),
            AppResponseError::VocadbClientError(e) => {
                format!("Web client error: {}", e.to_string())
            }
            AppResponseError::NotFoundError(e) => format!("Web client error: {}", e.to_string()),
            AppResponseError::BadRequestError(e) => format!("Web client error: {}", e.to_string())
        };
        let stacktrace = collect_stacktrace(self);
        let code = self.status_code();
        let response = ErrorResponse {
            code: code.as_u16(),
            message,
            stacktrace,
        };

        HttpResponse::build(code)
            .append_header((header::CONTENT_TYPE, "application/json"))
            .json(response)
    }
}

pub fn collect_stacktrace(err: &dyn Error) -> Vec<String> {
    let mut stacktrace = vec![];
    let mut cause = err.source();
    while cause.is_some() {
        let err = cause.unwrap();
        stacktrace.push(err.to_string());
        cause = err.source();
    }
    stacktrace
}
