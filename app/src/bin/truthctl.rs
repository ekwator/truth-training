use chrono::{TimeZone, Utc};
use clap::{Parser, Subcommand, ValueEnum};
use colored::*;
use core_lib::models::{NewNode, Node, NodeFilter, NodeSource, NodeType};
use core_lib::storage;
use reqwest::Client;
use rusqlite::OptionalExtension;
use serde::{Deserialize, Serialize};
use std::fs;
use std::path::PathBuf;
use std::str::FromStr;
use std::time::{Duration, SystemTime, UNIX_EPOCH};
use tokio::time::Instant;
use truth_core::p2p::node::{poll_global_registries, run_http_reachability_checks};

#[path = "../diagnostics.rs"]
mod diagnostics;
use diagnostics::{print_diagnostic_summary, reset_local_data, run_diagnostics, DiagnosticResult};

#[path = "../config_utils.rs"]
mod config_utils;
use config_utils::{default_config, load_config, save_config, Config as NodeConfig};

#[path = "../status_utils.rs"]
mod status_utils;
use status_utils::{get_recent_sync_events, print_status_summary, PeerItem, Peers};
use truth_core::p2p::sync::SyncResult;

#[path = "../cli.rs"]
mod cli_support;
use cli_support::{load_discovery_config, open_connection as open_node_db, open_shared_connection};

#[derive(Parser, Debug)]
#[command(
    name = "truthctl",
    version,
    about = "CLI для P2P синхронизации Truth Core"
)]
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
    /// Репозиторий узлов и ручные операции discovery
    Nodes {
        #[command(subcommand)]
        cmd: NodesCmd,
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
    /// Аутентификация на сервере и сохранение сессии
    Auth {
        /// Базовый URL сервера, напр. http://127.0.0.1:8080
        #[arg(long, default_value = "http://127.0.0.1:8080")]
        server: String,
        /// Путь к JSON-файлу ключей (private/public hex)
        #[arg(long)]
        identity: Option<PathBuf>,
    },
    /// Обновление токена по refresh
    Refresh {
        /// Базовый URL сервера
        #[arg(long, default_value = "http://127.0.0.1:8080")]
        server: String,
    },
    /// Пользователи и роли (RBAC)
    Users {
        #[command(subcommand)]
        cmd: UsersCmd,
    },
    /// Делегирование доверия
    Trust {
        #[command(subcommand)]
        cmd: TrustCmd,
    },
    /// Визуализация графа сети
    Graph {
        #[command(subcommand)]
        cmd: GraphCmd,
    },
}

#[derive(Copy, Clone, Debug, ValueEnum)]
enum Mode {
    Full,
    Incremental,
    Push,
    Pull,
}

#[derive(serde::Deserialize)]
#[allow(dead_code)] // может быть неиспользован без фичи p2p-client-sync
struct KeyFile {
    private_key: String,
    public_key: String,
}

#[derive(Subcommand, Debug)]
enum KeysCmd {
    /// Импорт пары ключей Ed25519 в локальное хранилище (~/.truthctl/keys.json)
    Import {
        private_key_hex: String,
        public_key_hex: String,
    },
    /// Список импортированных ключей
    List,
    /// Генерация новой пары ключей Ed25519 (опционально сохранить)
    Generate {
        #[arg(long)]
        save: bool,
    },
}

#[derive(Subcommand, Debug)]
enum PeersCmd {
    /// Список пиров из ~/.truthctl/peers.json
    List,
    /// Приоритеты ретрансляции и метрики
    Priorities {
        #[arg(long, default_value = "http://127.0.0.1:8080")]
        server: String,
        #[arg(long)]
        ascii_relay: bool,
    },
    /// Статистика локальной сети (успехи/ошибки, качество)
    Stats {
        #[arg(long, default_value = "http://127.0.0.1:8080")]
        server: String,
        #[arg(long, default_value = "table")]
        format: String,
    },
    /// История по пирам из локальной БД
    History {
        #[arg(long, default_value_t = 50)]
        limit: usize,
        #[arg(long, default_value = "truth.db")]
        db: PathBuf,
    },
    /// Добавить пира
    Add { url: String, public_key: String },
    /// Синхронизировать со всеми пирами
    SyncAll {
        /// Режим: full или incremental
        #[arg(long, default_value = "full")]
        mode: String,
        /// Сухой прогон без отправки
        #[arg(long)]
        dry_run: bool,
    },
}

#[derive(Subcommand, Debug)]
enum NodesCmd {
    /// Список узлов с фильтрами
    List {
        #[arg(long, default_value = "truth.db")]
        db: PathBuf,
        #[arg(long = "type", value_enum)]
        node_type: Option<NodeTypeArg>,
        #[arg(long)]
        reachable: Option<bool>,
        #[arg(long, default_value_t = 100)]
        limit: u32,
        #[arg(long, value_enum, default_value = "table")]
        format: OutputFormat,
    },
    /// Добавить или обновить узел вручную
    Add {
        #[arg(long, default_value = "truth.db")]
        db: PathBuf,
        #[arg(long)]
        address: String,
        #[arg(long = "type", value_enum)]
        node_type: NodeTypeArg,
        #[arg(long)]
        ttl: i64,
        #[arg(long, default_value_t = true)]
        reachable: bool,
        #[arg(long)]
        source: Option<String>,
        #[arg(long)]
        node_id: Option<String>,
        #[arg(long)]
        last_seen: Option<i64>,
    },
    /// Удалить запись по id или адресу
    Remove {
        #[arg(long, default_value = "truth.db")]
        db: PathBuf,
        #[arg(long)]
        id: Option<i64>,
        #[arg(long)]
        address: Option<String>,
    },
    /// Запустить discovery (локально или через сервер)
    Discover {
        #[arg(long, default_value = "truth.db")]
        db: PathBuf,
        #[arg(long = "type", value_enum)]
        types: Vec<NodeTypeArg>,
        #[arg(long)]
        server: Option<String>,
        #[arg(long = "registry")]
        registry: Vec<String>,
    },
    /// Синхронизировать список узлов с сервером
    Sync {
        #[arg(long, default_value = "truth.db")]
        db: PathBuf,
        #[arg(long, default_value = "http://127.0.0.1:8080")]
        server: String,
    },
    /// Очистить устаревшие записи (TTL/доступность)
    Cleanup {
        #[arg(long, default_value = "truth.db")]
        db: PathBuf,
    },
    /// Асинхронная проверка доступности (HTTP health check)
    HealthCheck {
        #[arg(long, default_value = "truth.db")]
        db: PathBuf,
    },
    /// Валидация схемы и статуса синхронизации (FR-010)
    Validate {
        #[arg(long, default_value = "truth.db")]
        db: PathBuf,
    },
}

#[derive(Copy, Clone, Debug, ValueEnum)]
enum NodeTypeArg {
    Lan,
    Wifi,
    Global,
    Relay,
    Client,
}

impl From<NodeTypeArg> for NodeType {
    fn from(value: NodeTypeArg) -> Self {
        match value {
            NodeTypeArg::Lan => NodeType::Lan,
            NodeTypeArg::Wifi => NodeType::Wifi,
            NodeTypeArg::Global => NodeType::Global,
            NodeTypeArg::Relay => NodeType::Relay,
            NodeTypeArg::Client => NodeType::Client,
        }
    }
}

#[derive(Copy, Clone, Debug, ValueEnum)]
enum OutputFormat {
    Table,
    Json,
}

#[derive(Subcommand, Debug)]
enum ConfigCmd {
    /// Показать текущую конфигурацию узла
    Show,
    /// Установить значение по ключу
    Set { key: String, value: String },
    /// Сбросить конфигурацию к значениям по умолчанию
    Reset {
        #[arg(long)]
        confirm: bool,
    },
}

