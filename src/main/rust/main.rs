use actix_web::{App, Error, HttpRequest, HttpResponse, HttpServer, middleware};
use actix_web::error::{InternalError, JsonPayloadError};
use actix_web::http::StatusCode;
use actix_web_httpauth::middleware::HttpAuthentication;
use crate::web::errors::{collect_stacktrace, ErrorResponse};

mod client;
mod web;

#[actix_web::main]
async fn main() -> std::io::Result<()> {
    if let Err(_) = std::env::var("RUST_LOG") {
        std::env::set_var("RUST_LOG", "debug")
    }
    env_logger::init();

    HttpServer::new(|| {
        App::new()
            .app_data(actix_web::web::JsonConfig::default().error_handler(json_error_handler))
            .wrap(middleware::Logger::default())
            .service(web::controller::login)
            .service(
                actix_web::web::scope("/api")
                    .wrap(HttpAuthentication::bearer(
                        web::middleware::auth_token::validate,
                    ))
                    .service(web::controller::fetch_videos)
                    .service(web::controller::fetch_videos_by_tag)
                    .service(web::controller::fetch_videos_from_db)
                    .service(web::controller::fetch_videos_from_db_before_since)
                    .service(web::controller::assign_tag)
                    .service(web::controller::lookup_and_assign_tag)
                    .service(web::controller::get_mapped_tags)
                    .service(web::controller::fetch_from_db_by_event_tag)
            )
    })
    .bind("127.0.0.1:8080")?
    .run()
    .await
}

pub fn json_error_handler(err: JsonPayloadError, _req: &HttpRequest) -> Error {
    let stacktrace = collect_stacktrace(&err);
    let message = err.to_string();
    let code = StatusCode::BAD_REQUEST.as_u16();
    let response = HttpResponse::BadRequest().json(&ErrorResponse {
        code,
        message,
        stacktrace,
    });
    InternalError::from_response(err, response).into()
}
