use actix_web::{App, HttpServer};
use actix_cors::Cors;
use utoipa_swagger_ui::SwaggerUi;
use utoipa::OpenApi;
use clap::Parser;
use std::sync::Arc;
use tokio::sync::{Mutex, RwLock};

mod api;
mod net;
mod p2p;

use net::{run_beacon_listener, run_beacon_sender, run_peer_logger, PeerSet};
use crate::p2p::node::Node;
use crate::p2p::encryption::CryptoIdentity;

#[derive(Parser, Debug)]
#[command(name = "truth_training")]
struct Args {
    #[arg(long, default_value = "8080")]
    port: u16,

    #[arg(long)]
    http_addr: Option<String>,

    #[arg(long, default_value = "truth_training.db")]
    db: String,
}

fn guess_local_ip() -> String {
    if let Ok(interfaces) = get_if_addrs::get_if_addrs() {
        for iface in interfaces {
            if let std::net::IpAddr::V4(ip) = iface.ip() {
                if !ip.is_loopback() {
                    return ip.to_string();
                }
            }
        }
    }
    "127.0.0.1".into()
}

#[actix_web::main]
async fn main() -> std::io::Result<()> {
    // Включаем логирование (по умолчанию выводит в stdout)
    env_logger::init();
    println!("Starting Truth node...");
    
    let args = Args::parse();
    let host_ip = guess_local_ip();
    println!("Detected local IP: {}", host_ip);

    let http_addr = args
        .http_addr
        .unwrap_or_else(|| format!("http://{host_ip}:{}", args.port));

    let peers = PeerSet(Arc::new(RwLock::new(Default::default())));

    // Запуск Beacon discovery
    tokio::spawn(run_beacon_sender(http_addr.clone()));
    tokio::spawn(run_beacon_listener(peers.clone()));
    tokio::spawn(run_peer_logger(peers.clone()));

    // Инициализация БД
    let mut conn = core_lib::storage::create_db_connection(&args.db)
        .map_err(std::io::Error::other)?;
    core_lib::storage::seed_knowledge_base(&mut conn, "ru")
        .map_err(std::io::Error::other)?;
    let conn_data = Arc::new(Mutex::new(conn));

    // 🔒 Генерация крипто-идентичности узла
    let crypto_identity = CryptoIdentity::new();
    println!("Node public key: {}", crypto_identity.public_key_hex());

    // Список известных пиров
    let peers_list = vec!["http://127.0.0.1:8081".to_string()];

    //Теперь при создании Node передаём CryptoIdentity и пул БД
    let crypto_identity = Arc::new(CryptoIdentity::new());
    let node = Arc::new(Node::new(peers_list, conn_data.clone(), crypto_identity.clone()));
    let node_for_task = node.clone();
    tokio::spawn(async move {
        node_for_task.start().await;
    });

    // Запуск HTTP сервера
    HttpServer::new(move || {
        App::new()
            // Wide-open CORS for Android debugging; lock down in production
            .wrap(Cors::permissive())
            .app_data(actix_web::web::Data::new(conn_data.clone()))
            .app_data(actix_web::web::Data::new(node.clone()))
            .app_data(actix_web::web::Data::new(crate::api::AppInfo {
                db_path: args.db.clone(),
                p2p_enabled: true,
            }))
            .configure(api::routes)
            // Serve Swagger UI and OpenAPI JSON
            .service(
                SwaggerUi::new("/api/docs/{_:.*}")
                    .url("/api/docs/openapi.json", crate::api::ApiDoc::openapi()),
            )
    })
    .bind(("0.0.0.0", args.port))?
    .run()
    .await
}
