#[cfg(feature = "desktop")]
use actix_cors::Cors;
#[cfg(feature = "desktop")]
use actix_web::{App, HttpServer};
#[cfg(feature = "desktop")]
use chrono::Utc;
#[cfg(feature = "desktop")]
use clap::Parser;
#[cfg(feature = "desktop")]
use std::sync::Arc;
#[cfg(feature = "desktop")]
use std::time::Duration as StdDuration;
#[cfg(feature = "desktop")]
use tokio::sync::{Mutex, RwLock};
#[cfg(feature = "desktop")]
use tokio::task::JoinHandle;
#[cfg(feature = "desktop")]
use tokio::time;
#[cfg(feature = "desktop")]
use utoipa::OpenApi;
#[cfg(feature = "desktop")]
use utoipa_swagger_ui::SwaggerUi;

#[cfg(feature = "desktop")]
mod api;
#[cfg(feature = "desktop")]
mod identity;
#[cfg(feature = "desktop")]
mod net;
#[cfg(feature = "desktop")]
mod node;
#[cfg(feature = "desktop")]
mod p2p;

#[cfg(feature = "desktop")]
use crate::identity::identity_manager::load_or_generate_identity;
#[cfg(feature = "desktop")]
use crate::node::NodeConfig;
#[cfg(feature = "desktop")]
use crate::p2p::encryption::CryptoIdentity;
#[cfg(feature = "desktop")]
use crate::p2p::node::{poll_global_registries, run_http_reachability_checks, Node};
#[cfg(feature = "desktop")]
use net::{run_beacon_listener, run_beacon_sender, run_peer_logger, PeerSet};

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

    /// Дополнительные HTTP(S) реестры для глобального обнаружения
    #[arg(long = "global-registry", value_delimiter = ',')]
    global_registry: Vec<String>,

    /// Enable LAN nearby sync (UDP broadcast discovery)
    #[arg(long, default_value_t = false)]
    nearby_sync: bool,

    /// Nearby sync broadcast interval in milliseconds
    #[arg(long, default_value_t = 3000)]
    nearby_interval_ms: u64,
}

#[cfg(all(
    feature = "desktop",
    not(target_os = "windows"),
    not(target_os = "android")
))]
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