#[derive(Subcommand, Debug)]
enum LogsCmd {
    /// Показать последние записи журнала синхронизации
    Show {
        #[arg(long, default_value_t = 50)]
        limit: usize,
        #[arg(long, default_value = "truth.db")]
        db: PathBuf,
    },
    /// Очистить журнал синхронизации
    Clear {
        #[arg(long, default_value = "truth.db")]
        db: PathBuf,
    },
}

#[derive(Subcommand, Debug)]
enum UsersCmd {
    /// Список пользователей (admin)
    List {
        #[arg(long, default_value = "http://127.0.0.1:8080")]
        server: String,
    },
    /// Назначить роль пользователю (admin)
    Grant {
        #[arg(long, default_value = "http://127.0.0.1:8080")]
        server: String,
        pubkey: String,
        role: String,
    },
    /// Отозвать роль (удалить запись пользователя)
    Revoke {
        #[arg(long, default_value = "http://127.0.0.1:8080")]
        server: String,
        pubkey: String,
    },
}

#[derive(Subcommand, Debug)]
enum TrustCmd {
    /// Делегировать доверие цели (role >= node)
    Delegate {
        #[arg(long, default_value = "http://127.0.0.1:8080")]
        server: String,
        target_pubkey: String,
        delta: f32,
    },
}

#[derive(Subcommand, Debug)]
enum GraphCmd {
    /// Показать граф сети в JSON формате
    Show {
        /// Базовый URL сервера
        #[arg(long, default_value = "http://127.0.0.1:8080")]
        server: String,
        /// Минимальный приоритет распространения (0.0-1.0)
        #[arg(long, default_value = "0.0")]
        min_priority: f32,
        /// Максимальное количество узлов
        #[arg(long, default_value = "50")]
        limit: usize,
        /// Формат вывода: json или ascii
        #[arg(long, default_value = "json")]
        format: String,
    },
}

#[derive(Subcommand, Debug)]
enum RatingsCmd {
    /// Показать доверие: локальный уровень, средняя сеть, дельты
    Trust {
        #[arg(long)]
        verbose: bool,
    },
}

// duplicated enums removed

