use actix_web::{http::StatusCode, HttpResponse};

use crate::client::errors::ClientError;

#[derive(derive_more::Display, Debug)]
pub enum AppError {
    #[display(fmt = "An error occurred during a remote call")]
    WebClientError(crate::client::errors::ClientError),

    #[display(fmt = "Provided credentials are not valid")]
    BadCredentialsError,

    #[display(fmt = "Some of the request fields have invalid/forbidden values")]
    ConstraintViolationError,

    #[display(fmt = "Internal Service Error")]
    InternalServiceError,

    #[display(fmt = "Access restricted for your user group")]
    NoPermissionError,

    #[display(fmt = "Tag is not mapped")]
    NoMappingError,
}

impl std::error::Error for AppError {}

#[derive(serde::Serialize)]
pub struct ErrorResponse {
    pub code: u16,
    // pub error: String,
    pub message: String,
}

impl actix_web::error::ResponseError for AppError {
    fn status_code(&self) -> StatusCode {
        match *self {
            Self::WebClientError(ref client_error) => match client_error {
                ClientError::BadCredentialsError => StatusCode::UNAUTHORIZED,
                ClientError::ResponseError => StatusCode::INTERNAL_SERVER_ERROR,
                ClientError::ConnectionError(_) => StatusCode::INTERNAL_SERVER_ERROR,
                ClientError::BadArgumentError => StatusCode::BAD_REQUEST
            },
            Self::BadCredentialsError => StatusCode::UNAUTHORIZED,
            Self::NoPermissionError => StatusCode::UNAUTHORIZED,
            Self::InternalServiceError => StatusCode::INTERNAL_SERVER_ERROR,
            Self::ConstraintViolationError => StatusCode::BAD_REQUEST,
            Self::NoMappingError => StatusCode::BAD_REQUEST,
        }
    }

    fn error_response(&self) -> HttpResponse {
        let status_code = self.status_code();
        let error_response = ErrorResponse {
            code: status_code.as_u16(),
            message: self.to_string(),
            // error: self.name(),
        };
        HttpResponse::build(status_code).json(error_response)
    }
}

impl From<actix_web::error::ParseError> for AppError {
    fn from(_err: actix_web::error::ParseError) -> AppError {
        AppError::InternalServiceError
    }
}

impl From<jsonwebtoken::errors::Error> for AppError {
    fn from(_err: jsonwebtoken::errors::Error) -> AppError {
        AppError::BadCredentialsError
    }
}

impl From<crate::client::errors::ClientError> for AppError {
    fn from(err: crate::client::errors::ClientError) -> AppError {
        AppError::WebClientError(err)
    }
}

impl From<regex::Error> for AppError {
    fn from(_err: regex::Error) -> AppError {
        AppError::InternalServiceError
    }
}

impl From<std::num::ParseIntError> for AppError {
    fn from(_err: std::num::ParseIntError) -> AppError {
        AppError::InternalServiceError
    }
}
