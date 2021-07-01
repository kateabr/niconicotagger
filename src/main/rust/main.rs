use actix_web::{App, HttpServer, middleware, web};
use actix_web_httpauth::middleware::HttpAuthentication;

mod client;
mod controller;
mod errors;
mod service;

#[actix_web::main]
async fn main() -> std::io::Result<()> {
    if let Err(_) = std::env::var("RUST_LOG") {
        std::env::set_var("RUST_LOG", "info")
    }
    env_logger::init();

    HttpServer::new(|| {
        App::new()
            .wrap(middleware::Logger::default())
            .route("/api/login", web::post().to(controller::login))
            .service(
                web::scope("/api")
                    .wrap(HttpAuthentication::bearer(service::auth::validate_token))
                    .route(
                        "/fetch",
                        web::post().to(controller::fetch_videos),
                    )
                    .route("/assign",
                    web::post().to(controller::assign_tag))
                    .route("/get_mapped_tags",
                           web::post().to(controller::get_mapped_tags))
            )
    })
    .bind("127.0.0.1:8080")?
    .run()
    .await
}