#[tokio::main(flavor = "multi_thread")]
async fn main() -> anyhow::Result<()> {
    let cli = Cli::parse();
    match cli.command {
        Commands::Sync {
            peer,
            identity,
            db,
            mode,
        } => {
            let peer = peer.unwrap_or_else(|| {
                // Если не указан --peer, пытаемся взять первого из peers.json
                let mut url = String::new();
                if let Ok(p) = load_peers() {
                    if let Some(first) = p.peers.first() {
                        url = first.url.clone();
                    }
                }
                if url.is_empty() {
                    eprintln!(
                        "{}",
                        "No peer specified. Use --peer or add peers.json".red()
                    );
                }
                url
            });
            let identity_path = identity.unwrap_or_else(|| PathBuf::from(""));
            run_sync(peer, identity_path, db, mode).await
        }
        Commands::Status { db, identity } => run_status(db, identity).await,
        Commands::Verify { db } => run_verify(db).await,
        Commands::Ratings { db, recalc, cmd } => run_ratings(db, recalc, cmd).await,
        Commands::Keys { cmd } => run_keys(cmd).await,
        Commands::InitNode {
            node_name,
            port,
            db,
            auto_peer,
        } => run_init_node(node_name, port, db, auto_peer).await,
        Commands::Peers { cmd } => run_peers(cmd).await,
        Commands::Nodes { cmd } => run_nodes(cmd).await,
        Commands::Logs { cmd } => run_logs(cmd).await,
        Commands::Diagnose { verbose, server } => {
            if server {
                let cfg = load_config().unwrap_or_else(|_| default_config());
                let base_url = format!("http://127.0.0.1:{}", cfg.port);
                let results = truth_core::server_diagnostics::run_diagnostics(
                    &base_url,
                    &cfg.db_path,
                    cfg.p2p_enabled,
                )
                .await;
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
                println!(
                    "{}\n{}",
                    "Verbose JSON:".blue(),
                    serde_json::to_string_pretty(&value).unwrap_or_else(|_| "{}".to_string())
                );
            }
            Ok(())
        }
        Commands::ResetData { confirm, reinit } => {
            reset_local_data(confirm, reinit)?;
            Ok(())
        }
        Commands::Users { cmd } => run_users(cmd).await,
        Commands::Trust { cmd } => run_trust(cmd).await,
        Commands::Graph { cmd } => run_graph(cmd).await,
        Commands::Config { cmd } => run_config(cmd).await,
        Commands::Auth { server, identity } => auth_flow(server, identity).await,
        Commands::Refresh { server } => refresh_flow(server).await, // duplicate unreachable arms removed
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

    // 4.5) Показать метрики сети, если доступна БД
    if let Ok(conn) = storage::open_db(db_path.to_str().unwrap_or("truth.db")) {
        if let Ok(node_ratings) = core_lib::storage::load_node_ratings(&conn) {
            if !node_ratings.is_empty() {
                let avg_priority: f32 = node_ratings
                    .iter()
                    .map(|r| r.propagation_priority)
                    .sum::<f32>()
                    / node_ratings.len() as f32;

                let high_priority_count = node_ratings
                    .iter()
                    .filter(|r| r.propagation_priority > 0.7)
                    .count();

                println!("{}", "\nNetwork Health:".blue());
                let avg_prio_color = if avg_priority > 0.7 {
                    "🔵"
                } else if avg_priority > 0.3 {
                    "🟡"
                } else {
                    "🔴"
                };
                println!(
                    "  Avg Priority: {:.2} {} | High Priority Nodes: {}",
                    avg_priority, avg_prio_color, high_priority_count
                );

                // Загружаем метрики ретрансляции
                if let Ok(node_metrics) = core_lib::storage::load_all_node_metrics(&conn) {
                    if !node_metrics.is_empty() {
                        let avg_relay_rate: f32 = node_metrics
                            .iter()
                            .map(|m| m.relay_success_rate)
                            .sum::<f32>()
                            / node_metrics.len() as f32;
                        let avg_quality: f32 =
                            node_metrics.iter().map(|m| m.quality_index).sum::<f32>()
                                / node_metrics.len() as f32;

                        let relay_color = if avg_relay_rate > 0.8 {
                            "🟢"
                        } else if avg_relay_rate > 0.5 {
                            "🟡"
                        } else {
                            "🔴"
                        };
                        let quality_color = if avg_quality > 0.8 {
                            "🔵"
                        } else if avg_quality > 0.5 {
                            "🟡"
                        } else {
                            "🔴"
                        };
                        println!(
                            "  Avg Relay Rate: {:.1}% {}",
                            avg_relay_rate * 100.0,
                            relay_color
                        );
                        println!("  Avg Quality: {:.2} {}", avg_quality, quality_color);
                    }
                }

                // Показать топ-3 узла по приоритету
                let mut top_nodes: Vec<_> = node_ratings.iter().collect();
                top_nodes.sort_by(|a, b| {
                    b.propagation_priority
                        .partial_cmp(&a.propagation_priority)
                        .unwrap_or(std::cmp::Ordering::Equal)
                });

                for (i, node) in top_nodes.iter().take(3).enumerate() {
                    let short_id = if node.node_id.len() > 8 {
                        &node.node_id[0..8]
                    } else {
                        &node.node_id
                    };
                    let priority_color = if node.propagation_priority > 0.7 {
                        "🔵"
                    } else if node.propagation_priority > 0.3 {
                        "🟡"
                    } else {
                        "🔴"
                    };

                    // Получаем метрики ретрансляции для этого узла
                    let (relay_rate, quality_index) =
                        match core_lib::storage::load_node_metrics(&conn, &node.node_id) {
                            Ok(Some(m)) => (m.relay_success_rate, m.quality_index),
                            _ => (0.0, 0.0),
                        };

                    let relay_color = if relay_rate > 0.8 {
                        "🟢"
                    } else if relay_rate > 0.5 {
                        "🟡"
                    } else {
                        "🔴"
                    };
                    let quality_color = if quality_index > 0.8 {
                        "🔵"
                    } else if quality_index > 0.5 {
                        "🟡"
                    } else {
                        "🔴"
                    };
                    println!("  {}. {} {} (priority: {:.2}, trust: {:.2}, relay: {:.1}% {}, quality: {:.2} {})", 
                             i + 1, short_id, priority_color, node.propagation_priority, node.trust_score, relay_rate * 100.0, relay_color, quality_index, quality_color);
                }
            }
        }
    }

    // 5) Дополнительно: показать публичный ключ, если указан identity
    if let Some(_identity_path) = identity_path {
        #[cfg(feature = "p2p-client-sync")]
        {
            let data = std::fs::read_to_string(&_identity_path)?;
            let k: KeyFile = serde_json::from_str(&data)?;
            let id = truth_core::p2p::encryption::CryptoIdentity::from_keypair_hex(
                &k.private_key,
                &k.public_key,
            )
            .map_err(|e: String| anyhow::anyhow!(e))?;
            println!("{} {}", "Identity:".blue(), id.public_key_hex());
        }
        #[cfg(not(feature = "p2p-client-sync"))]
        {
            let _ = &_identity_path; // подавить предупреждение об неиспользуемой переменной
            println!(
                "{}",
                "Identity display requires p2p-client-sync feature".yellow()
            );
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
                .is_ok()
            {
                valid_signatures += 1;
            }
        }
    }

    println!(
        "{}",
        format!(
            "✅ Verified {}/{} signed events",
            valid_signatures, total_signed
        )
        .green()
    );
    // Используем первый ключ (если есть) для демонстрации from_keypair_hex и снятия предупреждений
    #[cfg(feature = "p2p-client-sync")]
    {
        if let Ok(store) = load_keys() {
            if let Some(k) = store.keys.first() {
                let _id = truth_core::p2p::encryption::CryptoIdentity::from_keypair_hex(
                    &k.private_key_hex,
                    &k.public_key_hex,
                )
                .map_err(|e: String| anyhow::anyhow!(e))?;
            }
        }
    }
    Ok(())
}

#[derive(Debug, Clone, Serialize, Deserialize)]
struct KeyPair {
    id: u64,
    private_key_hex: String,
    public_key_hex: String,
    created_at: String,
}

#[derive(Debug, Clone, Serialize, Deserialize, Default)]
struct KeyStore {
    keys: Vec<KeyPair>,
}

fn keystore_path() -> anyhow::Result<PathBuf> {
    let dir = dirs::home_dir()
        .ok_or_else(|| anyhow::anyhow!("no HOME"))?
        .join(".truthctl");
    fs::create_dir_all(&dir)?;
    Ok(dir.join("keys.json"))
}

fn session_path() -> anyhow::Result<PathBuf> {
    let dir = dirs::home_dir()
        .ok_or_else(|| anyhow::anyhow!("no HOME"))?
        .join(".truthctl");
    fs::create_dir_all(&dir)?;
    Ok(dir.join("session.json"))
}

#[derive(Debug, Clone, Serialize, Deserialize, Default)]
struct Session {
    access_token: String,
    refresh_token: String,
    expires_at: i64,
}

async fn auth_flow(server: String, identity_path: Option<PathBuf>) -> anyhow::Result<()> {
    use reqwest::Client;
    use truth_core::p2p::encryption::CryptoIdentity;
    let identity = if let Some(p) = identity_path {
        let data = fs::read_to_string(&p)?;
        let k: crate::KeyFile = serde_json::from_str(&data)?;
        CryptoIdentity::from_keypair_hex(&k.private_key, &k.public_key)
            .map_err(|e| anyhow::anyhow!(e))?
    } else {
        // взять первый ключ из локального keystore
        let ks = load_keys().unwrap_or_default();
        let k = ks.keys.first().ok_or_else(|| {
            anyhow::anyhow!(
                "No keys found. Use 'truthctl keys generate --save' or 'truthctl keys import'"
            )
        })?;
        CryptoIdentity::from_keypair_hex(&k.private_key_hex, &k.public_key_hex)
            .map_err(|e| anyhow::anyhow!(e))?
    };
    let ts = chrono::Utc::now().timestamp().to_string();
    let message = format!("auth:{}", ts);
    let sig = identity.sign(message.as_bytes());
    let signature_hex = hex::encode(sig.to_bytes());
    let public_key_hex = identity.public_key_hex();
    let client = Client::new();
    let resp = client
        .post(format!("{}/api/v1/auth", server))
        .header("X-Public-Key", public_key_hex)
        .header("X-Signature", signature_hex)
        .header("X-Timestamp", ts)
        .send()
        .await?;
    if !resp.status().is_success() {
        anyhow::bail!(format!("Auth failed: {}", resp.status()));
    }
    let v: serde_json::Value = resp.json().await?;
    let access = v
        .get("access_token")
        .and_then(|x| x.as_str())
        .unwrap_or_default()
        .to_string();
    let refresh = v
        .get("refresh_token")
        .and_then(|x| x.as_str())
        .unwrap_or_default()
        .to_string();
    let expires_in = v.get("expires_in").and_then(|x| x.as_i64()).unwrap_or(3600);
    let session = Session {
        access_token: access,
        refresh_token: refresh,
        expires_at: chrono::Utc::now().timestamp() + expires_in,
    };
    fs::write(session_path()?, serde_json::to_string_pretty(&session)?)?;
    println!("{}", "✅ Authenticated and session stored".green());
    Ok(())
}

async fn refresh_flow(server: String) -> anyhow::Result<()> {
    use reqwest::Client;
    let path = session_path()?;
    if !path.exists() {
        anyhow::bail!("No session. Run 'truthctl auth' first");
    }
    let data = fs::read_to_string(&path)?;
    let mut s: Session = serde_json::from_str(&data).unwrap_or_default();
    // если токен ещё жив, выходим молча
    if chrono::Utc::now().timestamp() < s.expires_at - 30 {
        // 30с запас
        println!("{}", "Token still valid".yellow());
        return Ok(());
    }
    let client = Client::new();
    let resp = client
        .post(format!("{}/api/v1/refresh", server))
        .json(&serde_json::json!({"refresh_token": s.refresh_token}))
        .send()
        .await?;
    if !resp.status().is_success() {
        anyhow::bail!(format!("Refresh failed: {}", resp.status()));
    }
    let v: serde_json::Value = resp.json().await?;
    s.access_token = v
        .get("access_token")
        .and_then(|x| x.as_str())
        .unwrap_or_default()
        .to_string();
    s.refresh_token = v
        .get("refresh_token")
        .and_then(|x| x.as_str())
        .unwrap_or_default()
        .to_string();
    let expires_in = v.get("expires_in").and_then(|x| x.as_i64()).unwrap_or(3600);
    s.expires_at = chrono::Utc::now().timestamp() + expires_in;
    fs::write(path, serde_json::to_string_pretty(&s)?)?;
    println!("{}", "✅ Token refreshed".green());
    Ok(())
}

fn bearer_or_err() -> anyhow::Result<String> {
    let path = session_path()?;
    if !path.exists() {
        anyhow::bail!("No session. Run 'truthctl auth' first");
    }
    let data = fs::read_to_string(&path)?;
    let s: Session = serde_json::from_str(&data).unwrap_or_default();
    Ok(format!("Bearer {}", s.access_token))
}

// duplicate implementations removed; canonical versions defined earlier

fn peers_path() -> anyhow::Result<PathBuf> {
    let dir = dirs::home_dir()
        .ok_or_else(|| anyhow::anyhow!("no HOME"))?
        .join(".truthctl");
    fs::create_dir_all(&dir)?;
    Ok(dir.join("peers.json"))
}

fn load_peers() -> anyhow::Result<Peers> {
    let path = peers_path()?;
    if !path.exists() {
        return Ok(Peers::default());
    }
    let data = fs::read_to_string(path)?;
    Ok(serde_json::from_str(&data).unwrap_or_default())
}

fn load_keys() -> anyhow::Result<KeyStore> {
    let path = keystore_path()?;
    if !path.exists() {
        return Ok(KeyStore::default());
    }
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
        KeysCmd::Import {
            private_key_hex,
            public_key_hex,
        } => {
            #[cfg(feature = "p2p-client-sync")]
            {
                // валидация ключей
                truth_core::p2p::encryption::CryptoIdentity::from_keypair_hex(
                    &private_key_hex,
                    &public_key_hex,
                )
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
            let created_at = chrono::DateTime::<chrono::Utc>::from(
                UNIX_EPOCH + std::time::Duration::from_secs(ts),
            )
            .to_rfc3339();
            store.keys.push(KeyPair {
                id: next_id,
                private_key_hex,
                public_key_hex,
                created_at,
            });
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
                let created_at = chrono::DateTime::<chrono::Utc>::from(
                    UNIX_EPOCH + std::time::Duration::from_secs(ts),
                )
                .to_rfc3339();
                store.keys.push(KeyPair {
                    id: next_id,
                    private_key_hex: priv_hex,
                    public_key_hex: pub_hex,
                    created_at,
                });
                save_keys(&store)?;
                println!("{}", "✅ Key saved to ~/.truthctl/keys.json".green());
            }
        }
    }
    Ok(())
}

