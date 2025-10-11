use clap::{Parser, Subcommand, ValueEnum};
use std::path::PathBuf;
use colored::*;
use core_lib::storage;
use serde::{Deserialize, Serialize};
use std::fs;
use std::time::{SystemTime, UNIX_EPOCH};

#[path = "../diagnostics.rs"]
mod diagnostics;
use diagnostics::{run_diagnostics, print_diagnostic_summary, reset_local_data, DiagnosticResult};

#[path = "../config_utils.rs"]
mod config_utils;
use config_utils::{default_config, load_config, save_config, Config as NodeConfig};

#[path = "../status_utils.rs"]
mod status_utils;
use status_utils::{get_recent_sync_events, print_status_summary, Peers, PeerItem};
use truth_core::p2p::sync::SyncResult;

#[derive(Parser, Debug)]
#[command(name = "truthctl", version, about = "CLI для P2P синхронизации Truth Core")] 
struct Cli {
    #[command(subcommand)]
    command: Commands,
}

#[derive(Subcommand, Debug)]
enum Commands {
    /// Выполнить синхронизацию с пиром
    Sync {
        /// URL пира (например, http://127.0.0.1:8080)
        #[arg(long)]
        peer: Option<String>,
        /// Путь к JSON-файлу ключей (если не указан — берётся из локального хранилища)
        #[arg(long)]
        identity: Option<PathBuf>,
        /// Путь к БД
        #[arg(long, default_value = "truth.db")]
        db: PathBuf,
        /// Режим: full | incremental | push | pull
        #[arg(long, default_value = "full")]
        mode: Mode,
    },
    /// Показать статус узла и БД
    Status {
        /// Путь к БД
        #[arg(long, default_value = "truth.db")]
        db: PathBuf,
        /// Путь к JSON-файлу ключей (необязательно)
        #[arg(long)]
        identity: Option<PathBuf>,
    },
    /// Проверить целостность данных и подписей
    Verify {
        /// Путь к БД
        #[arg(long, default_value = "truth.db")]
        db: PathBuf,
    },
    /// Управление рейтингами узлов и групп
    Ratings {
        /// Путь к БД
        #[arg(long, default_value = "truth.db")]
        db: PathBuf,
        /// Пересчитать рейтинги
        #[arg(long)]
        recalc: bool,
        /// Подкоманда: trust
        #[command(subcommand)]
        cmd: Option<RatingsCmd>,
    },
    /// Управление ключами
    Keys {
        #[command(subcommand)]
        cmd: KeysCmd,
    },
    /// Инициализация локального узла и автодобавление в peers.json
    InitNode {
        node_name: String,
        #[arg(long, default_value_t = 8080)]
        port: u16,
        #[arg(long, default_value = "truth.db")]
        db: PathBuf,
        #[arg(long)]
        auto_peer: bool,
    },
    /// Управление конфигурацией узла
    Config {
        #[command(subcommand)]
        cmd: ConfigCmd,
    },
    /// Управление пирами и синхронизация
    Peers {
        #[command(subcommand)]
        cmd: PeersCmd,
    },
    /// Журналы синхронизации
    Logs {
        #[command(subcommand)]
        cmd: LogsCmd,
    },
    /// Диагностика узла и среды
    Diagnose {
        /// Подробный вывод JSON-конфигурации, пиров и ключей
        #[arg(long)]
        verbose: bool,
        /// Диагностика серверной части (HTTP/API, БД, P2P)
        #[arg(long)]
        server: bool,
    },
    /// Сброс локальных данных узла (БД, журналы, peers)
    ResetData {
        /// Безопасное подтверждение удаления peers.json
        #[arg(long)]
        confirm: bool,
        /// После очистки — переинициализировать узел (init-node) и управлять ключами
        #[arg(long)]
        reinit: bool,
    },
}

#[derive(Copy, Clone, Debug, ValueEnum)]
enum Mode { Full, Incremental, Push, Pull }

#[derive(serde::Deserialize)]
#[allow(dead_code)] // может быть неиспользован без фичи p2p-client-sync
struct KeyFile { private_key: String, public_key: String }

