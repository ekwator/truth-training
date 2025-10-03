use actix_web::{App, HttpServer};
use clap::Parser;
use std::sync::Arc;
use tokio::sync::Mutex;

mod api;
mod net;

use net::{run_beacon_listener, run_beacon_sender, run_peer_logger, PeerSet};
use tokio::sync::RwLock;

#[derive(Parser, Debug)]
#[command(name = "truth_training")]
struct Args {
    #[arg(long, default_value = "8080")]
    port: u16,

    #[arg(long)]
    http_addr: Option<String>,
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
    let args = Args::parse();

    let host_ip = guess_local_ip();
    println!("Detected local IP: {}", host_ip);

    let http_addr = args
        .http_addr
        .unwrap_or_else(|| format!("http://{host_ip}:{}", args.port));

    let peers = PeerSet(Arc::new(RwLock::new(Default::default())));

    // Фоновые задачи
    tokio::spawn(run_beacon_sender(http_addr.clone()));
    tokio::spawn(run_beacon_listener(peers.clone()));
    tokio::spawn(run_peer_logger(peers.clone()));

    // Инициализация SQLite
    let mut conn = core_lib::storage::create_db_connection("truth_training.db")
        .map_err(|e| std::io::Error::new(std::io::ErrorKind::Other, e))?;
    
    // Заполняем базу данных начальными данными
    core_lib::storage::seed_knowledge_base(&mut conn, "ru")
        .map_err(|e| std::io::Error::new(std::io::ErrorKind::Other, e))?;
    
    let conn_data = Arc::new(Mutex::new(conn));

    // HTTP сервер
    HttpServer::new(move || {
        App::new()
            .app_data(actix_web::web::Data::new(conn_data.clone()))
            .configure(api::routes)
    })
    .bind(("0.0.0.0", args.port))?
    .run()
    .await
}