// Типы Peers/PeerItem берём из status_utils

async fn run_init_node(
    node_name: String,
    port: u16,
    db: PathBuf,
    auto_peer: bool,
) -> anyhow::Result<()> {
    // get a key
    let ks = load_keys()?;
    let first = ks.keys.first().ok_or_else(|| {
        anyhow::anyhow!("No keys found. Generate or import one: truthctl keys generate --save")
    })?;
    let cfg = NodeConfig {
        node_name,
        port,
        db_path: db.display().to_string(),
        public_key: first.public_key_hex.clone(),
        private_key: first.private_key_hex.clone(),
        auto_peer,
        p2p_enabled: true,
        node_registries: Vec::new(),
    };
    save_config(&cfg)?;
    // find path for user output
    let dir = dirs::home_dir()
        .ok_or_else(|| anyhow::anyhow!("no HOME"))?
        .join(".truthctl");
    let cfg_path = dir.join("config.json");
    println!(
        "{} {}",
        "✅ Node config written:".green(),
        cfg_path.display()
    );

    if auto_peer {
        let mut peers: Peers = if peers_path()?.exists() {
            let data = fs::read_to_string(peers_path()?)?;
            serde_json::from_str(&data).unwrap_or_default()
        } else {
            Peers::default()
        };
        let url = format!("http://127.0.0.1:{}", port);
        let me = PeerItem {
            url,
            public_key: cfg.public_key.clone(),
        };
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
                    let p: u16 = value
                        .parse()
                        .map_err(|_| anyhow::anyhow!("port must be a number (u16)"))?;
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
                println!(
                    "{}",
                    "This will overwrite ~/.truthctl/config.json. Rerun with --confirm to proceed."
                        .yellow()
                );
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

async fn run_nodes(cmd: NodesCmd) -> anyhow::Result<()> {
    match cmd {
        NodesCmd::List {
            db,
            node_type,
            reachable,
            limit,
            format,
        } => {
            let conn = open_node_db(&db)?;
            let filter = NodeFilter {
                node_type: node_type.map(Into::into),
                reachable,
                limit: Some(limit as i64),
                address: None,
            };
            let nodes = storage::list_nodes(&conn, &filter)?;
            match format {
                OutputFormat::Table => print_nodes_table(&nodes),
                OutputFormat::Json => {
                    println!("{}", serde_json::to_string_pretty(&nodes)?);
                }
            }
        }
        NodesCmd::Add {
            db,
            address,
            node_type,
            ttl,
            reachable,
            source,
            node_id,
            last_seen,
        } => {
            let conn = open_node_db(&db)?;
            let node_type: NodeType = node_type.into();
            if ttl < node_type.min_ttl_secs() {
                anyhow::bail!(
                    "ttl must be >= {} for {}",
                    node_type.min_ttl_secs(),
                    node_type
                );
            }
            let parsed_source = match source {
                Some(label) => Some(parse_node_source(&label)?),
                None => Some(default_source_for(node_type)),
            };
            let ts = last_seen.unwrap_or_else(|| Utc::now().timestamp());
            let new_node = NewNode {
                address,
                node_type,
                reachable,
                last_seen: ts,
                ttl,
                source: parsed_source,
                node_id,
                created_at: ts,
                updated_at: ts,
            };
            let node = storage::insert_node(&conn, new_node)?;
            println!(
                "{}",
                format!("Stored node {} with id {}", node.address, node.id).green()
            );
        }
        NodesCmd::Remove { db, id, address } => {
            if id.is_none() && address.is_none() {
                anyhow::bail!("Specify either --id or --address");
            }
            let conn = open_node_db(&db)?;
            let target_id = if let Some(id) = id {
                id
            } else {
                let addr = address.clone().unwrap();
                let node = storage::get_node_by_address(&conn, &addr)?;
                node.ok_or_else(|| anyhow::anyhow!("Node with address {addr} not found"))?
                    .id
            };
            let removed = storage::delete_node(&conn, target_id)?;
            if removed {
                println!("{}", format!("Removed node {target_id}").green());
            } else {
                println!("{}", format!("Node {target_id} not found").yellow());
            }
        }
        NodesCmd::Discover {
            db,
            types,
            server,
            registry,
        } => {
            log::info!("discovery.command.discover started");
            if let Some(server_url) = server {
                run_remote_discover(&server_url, &types).await?;
                return Ok(());
            }
            let shared = open_shared_connection(&db)?;
            let requested = desired_types(types);
            let need_local = requested
                .iter()
                .any(|t| matches!(t, NodeType::Lan | NodeType::Wifi | NodeType::Client));
            let need_global = requested
                .iter()
                .any(|t| matches!(t, NodeType::Global | NodeType::Relay));
            let start = Instant::now();
            let mut updated = 0usize;
            if need_local {
                updated = run_http_reachability_checks(
                    shared.clone(),
                    Duration::from_secs(core_lib::HEALTH_CHECK_TIMEOUT_SECS),
                    core_lib::HEALTH_CHECK_RETRY_LIMIT,
                )
                .await?;
            }
            let mut discovered = 0usize;
            if need_global {
                let discovery_cfg = load_discovery_config(&registry);
                let node_cfg = discovery_cfg.into_node_config();
                if node_cfg.global_registry_urls.is_empty() {
                    println!(
                        "{}",
                        "No registry URLs configured; skipping global discovery.".yellow()
                    );
                } else {
                    discovered = poll_global_registries(&node_cfg, shared.clone()).await?;
                }
            }
            let duration_ms = start.elapsed().as_millis() as u64;
            println!(
                "{}",
                format!(
                    "Discovery completed in {} ms (new {}, updated {})",
                    duration_ms, discovered, updated
                )
                .green()
            );
            log::info!(
                "discovery.command.discover.completed discovered={} updated={} duration_ms={}",
                discovered,
                updated,
                duration_ms
            );
        }
        NodesCmd::Sync { db, server } => {
            log::info!("discovery.command.sync started");
            let conn = open_node_db(&db)?;
            let nodes = storage::list_nodes(&conn, &NodeFilter::default())?;
            if nodes.is_empty() {
                println!("{}", "No nodes available for sync".yellow());
                return Ok(());
            }
            let client = Client::builder().timeout(Duration::from_secs(20)).build()?;
            let base = normalize_base_url(&server);
            let resp = client
                .post(format!("{}/api/v1/nodes/sync", base))
                .json(&serde_json::json!({ "nodes": nodes }))
                .send()
                .await?;
            if !resp.status().is_success() {
                anyhow::bail!("Server responded {}", resp.status());
            }
            let payload: ApiSyncResponse = resp.json().await?;
            for node in &payload.merged {
                storage::upsert_node_by_address(&conn, node)?;
            }
            println!(
                "{}",
                format!(
                    "Merged {} nodes (added {}, updated {})",
                    payload.merged.len(),
                    payload.local_added,
                    payload.local_updated
                )
                .green()
            );
            log::info!(
                "discovery.command.sync.completed merged={} added={} updated={}",
                payload.merged.len(),
                payload.local_added,
                payload.local_updated
            );
        }
        NodesCmd::Cleanup { db } => {
            log::info!("discovery.command.cleanup started");
            let conn = open_node_db(&db)?;
            let removed = storage::prune_stale_nodes(&conn, Utc::now().timestamp())?;
            println!("{}", format!("Removed {removed} stale nodes").green());
            log::info!(
                "discovery.command.cleanup.completed pruned={} expired={} unreachable=0",
                removed,
                removed
            );
        }
        NodesCmd::HealthCheck { db } => {
            log::info!("discovery.command.health_check started");
            let shared = open_shared_connection(&db)?;
            let checked = run_http_reachability_checks(
                shared,
                Duration::from_secs(core_lib::HEALTH_CHECK_TIMEOUT_SECS),
                core_lib::HEALTH_CHECK_RETRY_LIMIT,
            )
            .await?;
            println!("{}", format!("Checked {checked} node(s)").green());
            log::info!(
                "discovery.command.health_check.completed checked={}",
                checked
            );
        }
        NodesCmd::Validate { db } => {
            let conn = open_node_db(&db)?;
            let gaps = nodes_schema_gaps(&conn)?;
            if gaps.is_empty() {
                println!("{}", "Schema parity: OK".green());
            } else {
                println!("{}", format!("Missing columns: {}", gaps.join(", ")).red());
            }
            let version = conn
                .query_row(
                    "SELECT version, applied_at FROM schema_version ORDER BY applied_at DESC LIMIT 1",
                    [],
                    |row| Ok((row.get::<_, String>(0)?, row.get::<_, i64>(1)?)),
                )
                .optional()?;
            if let Some((ver, ts)) = version {
                println!("Latest migration: {} at {}", ver, fmt_timestamp(ts));
            } else {
                println!("{}", "No schema_version records found".yellow());
            }
            let total: i64 = conn.query_row("SELECT COUNT(*) FROM nodes", [], |r| r.get(0))?;
            let reachable: i64 = conn
                .query_row("SELECT COUNT(*) FROM nodes WHERE reachable = 1", [], |r| {
                    r.get(0)
                })
                .unwrap_or(0);
            println!(
                "Discovery status: {} nodes ({} reachable)",
                total, reachable
            );
            let mut stmt =
                conn.prepare("SELECT type, COUNT(*) FROM nodes GROUP BY type ORDER BY type")?;
            let mut rows = stmt.query([])?;
            while let Some(row) = rows.next()? {
                let t: String = row.get(0)?;
                let count: i64 = row.get(1)?;
                println!("  - {t}: {count}");
            }
            let sync_logs = storage::get_recent_sync_logs(&conn, 1)?;
            if let Some(entry) = sync_logs.first() {
                println!(
                    "Last sync: {} ({})",
                    fmt_timestamp(entry.timestamp),
                    entry.status
                );
            } else {
                println!("{}", "No sync logs recorded yet".yellow());
            }
        }
    }
    Ok(())
}

fn parse_node_source(label: &str) -> anyhow::Result<NodeSource> {
    NodeSource::from_str(label).map_err(|e| anyhow::anyhow!(e.to_string()))
}

fn default_source_for(node_type: NodeType) -> NodeSource {
    match node_type {
        NodeType::Lan => NodeSource::LocalBroadcast,
        NodeType::Wifi => NodeSource::WifiScan,
        NodeType::Global | NodeType::Relay => NodeSource::GlobalRegistry,
        NodeType::Client => NodeSource::Manual,
    }
}

fn desired_types(types: Vec<NodeTypeArg>) -> Vec<NodeType> {
    if types.is_empty() {
        vec![NodeType::Lan, NodeType::Wifi, NodeType::Global]
    } else {
        types.into_iter().map(Into::into).collect()
    }
}

fn print_nodes_table(nodes: &[Node]) {
    if nodes.is_empty() {
        println!("{}", "(no nodes)".yellow());
        return;
    }
    println!(
        "{:<5} {:<32} {:<8} {:<6} {:<10} {:<15} {:<15}",
        "ID", "Address", "Type", "Reach", "TTL", "Last Seen", "Source"
    );
    for node in nodes {
        println!(
            "{:<5} {:<32} {:<8} {:<6} {:<10} {:<15} {:<15}",
            node.id,
            node.address,
            node.node_type,
            if node.reachable { "yes" } else { "no" },
            node.ttl,
            fmt_timestamp(node.last_seen),
            node.source
                .map(|s| s.to_string())
                .unwrap_or_else(|| "-".into())
        );
    }
}

fn fmt_timestamp(ts: i64) -> String {
    Utc.timestamp_opt(ts, 0)
        .single()
        .map(|dt| dt.to_rfc3339())
        .unwrap_or_else(|| "-".into())
}

fn normalize_base_url(base: &str) -> String {
    base.trim_end_matches('/').to_string()
}

fn nodes_schema_gaps(conn: &rusqlite::Connection) -> anyhow::Result<Vec<String>> {
    let mut stmt = conn.prepare("PRAGMA table_info('nodes')")?;
    let mut rows = stmt.query([])?;
    let mut columns = Vec::new();
    while let Some(row) = rows.next()? {
        columns.push(row.get::<_, String>(1)?);
    }
    let required = [
        "id",
        "address",
        "type",
        "reachable",
        "last_seen",
        "ttl",
        "source",
        "node_id",
        "created_at",
        "updated_at",
    ];
    Ok(required
        .iter()
        .filter(|col| !columns.iter().any(|existing| existing == *col))
        .map(|s| s.to_string())
        .collect())
}

#[derive(Deserialize)]
struct ApiSyncResponse {
    merged: Vec<Node>,
    local_added: usize,
    local_updated: usize,
}

async fn run_remote_discover(server: &str, types: &[NodeTypeArg]) -> anyhow::Result<()> {
    let client = Client::builder().timeout(Duration::from_secs(20)).build()?;
    let base = normalize_base_url(server);
    let mut body = serde_json::Map::new();
    if !types.is_empty() {
        body.insert(
            "types".into(),
            serde_json::Value::Array(
                types
                    .iter()
                    .map(|t| {
                        serde_json::Value::String(
                            match t {
                                NodeTypeArg::Lan => "LAN",
                                NodeTypeArg::Wifi => "WIFI",
                                NodeTypeArg::Global => "GLOBAL",
                                NodeTypeArg::Relay => "RELAY",
                                NodeTypeArg::Client => "CLIENT",
                            }
                            .to_string(),
                        )
                    })
                    .collect(),
            ),
        );
    }
    let resp = client
        .post(format!("{}/api/v1/nodes/discover", base))
        .json(&serde_json::Value::Object(body))
        .send()
        .await?;
    if !resp.status().is_success() {
        anyhow::bail!("Server responded {}", resp.status());
    }
    let payload: serde_json::Value = resp.json().await?;
    println!("{}", serde_json::to_string_pretty(&payload)?);
    Ok(())
}

async fn run_peers(cmd: PeersCmd) -> anyhow::Result<()> {
    match cmd {
        PeersCmd::List => {
            let peers = load_peers().unwrap_or_default();
            if peers.peers.is_empty() {
                println!("{}", "No peers found".yellow());
            } else {
                println!("{}", "Peers:".blue());
                for p in peers.peers {
                    println!("- {} ({})", p.url, &p.public_key.get(0..8).unwrap_or(""));
                }
            }
        }
        PeersCmd::Priorities {
            server,
            ascii_relay,
        } => {
            use reqwest::Client;
            let client = Client::new();
            let resp = client
                .get(format!("{}/api/v1/peers/priorities", server))
                .send()
                .await?;
            if !resp.status().is_success() {
                anyhow::bail!(format!("HTTP {}", resp.status()));
            }
            let v: serde_json::Value = resp.json().await?;
            // Таблица: Peer | Trust | Priority | Relays | ΔDelay
            println!(
                "{}",
                "Peer            Trust  Priority  Relays  ΔDelay".blue()
            );
            if let Some(arr) = v.as_array() {
                for it in arr {
                    let peer = it.get("peer_url").and_then(|x| x.as_str()).unwrap_or("");
                    let trust = it
                        .get("trust_score")
                        .and_then(|x| x.as_f64())
                        .unwrap_or(0.0);
                    let prio = it
                        .get("propagation_priority")
                        .and_then(|x| x.as_f64())
                        .unwrap_or(0.0);
                    let rate = it.get("relay_rate").and_then(|x| x.as_f64()).unwrap_or(0.0);
                    let delay = if prio < 0.3 {
                        1200
                    } else if prio < 0.6 {
                        600
                    } else {
                        0
                    };
                    let colorized = if prio < 0.3 {
                        format!("{:.2}", prio).red()
                    } else if prio < 0.6 {
                        format!("{:.2}", prio).yellow()
                    } else {
                        format!("{:.2}", prio).green()
                    };
                    println!(
                        "{:<15} {:>5.2}  {:>8}  {:>6.2}  {:>5}ms",
                        peer, trust, colorized, rate, delay
                    );
                }
                if ascii_relay && arr.len() >= 2 {
                    // Простейшая ASCII схема по первым трём
                    let nodes: Vec<(String, f64)> = arr
                        .iter()
                        .take(3)
                        .map(|it| {
                            (
                                it.get("peer_url")
                                    .and_then(|x| x.as_str())
                                    .unwrap_or("")
                                    .to_string(),
                                it.get("propagation_priority")
                                    .and_then(|x| x.as_f64())
                                    .unwrap_or(0.0),
                            )
                        })
                        .collect();
                    // Формат: A(0.9) ==> B(0.7) -> C(0.5)
                    let mut parts: Vec<String> = Vec::new();
                    for (i, (name, p)) in nodes.iter().enumerate() {
                        let glyph = if *p >= 0.6 {
                            "==>"
                        } else if *p >= 0.3 {
                            "->"
                        } else {
                            "-"
                        };
                        let node = format!("{}({:.1})", name, p);
                        if i == 0 {
                            parts.push(node);
                        } else {
                            parts.push(format!(" {} {}", glyph, node));
                        }
                    }
                    println!("\n{}", parts.join(""));
                }
            }
        }
        PeersCmd::Stats { server, format } => {
            use reqwest::Client;
            let client = Client::new();
            let resp = client
                .get(format!("{}/api/v1/network/local", server))
                .send()
                .await?;
            if !resp.status().is_success() {
                anyhow::bail!(format!("HTTP {}", resp.status()));
            }
            let v: serde_json::Value = resp.json().await?;
            match format.as_str() {
                "json" => {
                    println!("{}", serde_json::to_string_pretty(&v)?);
                }
                _ => {
                    println!("Peer                  Last Sync              Success  Fails  Quality  Trust");
                    println!("{}", "\u{2500}".repeat(70));
                    if let Some(arr) = v.get("peers").and_then(|x| x.as_array()) {
                        for it in arr {
                            let url = it.get("url").and_then(|x| x.as_str()).unwrap_or("");
                            let last = it.get("last_sync").and_then(|x| x.as_str()).unwrap_or("");
                            let succ = it
                                .get("success_count")
                                .and_then(|x| x.as_i64())
                                .unwrap_or(0);
                            let fail = it.get("fail_count").and_then(|x| x.as_i64()).unwrap_or(0);
                            let qual = it
                                .get("last_quality_index")
                                .and_then(|x| x.as_f64())
                                .unwrap_or(0.0);
                            let trust = it
                                .get("last_trust_score")
                                .and_then(|x| x.as_f64())
                                .unwrap_or(0.0);
                            println!(
                                "{:<22} {:<20} {:>7} {:>6} {:>7.2} {:>6.2}",
                                url, last, succ, fail, qual, trust
                            );
                        }
                    }
                    if let Some(summary) = v.get("summary") {
                        let avg_sr = summary
                            .get("avg_success_rate")
                            .and_then(|x| x.as_f64())
                            .unwrap_or(0.0);
                        let avg_q = summary
                            .get("avg_quality_index")
                            .and_then(|x| x.as_f64())
                            .unwrap_or(0.0);
                        println!("{}", "\u{2500}".repeat(70));
                        println!(
                            "Avg success rate: {:.2} | Avg quality: {:.2}",
                            avg_sr, avg_q
                        );
                    }
                }
            }
        }
        PeersCmd::History { limit, db } => {
            if !std::path::Path::new(&db).exists() {
                println!("{}", "❌ Database not found".red());
                return Ok(());
            }
            let conn = storage::open_db(db.to_str().unwrap())?;
            let hist = core_lib::storage::load_peer_history(&conn, Some(limit))?;
            if hist.is_empty() {
                println!("{}", "No peer history".yellow());
            } else {
                println!("Peer                  Last Sync        Success  Fails  Quality  Trust");
                println!("{}", "\u{2500}".repeat(70));
                for h in hist {
                    let last = h
                        .last_sync
                        .map(|ts| {
                            chrono::DateTime::<chrono::Utc>::from(
                                std::time::UNIX_EPOCH + std::time::Duration::from_secs(ts as u64),
                            )
                            .to_rfc3339()
                        })
                        .unwrap_or_else(|| "".into());
                    println!(
                        "{:<22} {:<16} {:>7} {:>6} {:>7.2} {:>6.2}",
                        h.peer_url,
                        last,
                        h.success_count,
                        h.fail_count,
                        h.last_quality_index,
                        h.last_trust_score
                    );
                }
                // Итоговая строка
                let summary = core_lib::storage::get_peer_summary(&conn)?;
                println!("{}", "\u{2500}".repeat(70));
                println!(
                    "Avg success rate: {:.2} | Avg quality: {:.2}",
                    summary.avg_success_rate, summary.avg_quality_index
                );
            }
        }
        PeersCmd::Add { url, public_key } => {
            let mut peers = load_peers().unwrap_or_default();
            if peers.peers.iter().any(|p| p.url == url) {
                println!("{}", "Peer already exists".yellow());
            } else {
                peers.peers.push(PeerItem {
                    url: url.clone(),
                    public_key,
                });
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
                        let _ = core_lib::storage::log_sync_event(
                            &conn,
                            &p.url,
                            &mode,
                            "dry-run",
                            "no network call",
                        );
                    }
                    println!(
                        "{} {}",
                        "✅ Dry-run logs recorded for".green(),
                        peers.peers.len()
                    );
                }
                return Ok(());
            }

            #[cfg(feature = "p2p-client-sync")]
            {
                use truth_core::p2p::encryption::CryptoIdentity;
                use truth_core::p2p::sync::{
                    bidirectional_sync_with_peer, incremental_sync_with_peer,
                };

                let peers = load_peers().unwrap_or_default();
                let store = load_keys()?;
                let cfg = load_config().unwrap_or_else(|_| default_config());
                let me = store
                    .keys
                    .first()
                    .ok_or_else(|| anyhow::anyhow!("No keys found"))?;
                let identity =
                    CryptoIdentity::from_keypair_hex(&me.private_key_hex, &me.public_key_hex)
                        .map_err(|e| anyhow::anyhow!(e))?;
                // Используем путь к БД из конфигурации узла
                let conn = storage::open_db(&cfg.db_path)?;
                let conn_arc = std::sync::Arc::new(tokio::sync::Mutex::new(conn));

                if peers.peers.is_empty() {
                    println!("{}", "No peers to sync".yellow());
                }
                for p in &peers.peers {
                    if p.public_key == me.public_key_hex {
                        continue;
                    } // skip self
                      // Фильтр по доверию: опционально можно пропускать пиров с низким рейтингом
                      // пока простая проверка локального знания о peer (если есть запись)
                    let skip_low_trust = false; // future: сделать флаг CLI
                    if skip_low_trust {
                        let guard = conn_arc.lock().await;
                        let nodes = core_lib::storage::load_node_ratings(&*guard)?;
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
                        incremental_sync_with_peer(&p.url, &identity, conn_arc.clone(), last).await
                    } else {
                        bidirectional_sync_with_peer(&p.url, &identity, conn_arc.clone()).await
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
                            println!(
                                "{} {}: +E{} +S{} +I{} (conflicts {}) [{}]",
                                "✅ synced".green(),
                                p.url,
                                r.events_added,
                                r.statements_added,
                                r.impacts_added,
                                r.conflicts_resolved,
                                trust_summary
                            );
                            {
                                let guard = conn_arc.lock().await;
                                let _ = core_lib::storage::log_sync_event(
                                    &*guard,
                                    &p.url,
                                    &mode,
                                    "success",
                                    &format!(
                                        "E{} S{} I{} C{} trustΔ{}",
                                        r.events_added,
                                        r.statements_added,
                                        r.impacts_added,
                                        r.conflicts_resolved,
                                        r.nodes_trust_changed
                                    ),
                                );
                            }
                        }
                        Err(e) => {
                            println!("{} {}: {}", "❌ failed".red(), p.url, e);
                            let guard = conn_arc.lock().await;
                            let _ = core_lib::storage::log_sync_event(
                                &*guard,
                                &p.url,
                                &mode,
                                "error",
                                &e.to_string(),
                            );
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
                    let ts = chrono::DateTime::<chrono::Utc>::from(
                        std::time::UNIX_EPOCH + std::time::Duration::from_secs(l.timestamp as u64),
                    )
                    .to_rfc3339();
                    println!(
                        "#{} {} {} {} {}",
                        l.id,
                        ts,
                        l.peer_url,
                        l.mode,
                        match l.status.as_str() {
                            "success" => "✅",
                            "error" => "❌",
                            _ => "",
                        }
                    );
                    if !l.details.is_empty() {
                        println!("   {}", l.details);
                    }
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

async fn run_ratings(
    db_path: PathBuf,
    recalc: bool,
    cmd: Option<RatingsCmd>,
) -> anyhow::Result<()> {
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
                node_ratings
                    .iter()
                    .map(|n| n.trust_score as f64)
                    .sum::<f64>()
                    / node_ratings.len() as f64
            };
            // Средняя сеть — по глобальной группе
            let avg_network: f64 = group_ratings
                .iter()
                .find(|g| g.group_id == "global")
                .map(|g| g.avg_score as f64)
                .unwrap_or_else(|| {
                    if node_ratings.is_empty() {
                        0.0
                    } else {
                        avg_local
                    }
                });

            // Дельты доверия из последнего события синхронизации, если оно записано
            let recent_logs = core_lib::storage::get_recent_sync_logs(&conn, 1)?;
            let mut trust_deltas: Vec<(String, f32)> = Vec::new();
            if let Some(last) = recent_logs.first() {
                if last.details.contains("trust propagation") {
                    // В этой версии детали не несут список дельт; покажем топ-3 изменения относительно медианы
                    let mut sorted = node_ratings.clone();
                    sorted.sort_by(|a, b| {
                        b.trust_score
                            .partial_cmp(&a.trust_score)
                            .unwrap_or(std::cmp::Ordering::Equal)
                    });
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
                    let sign = if sc > 0.0 {
                        "🟢 +"
                    } else if sc < 0.0 {
                        "🔴 –"
                    } else {
                        "⚪ ="
                    };
                    println!("  {} {:<8} {:.3}", sign, &id.get(0..8).unwrap_or(""), sc);
                }
            }
        }
        None => {
            println!("{}", format!("Node Ratings: {}", node_ratings.len()).blue());
            for rating in &node_ratings {
                println!("  {}: {:.3}", rating.node_id, rating.trust_score);
            }
            println!(
                "{}",
                format!("Group Ratings: {}", group_ratings.len()).blue()
            );
            for rating in &group_ratings {
                println!(
                    "  {}: {:.3} (coherence: {:.3})",
                    rating.group_id, rating.avg_score, rating.coherence
                );
            }
        }
    }

    Ok(())
}

#[cfg(feature = "p2p-client-sync")]
async fn run_sync(
    peer: String,
    identity_path: PathBuf,
    db_path: PathBuf,
    mode: Mode,
) -> anyhow::Result<()> {
    use rusqlite::Connection;
    use truth_core::p2p::encryption::CryptoIdentity;
    use truth_core::p2p::sync::{
        bidirectional_sync_with_peer, incremental_sync_with_peer, pull_remote_data, push_local_data,
    };

    // Если указан файл ключа — используем его, иначе берём первый ключ из локального хранилища
    let identity = if identity_path.as_os_str().is_empty() {
        let store = load_keys()?;
        let first = store.keys.first().ok_or_else(|| {
            anyhow::anyhow!("No keys found. Import via: truthctl keys import <priv_hex> <pub_hex>")
        })?;
        CryptoIdentity::from_keypair_hex(&first.private_key_hex, &first.public_key_hex)
            .map_err(|e| anyhow::anyhow!(e))?
    } else {
        let data = std::fs::read_to_string(&identity_path)?;
        let k: KeyFile = serde_json::from_str(&data)?;
        CryptoIdentity::from_keypair_hex(&k.private_key, &k.public_key)
            .map_err(|e| anyhow::anyhow!(e))?
    };

    let conn = Connection::open(db_path)?;
    let conn_arc = std::sync::Arc::new(tokio::sync::Mutex::new(conn));

    match mode {
        Mode::Full => {
            let res = bidirectional_sync_with_peer(&peer, &identity, conn_arc.clone()).await?;
            print_sync_result(res);
        }
        Mode::Incremental => {
            let last = chrono::Utc::now().timestamp() - 3600; // пример: последняя שעה
            let res = incremental_sync_with_peer(&peer, &identity, conn_arc.clone(), last).await?;
            print_sync_result(res);
        }
        Mode::Push => {
            let guard = conn_arc.lock().await;
            let res = push_local_data(&peer, &identity, &*guard).await?;
            print_sync_result(res);
        }
        Mode::Pull => {
            let data = pull_remote_data(&peer, &identity).await?;
            let guard = conn_arc.lock().await;
            let res = truth_core::p2p::sync::reconcile(&*guard, &data)?;
            print_sync_result(res);
        }
    }
    Ok(())
}

#[cfg(not(feature = "p2p-client-sync"))]
async fn run_sync(
    _peer: String,
    _identity_path: PathBuf,
    _db_path: PathBuf,
    _mode: Mode,
) -> anyhow::Result<()> {
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

async fn run_users(cmd: UsersCmd) -> anyhow::Result<()> {
    use reqwest::Client;
    let client = Client::new();
    match cmd {
        UsersCmd::List { server } => {
            let auth = bearer_or_err()?;
            let resp = client
                .get(format!("{}/api/v1/users", server))
                .header("Authorization", auth)
                .send()
                .await?;
            if resp.status().is_success() {
                let v: serde_json::Value = resp.json().await?;
                println!("{}", "✅ Users".green());
                println!("{}", serde_json::to_string_pretty(&v)?);
            } else {
                println!("{} {}", "❌ Access denied".red(), resp.status());
            }
        }
        UsersCmd::Grant {
            server,
            pubkey,
            role,
        } => {
            let auth = bearer_or_err()?;
            let resp = client
                .post(format!("{}/api/v1/users/role", server))
                .header("Authorization", auth)
                .json(&serde_json::json!({"pubkey": pubkey, "role": role}))
                .send()
                .await?;
            if resp.status().is_success() {
                println!("{}", "✅ Role updated".green());
            } else {
                println!("{} {}", "❌ Denied".red(), resp.status());
            }
        }
        UsersCmd::Revoke { server, pubkey } => {
            let auth = bearer_or_err()?;
            let resp = client
                .post(format!("{}/api/v1/users/role", server))
                .header("Authorization", auth)
                .json(&serde_json::json!({"pubkey": pubkey, "role": "observer"}))
                .send()
                .await?;
            if resp.status().is_success() {
                println!("{}", "✅ Role revoked".green());
            } else {
                println!("{} {}", "❌ Denied".red(), resp.status());
            }
        }
    }
    Ok(())
}

async fn run_trust(cmd: TrustCmd) -> anyhow::Result<()> {
    use reqwest::Client;
    let client = Client::new();
    match cmd {
        TrustCmd::Delegate {
            server,
            target_pubkey,
            delta,
        } => {
            let auth = bearer_or_err()?;
            let resp = client
                .post(format!("{}/api/v1/trust/delegate", server))
                .header("Authorization", auth)
                .json(&serde_json::json!({"target_pubkey": target_pubkey, "delta": delta}))
                .send()
                .await?;
            if resp.status().is_success() {
                println!("{}", "✅ Delegation applied".green());
            } else {
                println!("{} {}", "❌ Denied".red(), resp.status());
            }
        }
    }
    Ok(())
}

async fn run_graph(cmd: GraphCmd) -> anyhow::Result<()> {
    use reqwest::Client;
    let client = Client::new();
    match cmd {
        GraphCmd::Show {
            server,
            min_priority,
            limit,
            format,
        } => {
            let url = format!(
                "{}/graph/json?min_priority={}&limit={}",
                server, min_priority, limit
            );
            let resp = client.get(&url).send().await?;

            if !resp.status().is_success() {
                anyhow::bail!("HTTP {}: {}", resp.status(), resp.text().await?);
            }

            let graph: serde_json::Value = resp.json().await?;

            match format.as_str() {
                "json" => {
                    println!("{}", serde_json::to_string_pretty(&graph)?);
                }
                "ascii" => {
                    render_ascii_graph(&graph)?;
                }
                _ => {
                    anyhow::bail!("Unsupported format: {}. Use 'json' or 'ascii'", format);
                }
            }
        }
    }
    Ok(())
}

fn render_ascii_graph(graph: &serde_json::Value) -> anyhow::Result<()> {
    let empty_vec = vec![];
    let nodes = graph
        .get("nodes")
        .and_then(|n| n.as_array())
        .unwrap_or(&empty_vec);
    let links = graph
        .get("links")
        .and_then(|l| l.as_array())
        .unwrap_or(&empty_vec);

    if nodes.is_empty() {
        println!("{}", "No nodes found".yellow());
        return Ok(());
    }

    println!("{}", "Network Graph:".blue());
    println!("{}", "=".repeat(50));

    // Показываем топ-3 узла с их метриками
    let mut sorted_nodes: Vec<_> = nodes.iter().collect();
    sorted_nodes.sort_by(|a, b| {
        let score_a = a.get("score").and_then(|s| s.as_f64()).unwrap_or(0.0);
        let score_b = b.get("score").and_then(|s| s.as_f64()).unwrap_or(0.0);
        score_b
            .partial_cmp(&score_a)
            .unwrap_or(std::cmp::Ordering::Equal)
    });

    for (i, node) in sorted_nodes.iter().take(3).enumerate() {
        let id = node.get("id").and_then(|i| i.as_str()).unwrap_or("unknown");
        let score = node.get("score").and_then(|s| s.as_f64()).unwrap_or(0.0);
        let priority = node
            .get("propagation_priority")
            .and_then(|p| p.as_f64())
            .unwrap_or(0.0);
        let relay_rate = node
            .get("relay_success_rate")
            .and_then(|r| r.as_f64())
            .unwrap_or(0.0);
        let quality = node
            .get("quality_index")
            .and_then(|q| q.as_f64())
            .unwrap_or(0.0);

        let short_id = if id.len() > 8 { &id[0..8] } else { id };
        let score_color = if score > 0.5 {
            "🟢"
        } else if score > 0.0 {
            "🟡"
        } else {
            "🔴"
        };
        let priority_color = if priority > 0.7 {
            "🔵"
        } else if priority > 0.3 {
            "🟡"
        } else {
            "🔴"
        };
        let relay_color = if relay_rate > 0.8 {
            "🟢"
        } else if relay_rate > 0.5 {
            "🟡"
        } else {
            "🔴"
        };
        let quality_color = if quality > 0.8 {
            "🔵"
        } else if quality > 0.5 {
            "🟡"
        } else {
            "🔴"
        };

        println!(
            "{}. {} {} {} {} (trust: {:.2}, priority: {:.2}, relay: {:.1}% {}, quality: {:.2} {})",
            i + 1,
            short_id,
            score_color,
            priority_color,
            relay_color,
            score,
            priority,
            relay_rate * 100.0,
            relay_color,
            quality,
            quality_color
        );
    }

    // Показываем связи в ASCII формате
    if !links.is_empty() {
        println!("\n{}", "Connections:".blue());
        for link in links.iter().take(5) {
            let source = link
                .get("source")
                .and_then(|s| s.as_str())
                .unwrap_or("unknown");
            let target = link
                .get("target")
                .and_then(|t| t.as_str())
                .unwrap_or("unknown");
            let weight = link.get("weight").and_then(|w| w.as_f64()).unwrap_or(0.0);
            let latency = link.get("latency_ms").and_then(|l| l.as_u64()).unwrap_or(0);

            let short_source = if source.len() > 6 {
                &source[0..6]
            } else {
                source
            };
            let short_target = if target.len() > 6 {
                &target[0..6]
            } else {
                target
            };
            let weight_color = if weight > 0.7 {
                "🟢"
            } else if weight > 0.3 {
                "🟡"
            } else {
                "🔴"
            };
            let latency_color = if latency < 50 {
                "🟢"
            } else if latency < 200 {
                "🟡"
            } else {
                "🔴"
            };

            println!(
                "[{}]--{}{}ms-->[{}] {} (weight: {:.2})",
                short_source, latency_color, latency, short_target, weight_color, weight
            );
        }
    }

    Ok(())
}