#[cfg(all(feature = "desktop", any(target_os = "windows", target_os = "android")))]
fn guess_local_ip() -> String {
    // Minimal fallback for cross-compile; runtime detection handled separately in native workflow
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

    let env_registries: Vec<String> = std::env::var("TRUTH_GLOBAL_REGISTRIES")
        .ok()
        .map(|raw| {
            raw.split(',')
                .filter_map(|s| {
                    let trimmed = s.trim();
                    if trimmed.is_empty() {
                        None
                    } else {
                        Some(trimmed.to_string())
                    }
                })
                .collect()
        })
        .unwrap_or_default();
    let registry_urls = if args.global_registry.is_empty() {
        env_registries
    } else {
        args.global_registry.clone()
    };

    let peers = PeerSet(Arc::new(RwLock::new(Default::default())));

    // Запуск Beacon discovery
    tokio::spawn(run_beacon_sender(http_addr.clone()));
    tokio::spawn(run_beacon_listener(peers.clone()));
    tokio::spawn(run_peer_logger(peers.clone()));

    // Инициализация БД
    let mut conn =
        core_lib::storage::create_db_connection(&args.db).map_err(std::io::Error::other)?;
    core_lib::storage::seed_knowledge_base(&mut conn, "ru").map_err(std::io::Error::other)?;
    let conn_data = Arc::new(Mutex::new(conn));

    // 🔒 Генерация крипто-идентичности узла (через identity_manager)
    let crypto_identity: Arc<CryptoIdentity> = Arc::new(load_or_generate_identity());
    println!("Node public key: {}", crypto_identity.public_key_hex());

    // Конфигурация узла для discovery
    let node_config = NodeConfig {
        bind_host: "0.0.0.0".into(),
        bind_port: args.port,
        public_host: Some(host_ip.clone()),
        public_port: Some(args.port),
        timing: core_lib::DEFAULT_DISCOVERY_TIMING,
        global_registry_urls: registry_urls.clone(),
    };
    let scheduler_cfg = Arc::new(node_config.clone());

    // Список известных пиров
    let peers_list = vec!["http://127.0.0.1:8081".to_string()];

    //Теперь при создании Node передаём CryptoIdentity и пул БД
    let node = Arc::new(Node::new(
        peers_list,
        conn_data.clone(),
        crypto_identity.clone(),
        node_config,
    ));
    let node_for_task = node.clone();
    tokio::spawn(async move {
        node_for_task.start().await;
    });

    // TTL cleanup scheduler
    {
        let cleanup_conn = conn_data.clone();
        let cleanup_interval = scheduler_cfg.timing.cleanup_interval;
        tokio::spawn(async move {
            let mut interval = time::interval(cleanup_interval);
            loop {
                interval.tick().await;
                let removed = {
                    let conn = cleanup_conn.lock().await;
                    core_lib::storage::prune_stale_nodes(&conn, Utc::now().timestamp())
                };
                match removed {
                    Ok(count) if count > 0 => {
                        log::info!("TTL cleanup removed {count} expired nodes");
                        log::info!(
                            "discovery.cleanup.completed pruned={} expired={} unreachable=0",
                            count,
                            count
                        );
                    }
                    Ok(_) => {
                        log::debug!("discovery.cleanup.completed pruned=0 expired=0 unreachable=0");
                    }
                    Err(e) => {
                        log::warn!("TTL cleanup failed: {e}");
                        log::warn!("discovery.cleanup.error error={}", e);
                    }
                }
            }
        });
    }

    // Global registry polling scheduler
    if !scheduler_cfg.global_registry_urls.is_empty() {
        let registry_conn = conn_data.clone();
        let registry_cfg = scheduler_cfg.clone();
        tokio::spawn(async move {
            let mut interval = time::interval(registry_cfg.timing.global_poll_interval);
            loop {
                interval.tick().await;
                match poll_global_registries(registry_cfg.as_ref(), registry_conn.clone()).await {
                    Ok(count) if count > 0 => {
                        log::info!("discovery.registry.poll.completed total_added={}", count);
                    }
                    Ok(_) => {
                        log::debug!("discovery.registry.poll.completed total_added=0");
                    }
                    Err(e) => {
                        log::warn!("Global registry poll failed: {e}");
                        log::warn!("discovery.registry.error error={}", e);
                    }
                }
            }
        });
    }

    // HTTP reachability scheduler
    {
        let reach_conn = conn_data.clone();
        tokio::spawn(async move {
            let mut interval = time::interval(scheduler_cfg.timing.cleanup_interval);
            let timeout = StdDuration::from_secs(core_lib::HEALTH_CHECK_TIMEOUT_SECS);
            loop {
                interval.tick().await;
                match run_http_reachability_checks(
                    reach_conn.clone(),
                    timeout,
                    core_lib::HEALTH_CHECK_RETRY_LIMIT,
                )
                .await
                {
                    Ok(count) => {
                        if count > 0 {
                            log::debug!("HTTP reachability sweep updated {count} nodes");
                        }
                    }
                    Err(e) => log::warn!("HTTP reachability checks failed: {e}"),
                }
            }
        });
    }

    // Nearby sync handle state for dynamic control (persisted config)
    let nearby_handle: Arc<RwLock<Option<JoinHandle<()>>>> = Arc::new(RwLock::new(None));
    // Load persisted config
    let (persist_enabled, persist_interval) = {
        let guard = conn_data.lock().await;
        core_lib::storage::load_app_config(&guard).unwrap_or((false, 3000))
    };
    let effective_enabled = if args.nearby_sync {
        true
    } else {
        persist_enabled
    };
    let effective_interval = if args.nearby_sync {
        args.nearby_interval_ms
    } else {
        persist_interval
    };
    if effective_enabled {
        let _conn_for_nearby = conn_data.clone();
        let _id_for_nearby = crypto_identity.clone();
        let _port_for_nearby = args.port;
        let _interval_ms = effective_interval;
        let h = tokio::spawn(async move {
            #[cfg(any(test, feature = "p2p-client-sync"))]
            crate::p2p::wifi_direct::start_nearby_sync(
                _conn_for_nearby,
                _id_for_nearby,
                _port_for_nearby,
                _interval_ms,
            )
            .await;
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
