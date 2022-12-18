pub type Result<T, E = VocadbClientError> = core::result::Result<T, E>;

#[derive(thiserror::Error, Debug)]
#[allow(clippy::enum_variant_names)]
pub enum VocadbClientError {
    #[error("Provided credentials are not valid")]
    BadCredentialsError,
    #[error("Ambiguous response")]
    AmbiguousResponseError,
    #[error("{0}")]
    NotFoundError(String),
    #[error(transparent)]
    SendRequestError(#[from] awc::error::SendRequestError),
    #[error(transparent)]
    ConnectError(#[from] awc::error::ConnectError),
    #[error(transparent)]
    PayloadError(#[from] awc::error::PayloadError),
    #[error(transparent)]
    UnexpectedError(#[from] anyhow::Error),
}
