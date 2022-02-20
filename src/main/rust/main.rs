use actix_web::{App, HttpServer, middleware};
use actix_web_httpauth::middleware::HttpAuthentication;

mod client;
mod web;

#[actix_web::main]
async fn main() -> std::io::Result<()> {
    if let Err(_) = std::env::var("RUST_LOG") {
        std::env::set_var("RUST_LOG", "info")
    }
    env_logger::init();

    HttpServer::new(|| {
        App::new()
            .wrap(middleware::Logger::default())
            .service(web::controller::login)
            .service(
                actix_web::web::scope("/api")
                    .wrap(HttpAuthentication::bearer(
                        web::middleware::auth_token::validate,
                    ))
                    .service(web::controller::fetch_videos)
                    .service(web::controller::fetch_videos_from_db)
                    .service(web::controller::fetch_videos_from_db_before_since)
                    .service(web::controller::assign_tag)
                    .service(web::controller::lookup_and_assign_tag)
                    .service(web::controller::get_mapped_tags)
            )
    })
    .bind("127.0.0.1:8080")?
    .run()
    .await
}
