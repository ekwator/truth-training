#[cfg(feature = "desktop")]
use actix_web::{App, HttpServer};
#[cfg(feature = "desktop")]
use actix_cors::Cors;
#[cfg(feature = "desktop")]
use utoipa_swagger_ui::SwaggerUi;
#[cfg(feature = "desktop")]
use utoipa::OpenApi;
#[cfg(feature = "desktop")]
use clap::Parser;
#[cfg(feature = "desktop")]
use std::sync::Arc;
#[cfg(feature = "desktop")]
use tokio::sync::{Mutex, RwLock};
#[cfg(feature = "desktop")]
use tokio::task::JoinHandle;

#[cfg(feature = "desktop")]
mod api;
#[cfg(feature = "desktop")]
mod net;
#[cfg(feature = "desktop")]
mod p2p;

#[cfg(feature = "desktop")]
use net::{run_beacon_listener, run_beacon_sender, run_peer_logger, PeerSet};
#[cfg(feature = "desktop")]
use crate::p2p::node::Node;
#[cfg(feature = "desktop")]
use crate::p2p::encryption::CryptoIdentity;

#[cfg(feature = "desktop")]
#[derive(Parser, Debug)]
#[command(name = "truth_training")]
struct Args {
    #[arg(long, default_value = "8080")]
    port: u16,

    #[arg(long)]
    http_addr: Option<String>,

    #[arg(long, default_value = "truth_training.db")]
    db: String,

    /// Enable LAN nearby sync (UDP broadcast discovery)
    #[arg(long, default_value_t = false)]
    nearby_sync: bool,

    /// Nearby sync broadcast interval in milliseconds
    #[arg(long, default_value_t = 3000)]
    nearby_interval_ms: u64,
}

#[cfg(feature = "desktop")]
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

#[cfg(feature = "desktop")]
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
    let crypto_identity = Arc::new(CryptoIdentity::new());
    println!("Node public key: {}", crypto_identity.public_key_hex());

    // Список известных пиров
    let peers_list = vec!["http://127.0.0.1:8081".to_string()];

    //Теперь при создании Node передаём CryptoIdentity и пул БД
    let node = Arc::new(Node::new(peers_list, conn_data.clone(), crypto_identity.clone()));
    let node_for_task = node.clone();
    tokio::spawn(async move {
        node_for_task.start().await;
    });

    // Nearby sync handle state for dynamic control (persisted config)
    let nearby_handle: Arc<RwLock<Option<JoinHandle<()>>>> = Arc::new(RwLock::new(None));
    // Load persisted config
    let (persist_enabled, persist_interval) = {
        let guard = conn_data.lock().await;
        core_lib::storage::load_app_config(&guard).unwrap_or((false, 3000))
    };
    let effective_enabled = if args.nearby_sync { true } else { persist_enabled };
    let effective_interval = if args.nearby_sync { args.nearby_interval_ms } else { persist_interval };
    if effective_enabled {
        let _conn_for_nearby = conn_data.clone();
        let _id_for_nearby = crypto_identity.clone();
        let _port_for_nearby = args.port;
        let _interval_ms = effective_interval;
        let h = tokio::spawn(async move {
            #[cfg(any(test, feature = "p2p-client-sync"))]
            crate::p2p::wifi_direct::start_nearby_sync(_conn_for_nearby, _id_for_nearby, _port_for_nearby, _interval_ms).await;
        });
        *nearby_handle.write().await = Some(h);
    }

    // Запуск HTTP сервера
    HttpServer::new(move || {
        App::new()
            // Wide-open CORS for Android debugging; lock down in production
            .wrap(Cors::permissive())
            .app_data(actix_web::web::Data::new(conn_data.clone()))
            .app_data(actix_web::web::Data::new(node.clone()))
            .app_data(actix_web::web::Data::new(crypto_identity.clone()))
            .app_data(actix_web::web::Data::new(nearby_handle.clone()))
            .app_data(actix_web::web::Data::new(crate::api::AppInfo {
                db_path: args.db.clone(),
                p2p_enabled: true,
                http_port: args.port,
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

#[cfg(not(feature = "desktop"))]
fn main() {
    println!("truth_core built without desktop feature; no binary runtime");
}
