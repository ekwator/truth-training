use clap::{Parser, Subcommand, ValueEnum};
use std::path::PathBuf;
use colored::*;
use core_lib::storage;
use serde::{Deserialize, Serialize};
use std::fs;
use std::time::{SystemTime, UNIX_EPOCH};

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
        peer: String,
        /// Путь к JSON-файлу ключей
        #[arg(long)]
        identity: PathBuf,
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
    },
    /// Управление ключами
    Keys {
        #[command(subcommand)]
        cmd: KeysCmd,
    },
}

#[derive(Copy, Clone, Debug, ValueEnum)]
enum Mode { Full, Incremental, Push, Pull }

#[derive(serde::Deserialize)]
struct KeyFile { private_key: String, public_key: String }

#[derive(Subcommand, Debug)]
enum KeysCmd {
    /// Импорт пары ключей Ed25519 в локальное хранилище (~/.truthctl/keys.json)
    Import { private_key_hex: String, public_key_hex: String },
    /// Список импортированных ключей
    List,
}

#[tokio::main(flavor = "multi_thread")] 
async fn main() -> anyhow::Result<()> {
    let cli = Cli::parse();
    match cli.command {
        Commands::Sync { peer, identity, db, mode } => {
            run_sync(peer, identity, db, mode).await
        }
        Commands::Status { db, identity } => {
            run_status(db, identity).await
        }
        Commands::Verify { db } => {
            run_verify(db).await
        }
        Commands::Ratings { db, recalc } => {
            run_ratings(db, recalc).await
        }
        Commands::Keys { cmd } => {
            run_keys(cmd).await
        }
    }
}

async fn run_status(db_path: PathBuf, identity_path: Option<PathBuf>) -> anyhow::Result<()> {
    let exists = std::path::Path::new(&db_path).exists();
    println!("{}", format!("DB: {} ({})", db_path.display(), if exists { "exists" } else { "missing" }).blue());
    
    if let Some(p) = identity_path {
        let data = std::fs::read_to_string(&p)?;
        let k: KeyFile = serde_json::from_str(&data)?;
        let id = truth_core::p2p::encryption::CryptoIdentity::from_keypair_hex(&k.private_key, &k.public_key)
            .map_err(|e| anyhow::anyhow!(e))?;
        println!("{}", format!("Public Key: {}", id.public_key_hex()).green());
    }
    
    // базовая статистика
    if exists {
        let conn = storage::open_db(db_path.to_str().unwrap())?;
        let events = storage::load_truth_events(&conn)?;
        let statements = storage::load_statements(&conn)?;
        let node_ratings = storage::load_node_ratings(&conn)?;
        let group_ratings = storage::load_group_ratings(&conn)?;
        
        println!("{}", format!("Events: {}", events.len()).yellow());
        println!("{}", format!("Statements: {}", statements.len()).yellow());
        println!("{}", format!("Node Ratings: {}", node_ratings.len()).yellow());
        println!("{}", format!("Group Ratings: {}", group_ratings.len()).yellow());
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
    if let Ok(store) = load_keys() {
        if let Some(k) = store.keys.first() {
            let _id = truth_core::p2p::encryption::CryptoIdentity::from_keypair_hex(&k.private_key_hex, &k.public_key_hex)
                .map_err(|e| anyhow::anyhow!(e))?;
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
            // валидация ключей
            truth_core::p2p::encryption::CryptoIdentity::from_keypair_hex(&private_key_hex, &public_key_hex)
                .map_err(|e| anyhow::anyhow!(e))?;

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
    }
    Ok(())
}

async fn run_ratings(db_path: PathBuf, recalc: bool) -> anyhow::Result<()> {
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
    
    println!("{}", format!("Node Ratings: {}", node_ratings.len()).blue());
    for rating in &node_ratings {
        println!("  {}: {:.3}", rating.node_id, rating.trust_score);
    }
    
    println!("{}", format!("Group Ratings: {}", group_ratings.len()).blue());
    for rating in &group_ratings {
        println!("  {}: {:.3} (coherence: {:.3})", rating.group_id, rating.avg_score, rating.coherence);
    }
    
    Ok(())
}

#[cfg(feature = "p2p-client-sync")]
async fn run_sync(peer: String, identity_path: PathBuf, db_path: PathBuf, mode: Mode) -> anyhow::Result<()> {
    use truth_core::p2p::encryption::CryptoIdentity;
    use truth_core::p2p::sync::{sync_with_peer, bidirectional_sync_with_peer, incremental_sync_with_peer, push_local_data, pull_remote_data};
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

fn print_sync_result(res: truth_core::p2p::sync::SyncResult) {
    println!("{}", format!("✅ Sync successful:\n   - Events added: {}\n   - Statements added: {}\n   - Impacts added: {}\n   - Conflicts resolved: {}",
        res.events_added, res.statements_added, res.impacts_added, res.conflicts_resolved).green());
}