#[derive(Subcommand, Debug)]
enum KeysCmd {
    /// Импорт пары ключей Ed25519 в локальное хранилище (~/.truthctl/keys.json)
    Import { private_key_hex: String, public_key_hex: String },
    /// Список импортированных ключей
    List,
    /// Генерация новой пары ключей Ed25519 (опционально сохранить)
    Generate { #[arg(long)] save: bool },
}

#[derive(Subcommand, Debug)]
enum PeersCmd {
    /// Список пиров из ~/.truthctl/peers.json
    List,
    /// Добавить пира
    Add { url: String, public_key: String },
    /// Синхронизировать со всеми пирами
    SyncAll {
        /// Режим: full или incremental
        #[arg(long, default_value = "full")] mode: String,
        /// Сухой прогон без отправки
        #[arg(long)] dry_run: bool,
    },
}

#[derive(Subcommand, Debug)]
enum ConfigCmd {
    /// Показать текущую конфигурацию узла
    Show,
    /// Установить значение по ключу
    Set { key: String, value: String },
    /// Сбросить конфигурацию к значениям по умолчанию
    Reset { #[arg(long)] confirm: bool },
}

#[derive(Subcommand, Debug)]
enum LogsCmd {
    /// Показать последние записи журнала синхронизации
    Show { #[arg(long, default_value_t = 50)] limit: usize, #[arg(long, default_value = "truth.db")] db: PathBuf },
    /// Очистить журнал синхронизации
    Clear { #[arg(long, default_value = "truth.db")] db: PathBuf },
}

#[derive(Subcommand, Debug)]
enum RatingsCmd {
    /// Показать доверие: локальный уровень, средняя сеть, дельты
    Trust { #[arg(long)] verbose: bool },
}

#[tokio::main(flavor = "multi_thread")] 
async fn main() -> anyhow::Result<()> {
    let cli = Cli::parse();
    match cli.command {
        Commands::Sync { peer, identity, db, mode } => {
            let peer = peer.unwrap_or_else(|| {
                // Если не указан --peer, пытаемся взять первого из peers.json
                let mut url = String::new();
                if let Ok(p) = load_peers() { if let Some(first) = p.peers.first() { url = first.url.clone(); } }
                if url.is_empty() { eprintln!("{}", "No peer specified. Use --peer or add peers.json".red()); }
                url
            });
            let identity_path = identity.unwrap_or_else(|| PathBuf::from("") );
            run_sync(peer, identity_path, db, mode).await
        }
        Commands::Status { db, identity } => {
            run_status(db, identity).await
        }
        Commands::Verify { db } => {
            run_verify(db).await
        }
        Commands::Ratings { db, recalc, cmd } => {
            run_ratings(db, recalc, cmd).await
        }
        Commands::Keys { cmd } => {
            run_keys(cmd).await
        }
        Commands::InitNode { node_name, port, db, auto_peer } => {
            run_init_node(node_name, port, db, auto_peer).await
        }
        Commands::Peers { cmd } => {
            run_peers(cmd).await
        }
        Commands::Logs { cmd } => {
            run_logs(cmd).await
        }
        Commands::Diagnose { verbose, server } => {
            if server {
                let cfg = load_config().unwrap_or_else(|_| default_config());
                let base_url = format!("http://127.0.0.1:{}", cfg.port);
                let results = truth_core::server_diagnostics::run_diagnostics(&base_url, &cfg.db_path, cfg.p2p_enabled).await;
                println!("{}", "Server diagnostics".blue());
                for r in &results {
                    let status_colored = match r.status.as_str() {
                        s if s.contains("✅") => r.status.green(),
                        s if s.contains("❌") => r.status.red(),
                        _ => r.status.yellow(),
                    };
                    if r.message.is_empty() {
                        println!("- {}: {}", r.check.blue(), status_colored);
                    } else {
                        println!("- {}: {} — {}", r.check.blue(), status_colored, r.message);
                    }
                }
                return Ok(());
            }

            let results: Vec<DiagnosticResult> = run_diagnostics(verbose).await;
            print_diagnostic_summary(&results);
            if verbose {
                let cfg = load_config().unwrap_or_else(|_| default_config());
                let peers = load_peers().unwrap_or_default();
                let keys = load_keys().unwrap_or_default();
                let value = serde_json::json!({
                    "config": cfg,
                    "peers": peers,
                    "keys": keys,
                });
                println!("{}\n{}", "Verbose JSON:".blue(), serde_json::to_string_pretty(&value).unwrap_or_else(|_| "{}".to_string()));
            }
            Ok(())
        }
        Commands::ResetData { confirm, reinit } => {
            reset_local_data(confirm, reinit)?;
            Ok(())
        }
        Commands::Config { cmd } => {
            run_config(cmd).await
        }
    }
}

async fn run_status(db_path_flag: PathBuf, identity_path: Option<PathBuf>) -> anyhow::Result<()> {
    // 1) Конфиг узла
    let cfg = load_config().unwrap_or_else(|_| default_config());

    // Приоритет пути БД: флаг CLI > config.json
    let db_path = if db_path_flag != PathBuf::from("truth.db") {
        db_path_flag
    } else {
        PathBuf::from(cfg.db_path.clone())
    };

    // 2) Пиры из ~/.truthctl/peers.json
    let peers = load_peers().unwrap_or_default();

    // 3) Подключение к БД и чтение последних 5 логов синхронизации
    let mut recent: Vec<core_lib::models::SyncLog> = Vec::new();
    match storage::open_db(db_path.to_str().unwrap_or("truth.db")) {
        Ok(conn) => {
            // Таблица может отсутствовать в свежей БД — обрабатываем аккуратно
            match get_recent_sync_events(&conn, 5) {
                Ok(v) => {
                    recent = v;
                }
                Err(_) => {
                    // печатать будем ниже через print_status_summary
                }
            }
        }
        Err(_) => {
            // нет БД — сводка обработает это как отсутствие истории
        }
    }

    // 4) Вывод краткой сводки
    print_status_summary(&cfg, &peers, &recent);

    // 5) Дополнительно: показать публичный ключ, если указан identity
    if let Some(_identity_path) = identity_path {
        #[cfg(feature = "p2p-client-sync")]
        {
            let data = std::fs::read_to_string(&_identity_path)?;
            let k: KeyFile = serde_json::from_str(&data)?;
            let id = truth_core::p2p::encryption::CryptoIdentity::from_keypair_hex(&k.private_key, &k.public_key)
                .map_err(|e: String| anyhow::anyhow!(e))?;
            println!("{} {}", "Identity:".blue(), id.public_key_hex());
        }
        #[cfg(not(feature = "p2p-client-sync"))]
        {
            let _ = &_identity_path; // подавить предупреждение об неиспользуемой переменной
            println!("{}", "Identity display requires p2p-client-sync feature".yellow());
        }
    }

    Ok(())
}

async fn run_verify(db_path: PathBuf) -> anyhow::Result<()> {
    if !std::path::Path::new(&db_path).exists() {
        println!("{}", "❌ Database not found".red());
        return Ok(());
    }
    
    let conn = storage::open_db(db_path.to_str().unwrap())?;
    
    // Проверка событий с подписями
    let events = storage::load_truth_events(&conn)?;
    let mut valid_signatures = 0;
    let mut total_signed = 0;
    
    for event in &events {
        if let (Some(sig), Some(pk)) = (&event.signature, &event.public_key) {
            total_signed += 1;
            if truth_core::p2p::encryption::CryptoIdentity::from_public_key_hex(pk)
                .and_then(|id| id.verify_from_hex(format!("event:{}", event.id).as_bytes(), sig))
                .is_ok() {
                valid_signatures += 1;
            }
        }
    }
    
    println!("{}", format!("✅ Verified {}/{} signed events", valid_signatures, total_signed).green());
    // Используем первый ключ (если есть) для демонстрации from_keypair_hex и снятия предупреждений
    #[cfg(feature = "p2p-client-sync")]
    {
        if let Ok(store) = load_keys() {
            if let Some(k) = store.keys.first() {
                let _id = truth_core::p2p::encryption::CryptoIdentity::from_keypair_hex(&k.private_key_hex, &k.public_key_hex)
                    .map_err(|e: String| anyhow::anyhow!(e))?;
            }
        }
    }
    Ok(())
}

#[derive(Debug, Clone, Serialize, Deserialize)]
struct KeyPair { id: u64, private_key_hex: String, public_key_hex: String, created_at: String }

#[derive(Debug, Clone, Serialize, Deserialize, Default)]
struct KeyStore { keys: Vec<KeyPair> }

fn keystore_path() -> anyhow::Result<PathBuf> {
    let dir = dirs::home_dir().ok_or_else(|| anyhow::anyhow!("no HOME"))?.join(".truthctl");
    fs::create_dir_all(&dir)?;
    Ok(dir.join("keys.json"))
}

fn peers_path() -> anyhow::Result<PathBuf> {
    let dir = dirs::home_dir().ok_or_else(|| anyhow::anyhow!("no HOME"))?.join(".truthctl");
    fs::create_dir_all(&dir)?;
    Ok(dir.join("peers.json"))
}

fn load_peers() -> anyhow::Result<Peers> {
    let path = peers_path()?;
    if !path.exists() { return Ok(Peers::default()); }
    let data = fs::read_to_string(path)?;
    Ok(serde_json::from_str(&data).unwrap_or_default())
}

fn load_keys() -> anyhow::Result<KeyStore> {
    let path = keystore_path()?;
    if !path.exists() { return Ok(KeyStore::default()); }
    let data = fs::read_to_string(path)?;
    Ok(serde_json::from_str(&data)?)
}

fn save_keys(store: &KeyStore) -> anyhow::Result<()> {
    let path = keystore_path()?;
    let json = serde_json::to_string_pretty(store)?;
    fs::write(path, json)?;
    Ok(())
}

fn print_keys_table(store: &KeyStore) {
    println!("{}", "ID   PUBLIC(8)   CREATED_AT".blue());
    for k in &store.keys {
        let short = k.public_key_hex.get(0..8).unwrap_or("");
        println!("{:<4} {:<11} {}", k.id, short, k.created_at);
    }
}

async fn run_keys(cmd: KeysCmd) -> anyhow::Result<()> {
    match cmd {
        KeysCmd::Import { private_key_hex, public_key_hex } => {
            #[cfg(feature = "p2p-client-sync")]
            {
                // валидация ключей
                truth_core::p2p::encryption::CryptoIdentity::from_keypair_hex(&private_key_hex, &public_key_hex)
                    .map_err(|e: String| anyhow::anyhow!(e))?;
            }
            #[cfg(not(feature = "p2p-client-sync"))]
            {
                // Простая валидация длины hex строк
                if private_key_hex.len() != 64 || public_key_hex.len() != 64 {
                    anyhow::bail!("Invalid key length. Expected 64 hex characters for both keys");
                }
            }

            let mut store = load_keys()?;
            let next_id = store.keys.iter().map(|k| k.id).max().unwrap_or(0) + 1;
            let ts = SystemTime::now().duration_since(UNIX_EPOCH)?.as_secs();
            let created_at = chrono::DateTime::<chrono::Utc>::from(UNIX_EPOCH + std::time::Duration::from_secs(ts)).to_rfc3339();
            store.keys.push(KeyPair { id: next_id, private_key_hex, public_key_hex, created_at });
            save_keys(&store)?;
            println!("{}", "✅ Key imported".green());
        }
        KeysCmd::List => {
            let store = load_keys()?;
            if store.keys.is_empty() {
                println!("{}", "No keys found. Use 'truthctl keys import'".yellow());
            } else {
                print_keys_table(&store);
            }
        }
        KeysCmd::Generate { save } => {
            use ed25519_dalek::SigningKey;
            use rand::rngs::OsRng;
            let mut rng = OsRng;
            let sk = SigningKey::generate(&mut rng);
            let vk = sk.verifying_key();
            let priv_hex = hex::encode(sk.to_bytes());
            let pub_hex = hex::encode(vk.to_bytes());
            println!("private: {}\npublic: {}", priv_hex, pub_hex);
            if save {
                let mut store = load_keys()?;
                let next_id = store.keys.iter().map(|k| k.id).max().unwrap_or(0) + 1;
                let ts = SystemTime::now().duration_since(UNIX_EPOCH)?.as_secs();
                let created_at = chrono::DateTime::<chrono::Utc>::from(UNIX_EPOCH + std::time::Duration::from_secs(ts)).to_rfc3339();
                store.keys.push(KeyPair { id: next_id, private_key_hex: priv_hex, public_key_hex: pub_hex, created_at });
                save_keys(&store)?;
                println!("{}", "✅ Key saved to ~/.truthctl/keys.json".green());
            }
        }
    }
    Ok(())
}

// Типы Peers/PeerItem берём из status_utils

async fn run_init_node(node_name: String, port: u16, db: PathBuf, auto_peer: bool) -> anyhow::Result<()> {
    // get a key
    let ks = load_keys()?;
    let first = ks.keys.first().ok_or_else(|| anyhow::anyhow!("No keys found. Generate or import one: truthctl keys generate --save"))?;
    let cfg = NodeConfig {
        node_name,
        port,
        db_path: db.display().to_string(),
        public_key: first.public_key_hex.clone(),
        private_key: first.private_key_hex.clone(),
        auto_peer,
        p2p_enabled: true,
    };
    save_config(&cfg)?;
    // find path for user output
    let dir = dirs::home_dir().ok_or_else(|| anyhow::anyhow!("no HOME"))?.join(".truthctl");
    let cfg_path = dir.join("config.json");
    println!("{} {}", "✅ Node config written:".green(), cfg_path.display());

    if auto_peer {
        let mut peers: Peers = if peers_path()?.exists() {
            let data = fs::read_to_string(peers_path()?)?;
            serde_json::from_str(&data).unwrap_or_default()
        } else { Peers::default() };
        let url = format!("http://127.0.0.1:{}", port);
        let me = PeerItem { url, public_key: cfg.public_key.clone() };
        if !peers.peers.iter().any(|p| p.public_key == me.public_key) {
            peers.peers.push(me);
            fs::write(peers_path()?, serde_json::to_string_pretty(&peers)?)?;
            println!("{}", "✅ Added self node to peers.json".green());
        }
    }
    Ok(())
}

async fn run_config(cmd: ConfigCmd) -> anyhow::Result<()> {
    match cmd {
        ConfigCmd::Show => {
            let cfg = load_config()?;
            let json = serde_json::to_string_pretty(&cfg)?;
            println!("{}", json);
        }
        ConfigCmd::Set { key, value } => {
            let mut cfg = load_config()?;
            let k = key.to_lowercase();
            match k.as_str() {
                "node_name" => cfg.node_name = value,
                "port" => {
                    let p: u16 = value.parse().map_err(|_| anyhow::anyhow!("port must be a number (u16)"))?;
                    cfg.port = p;
                }
                "database" => {
                    cfg.db_path = value;
                }
                "auto_peer" => {
                    cfg.auto_peer = parse_bool(&value)?;
                }
                "p2p_enabled" => {
                    cfg.p2p_enabled = parse_bool(&value)?;
                }
                _ => {
                    anyhow::bail!(format!("Unsupported key: {}", key));
                }
            }
            save_config(&cfg)?;
            println!("{} {}", "✅ Updated".green(), key);
        }
        ConfigCmd::Reset { confirm } => {
            if !confirm {
                println!("{}", "This will overwrite ~/.truthctl/config.json. Rerun with --confirm to proceed.".yellow());
                return Ok(());
            }
            // keep keys if present?
            let existing = load_config().unwrap_or_else(|_| default_config());
            let mut cfg = default_config();
            // Preserve existing keys if they are set; otherwise leave empty defaults
            cfg.public_key = existing.public_key;
            cfg.private_key = existing.private_key;
            save_config(&cfg)?;
            println!("{}", "✅ Configuration reset".green());
        }
    }
    Ok(())
}

fn parse_bool(s: &str) -> anyhow::Result<bool> {
    match s.to_ascii_lowercase().as_str() {
        "true" | "1" | "yes" | "y" => Ok(true),
        "false" | "0" | "no" | "n" => Ok(false),
        _ => Err(anyhow::anyhow!("expected boolean (true/false)")),
    }
}

async fn run_peers(cmd: PeersCmd) -> anyhow::Result<()> {
    match cmd {
        PeersCmd::List => {
            let peers = load_peers().unwrap_or_default();
            if peers.peers.is_empty() {
                println!("{}", "No peers found".yellow());
            } else {
                println!("{}", "Peers:".blue());
                for p in peers.peers { println!("- {} ({})", p.url, &p.public_key.get(0..8).unwrap_or("")); }
            }
        }
        PeersCmd::Add { url, public_key } => {
            let mut peers = load_peers().unwrap_or_default();
            if peers.peers.iter().any(|p| p.url == url) {
                println!("{}", "Peer already exists".yellow());
            } else {
                peers.peers.push(PeerItem { url: url.clone(), public_key });
                fs::write(peers_path()?, serde_json::to_string_pretty(&peers)?)?;
                println!("{} {}", "✅ Peer added:".green(), url);
            }
        }
        PeersCmd::SyncAll { mode, dry_run } => {
            // Всегда поддерживаем dry-run: пишем записи в sync_logs без сетевых вызовов
            if dry_run {
                let peers = load_peers().unwrap_or_default();
                let cfg = load_config().unwrap_or_else(|_| default_config());
                let conn = storage::open_db(&cfg.db_path)?;
                if peers.peers.is_empty() {
                    println!("{}", "No peers to sync".yellow());
                } else {
                    for p in &peers.peers {
                        let _ = core_lib::storage::log_sync_event(&conn, &p.url, &mode, "dry-run", "no network call");
                    }
                    println!("{} {}", "✅ Dry-run logs recorded for".green(), peers.peers.len());
                }
                return Ok(());
            }

            #[cfg(feature = "p2p-client-sync")]
            {
                use truth_core::p2p::encryption::CryptoIdentity;
                use truth_core::p2p::sync::{bidirectional_sync_with_peer, incremental_sync_with_peer};

                let peers = load_peers().unwrap_or_default();
                let store = load_keys()?;
                let cfg = load_config().unwrap_or_else(|_| default_config());
                let me = store.keys.first().ok_or_else(|| anyhow::anyhow!("No keys found"))?;
                let identity = CryptoIdentity::from_keypair_hex(&me.private_key_hex, &me.public_key_hex)
                    .map_err(|e| anyhow::anyhow!(e))?;
                // Используем путь к БД из конфигурации узла
                let conn = storage::open_db(&cfg.db_path)?;

                if peers.peers.is_empty() { println!("{}", "No peers to sync".yellow()); }
                for p in &peers.peers {
                    if p.public_key == me.public_key_hex { continue; } // skip self
                    // Фильтр по доверию: опционально можно пропускать пиров с низким рейтингом
                    // пока простая проверка локального знания о peer (если есть запись)
                    let skip_low_trust = false; // future: сделать флаг CLI
                    if skip_low_trust {
                        let nodes = core_lib::storage::load_node_ratings(&conn)?;
                        if let Some(nr) = nodes.iter().find(|n| n.node_id == p.public_key) {
                            if nr.trust_score < 0.2 {
                                println!("{} {} (trust<{:.1})", "skip".yellow(), p.url, 0.2);
                                continue;
                            }
                        }
                    }
                    // реальная синхронизация
            let res = if mode == "incremental" {
                        let last = chrono::Utc::now().timestamp() - 3600;
                        incremental_sync_with_peer(&p.url, &identity, &conn, last).await
                    } else {
                        bidirectional_sync_with_peer(&p.url, &identity, &conn).await
                    };
                    match res {
                        Ok(r) => {
                            // Показать изменения доверия c цветами
                            let trust_summary = if r.nodes_trust_changed == 0 {
                                "⚪ =0".to_string()
                            } else {
                                let gains = r.trust_diff.iter().filter(|d| d.delta > 0.0).count();
                                let losses = r.trust_diff.iter().filter(|d| d.delta < 0.0).count();
                                format!("🟢 +{} 🔴 –{}", gains, losses)
                            };
                            println!("{} {}: +E{} +S{} +I{} (conflicts {}) [{}]",
                                "✅ synced".green(), p.url, r.events_added, r.statements_added, r.impacts_added, r.conflicts_resolved, trust_summary);
                            let _ = core_lib::storage::log_sync_event(&conn, &p.url, &mode, "success",
                                &format!("E{} S{} I{} C{} trustΔ{}", r.events_added, r.statements_added, r.impacts_added, r.conflicts_resolved, r.nodes_trust_changed));
                        }
                        Err(e) => {
                            println!("{} {}: {}", "❌ failed".red(), p.url, e);
                            let _ = core_lib::storage::log_sync_event(&conn, &p.url, &mode, "error", &e.to_string());
                        }
                    }
                }
            }
            #[cfg(not(feature = "p2p-client-sync"))]
            {
                // Без фичи доступны только dry-run (обработан выше) и сообщение
                println!("Build with --features p2p-client-sync to perform network sync");
            }
        }
    }
    Ok(())
}

async fn run_logs(cmd: LogsCmd) -> anyhow::Result<()> {
    match cmd {
        LogsCmd::Show { limit, db } => {
            let conn = storage::open_db(db.to_str().unwrap())?;
            let logs = core_lib::storage::get_recent_sync_logs(&conn, limit)?;
            if logs.is_empty() {
                println!("{}", "No sync logs".yellow());
            } else {
                println!("{}", "Sync logs:".blue());
                for l in logs {
                    let ts = chrono::DateTime::<chrono::Utc>::from(std::time::UNIX_EPOCH + std::time::Duration::from_secs(l.timestamp as u64)).to_rfc3339();
                    println!("#{} {} {} {} {}", l.id, ts, l.peer_url, l.mode, match l.status.as_str() { "success" => "✅", "error" => "❌", _ => "" });
                    if !l.details.is_empty() { println!("   {}", l.details); }
                }
            }
        }
        LogsCmd::Clear { db } => {
            let conn = storage::open_db(db.to_str().unwrap())?;
            core_lib::storage::clear_sync_logs(&conn)?;
            println!("{}", "✅ Logs cleared".green());
        }
    }
    Ok(())
}

async fn run_ratings(db_path: PathBuf, recalc: bool, cmd: Option<RatingsCmd>) -> anyhow::Result<()> {
    if !std::path::Path::new(&db_path).exists() {
        println!("{}", "❌ Database not found".red());
        return Ok(());
    }
    
    let conn = storage::open_db(db_path.to_str().unwrap())?;
    
    if recalc {
        println!("{}", "🔄 Recalculating ratings...".yellow());
        storage::recalc_ratings(&conn, chrono::Utc::now().timestamp())?;
        println!("{}", "✅ Ratings recalculated".green());
    }
    
    let node_ratings = storage::load_node_ratings(&conn)?;
    let group_ratings = storage::load_group_ratings(&conn)?;
    
    match cmd {
        Some(RatingsCmd::Trust { verbose }) => {
            // Локальный доверительный уровень (средний по узлам)
            let avg_local: f64 = if node_ratings.is_empty() {
                0.0
            } else {
                node_ratings.iter().map(|n| n.trust_score as f64).sum::<f64>() / node_ratings.len() as f64
            };
            // Средняя сеть — по глобальной группе
            let avg_network: f64 = group_ratings
                .iter()
                .find(|g| g.group_id == "global")
                .map(|g| g.avg_score as f64)
                .unwrap_or_else(|| {
                    if node_ratings.is_empty() { 0.0 } else { avg_local }
                });

            // Дельты доверия из последнего события синхронизации, если оно записано
            let recent_logs = core_lib::storage::get_recent_sync_logs(&conn, 1)?;
            let mut trust_deltas: Vec<(String, f32)> = Vec::new();
            if let Some(last) = recent_logs.first() {
                if last.details.contains("trust propagation") {
                    // В этой версии детали не несут список дельт; покажем топ-3 изменения относительно медианы
                    let mut sorted = node_ratings.clone();
                    sorted.sort_by(|a, b| b.trust_score.partial_cmp(&a.trust_score).unwrap_or(std::cmp::Ordering::Equal));
                    for nr in sorted.into_iter().take(3) {
                        trust_deltas.push((nr.node_id, nr.trust_score));
                    }
                }
            }

            println!("{} {:.3}", "Local trust:".blue(), avg_local);
            println!("{} {:.3}", "Network trust:".blue(), avg_network);
            if verbose {
                println!("{}", "Trust samples:".blue());
                for (id, sc) in trust_deltas {
                    let sign = if sc > 0.0 { "🟢 +" } else if sc < 0.0 { "🔴 –" } else { "⚪ =" };
                    println!("  {} {:<8} {:.3}", sign, &id.get(0..8).unwrap_or(""), sc);
                }
            }
        }
        None => {
            println!("{}", format!("Node Ratings: {}", node_ratings.len()).blue());
            for rating in &node_ratings {
                println!("  {}: {:.3}", rating.node_id, rating.trust_score);
            }
            println!("{}", format!("Group Ratings: {}", group_ratings.len()).blue());
            for rating in &group_ratings {
                println!("  {}: {:.3} (coherence: {:.3})", rating.group_id, rating.avg_score, rating.coherence);
            }
        }
    }
    
    Ok(())
}

#[cfg(feature = "p2p-client-sync")]
async fn run_sync(peer: String, identity_path: PathBuf, db_path: PathBuf, mode: Mode) -> anyhow::Result<()> {
    use truth_core::p2p::encryption::CryptoIdentity;
    use truth_core::p2p::sync::{bidirectional_sync_with_peer, incremental_sync_with_peer, push_local_data, pull_remote_data};
    use rusqlite::Connection;

    // Если указан файл ключа — используем его, иначе берём первый ключ из локального хранилища
    let identity = if identity_path.as_os_str().is_empty() {
        let store = load_keys()?;
        let first = store.keys.first().ok_or_else(|| anyhow::anyhow!("No keys found. Import via: truthctl keys import <priv_hex> <pub_hex>"))?;
        CryptoIdentity::from_keypair_hex(&first.private_key_hex, &first.public_key_hex)
            .map_err(|e| anyhow::anyhow!(e))?
    } else {
        let data = std::fs::read_to_string(&identity_path)?;
        let k: KeyFile = serde_json::from_str(&data)?;
        CryptoIdentity::from_keypair_hex(&k.private_key, &k.public_key)
            .map_err(|e| anyhow::anyhow!(e))?
    };

    let conn = Connection::open(db_path)?;

    match mode {
        Mode::Full => {
            let res = bidirectional_sync_with_peer(&peer, &identity, &conn).await?;
            print_sync_result(res);
        }
        Mode::Incremental => {
            let last = chrono::Utc::now().timestamp() - 3600; // пример: последняя שעה
            let res = incremental_sync_with_peer(&peer, &identity, &conn, last).await?;
            print_sync_result(res);
        }
        Mode::Push => {
            let res = push_local_data(&peer, &identity, &conn).await?;
            print_sync_result(res);
        }
        Mode::Pull => {
            let data = pull_remote_data(&peer, &identity).await?;
            let res = truth_core::p2p::sync::reconcile(&conn, &data)?;
            print_sync_result(res);
        }
    }
    Ok(())
}

#[cfg(not(feature = "p2p-client-sync"))]
async fn run_sync(_peer: String, _identity_path: PathBuf, _db_path: PathBuf, _mode: Mode) -> anyhow::Result<()> {
    anyhow::bail!("Build with --features p2p-client-sync to use sync commands")
}

#[allow(dead_code)]
fn print_sync_result(res: SyncResult) {
    let trust_summary = if res.nodes_trust_changed == 0 {
        "⚪ =0".to_string()
    } else {
        let gains = res.trust_diff.iter().filter(|d| d.delta > 0.0).count();
        let losses = res.trust_diff.iter().filter(|d| d.delta < 0.0).count();
        format!("🟢 +{} 🔴 –{}", gains, losses)
    };
    println!(
        "{}",
        format!(
            "✅ Sync successful:\n   - Events added: {}\n   - Statements added: {}\n   - Impacts added: {}\n   - Conflicts resolved: {}\n   - Trust changes: {}",
            res.events_added, res.statements_added, res.impacts_added, res.conflicts_resolved, trust_summary
        )
        .green()
    );
}
