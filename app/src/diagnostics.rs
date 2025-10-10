use anyhow::Result;
use colored::*;
use serde::{Deserialize, Serialize};
use std::fs;
use std::path::{Path, PathBuf};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct DiagnosticResult {
    pub check: String,
    pub status: String,
    pub message: String,
}

#[derive(Debug, Clone, Serialize, Deserialize, Default)]
struct KeyStore {
    pub keys: Vec<KeyPair>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
struct KeyPair {
    pub id: u64,
    pub private_key_hex: String,
    pub public_key_hex: String,
    pub created_at: String,
}

fn truthctl_dir() -> Result<PathBuf> {
    let dir = dirs::home_dir()
        .ok_or_else(|| anyhow::anyhow!("no HOME"))?
        .join(".truthctl");
    fs::create_dir_all(&dir)?;
    Ok(dir)
}

fn config_path() -> Result<PathBuf> { Ok(truthctl_dir()?.join("config.json")) }
fn keys_path() -> Result<PathBuf> { Ok(truthctl_dir()?.join("keys.json")) }
fn peers_path() -> Result<PathBuf> { Ok(truthctl_dir()?.join("peers.json")) }

fn load_keys_file() -> Result<KeyStore> {
    let path = keys_path()?;
    if !path.exists() { return Ok(KeyStore::default()); }
    let data = fs::read_to_string(path)?;
    Ok(serde_json::from_str(&data).unwrap_or_default())
}

fn save_keys_file(store: &KeyStore) -> Result<()> {
    let path = keys_path()?;
    let json = serde_json::to_string_pretty(store)?;
    fs::write(path, json)?;
    Ok(())
}

fn load_peers_file() -> Result<crate::status_utils::Peers> {
    let path = peers_path()?;
    if !path.exists() { return Ok(crate::status_utils::Peers::default()); }
    let data = fs::read_to_string(path)?;
    Ok(serde_json::from_str(&data).unwrap_or_default())
}

pub async fn run_diagnostics(_verbose: bool) -> Vec<DiagnosticResult> {
    let mut results: Vec<DiagnosticResult> = Vec::new();

    // Config
    let cfg_path = config_path().ok();
    let cfg_exists = cfg_path.as_ref().map(|p| p.exists()).unwrap_or(false);
    let mut cfg_loaded: Option<crate::config_utils::Config> = None;
    if cfg_exists {
        match fs::read_to_string(cfg_path.unwrap())
            .ok()
            .and_then(|t| serde_json::from_str::<crate::config_utils::Config>(&t).ok())
        {
            Some(cfg) => {
                let ok = !cfg.node_name.is_empty()
                    && !cfg.db_path.is_empty()
                    && cfg.port > 0;
                if ok {
                    results.push(DiagnosticResult {
                        check: "Config".to_string(),
                        status: "✅ OK".to_string(),
                        message: format!("node={}, port={}, db={}", cfg.node_name, cfg.port, cfg.db_path),
                    });
                    cfg_loaded = Some(cfg);
                } else {
                    results.push(DiagnosticResult {
                        check: "Config".to_string(),
                        status: "⚠️ Missing".to_string(),
                        message: "invalid or incomplete fields".to_string(),
                    });
                }
            }
            None => results.push(DiagnosticResult {
                check: "Config".to_string(),
                status: "⚠️ Missing".to_string(),
                message: "cannot read config.json".to_string(),
            }),
        }
    } else {
        results.push(DiagnosticResult {
            check: "Config".to_string(),
            status: "⚠️ Missing".to_string(),
            message: "~/.truthctl/config.json not found".to_string(),
        });
    }

    // Keys
    match load_keys_file() {
        Ok(ks) => {
            let valid = ks.keys.iter().any(|k| is_valid_hex64(&k.private_key_hex) && is_valid_hex64(&k.public_key_hex));
            if valid {
                results.push(DiagnosticResult {
                    check: "Keys".to_string(),
                    status: "✅ OK".to_string(),
                    message: format!("{} key(s) present", ks.keys.len()),
                });
            } else {
                let status = "❌ Missing";
                results.push(DiagnosticResult {
                    check: "Keys".to_string(),
                    status: status.to_string(),
                    message: "no valid keypair found".to_string(),
                });
            }
        }
        Err(_) => results.push(DiagnosticResult {
            check: "Keys".to_string(),
            status: "❌ Missing".to_string(),
            message: "cannot read keys.json".to_string(),
        }),
    }

    // Peers
    match load_peers_file() {
        Ok(peers) => {
            let count = peers.peers.len();
            if count > 0 {
                results.push(DiagnosticResult {
                    check: "Peers".to_string(),
                    status: "✅ OK".to_string(),
                    message: format!("{} peer(s)", count),
                });
            } else {
                results.push(DiagnosticResult {
                    check: "Peers".to_string(),
                    status: "⚠️ None".to_string(),
                    message: "no peers configured".to_string(),
                });
            }
        }
        Err(_) => results.push(DiagnosticResult {
            check: "Peers".to_string(),
            status: "⚠️ None".to_string(),
            message: "cannot read peers.json".to_string(),
        }),
    }

    // Database
    let db_status = if let Some(cfg) = &cfg_loaded {
        if Path::new(&cfg.db_path).exists() {
            DiagnosticResult { check: "Database".to_string(), status: "✅ OK".to_string(), message: cfg.db_path.clone() }
        } else {
            DiagnosticResult { check: "Database".to_string(), status: "⚠️ Missing".to_string(), message: cfg.db_path.clone() }
        }
    } else {
        DiagnosticResult { check: "Database".to_string(), status: "⚠️ Missing".to_string(), message: "config not found".to_string() }
    };
    results.push(db_status);

    // P2P feature
    if cfg!(feature = "p2p-client-sync") {
        results.push(DiagnosticResult { check: "P2P Feature".to_string(), status: "✅ Enabled".to_string(), message: String::new() });
    } else {
        results.push(DiagnosticResult { check: "P2P Feature".to_string(), status: "⚠️ Disabled".to_string(), message: String::new() });
    }

    results
}

pub fn print_diagnostic_summary(results: &[DiagnosticResult]) {
    println!("{}", "Diagnostics".blue());
    for r in results {
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
}

pub fn reset_local_data(confirm: bool, reinit: bool) -> Result<()> {
    use rusqlite::Connection;
    // Load config (existing or default)
    let cfg = crate::config_utils::load_config().unwrap_or_else(|_| crate::config_utils::default_config());

    // Clear logs if DB exists, then remove DB file
    let db_path = PathBuf::from(&cfg.db_path);
    if db_path.exists() {
        if let Ok(conn) = Connection::open(&db_path) {
            let _ = core_lib::storage::clear_sync_logs(&conn);
        }
        let _ = fs::remove_file(&db_path);
    }

    // Remove peers.json based on confirmation
    let peers_path = peers_path()?;
    if peers_path.exists() {
        let mut remove = confirm;
        if !confirm {
            // Ask interactively
            let question = format!("Remove {}?", peers_path.display());
            if let Ok(ans) = dialoguer::Confirm::new().with_prompt(question).default(false).interact() {
                remove = ans;
            }
        }
        if remove {
            let _ = fs::remove_file(&peers_path);
        }
    }

    println!("{}", "🧹 Node data cleared successfully.".green());

    if reinit {
        ensure_keypair_exists(true)?;
        // derive values from existing or default config
        let name = if cfg.node_name.is_empty() { "node".to_string() } else { cfg.node_name };
        let port = cfg.port.to_string();
        let db = if cfg.db_path.is_empty() { "truth.db".to_string() } else { cfg.db_path };

        let exe = std::env::current_exe().unwrap_or_else(|_| PathBuf::from("truthctl"));
        let status = std::process::Command::new(exe)
            .arg("init-node")
            .arg(name)
            .arg("--port")
            .arg(port)
            .arg("--db")
            .arg(db)
            .arg("--auto-peer")
            .status();
        match status {
            Ok(s) if s.success() => {
                println!("{}", "🚀 Node reinitialized successfully.".green());
            }
            Ok(s) => {
                anyhow::bail!(format!("init-node failed with status {}", s));
            }
            Err(e) => anyhow::bail!(e),
        }
    }

    Ok(())
}

pub fn ensure_keypair_exists(interactive: bool) -> Result<()> {
    use ed25519_dalek::SigningKey;
    use rand::rngs::OsRng;
    use std::time::{SystemTime, UNIX_EPOCH};

    let mut ks = load_keys_file()?;

    if ks.keys.is_empty() {
        // generate a new keypair
        let mut rng = OsRng;
        let sk = SigningKey::generate(&mut rng);
        let vk = sk.verifying_key();
        let priv_hex = hex::encode(sk.to_bytes());
        let pub_hex = hex::encode(vk.to_bytes());
        let ts = SystemTime::now().duration_since(UNIX_EPOCH)?.as_secs();
        let created_at = chrono::DateTime::<chrono::Utc>::from(UNIX_EPOCH + std::time::Duration::from_secs(ts)).to_rfc3339();
        ks.keys.push(KeyPair { id: 1, private_key_hex: priv_hex, public_key_hex: pub_hex, created_at });
        save_keys_file(&ks)?;
        println!("{}", "🔑 New keypair generated.".green());
        return Ok(());
    }

    if interactive {
        println!("A keypair already exists.\nDo you want to:\n[1] Keep existing key\n[2] Generate new key and replace old one");
        let choice: String = dialoguer::Input::new()
            .with_prompt("Enter choice [1/2]")
            .interact_text()
            .unwrap_or_else(|_| "1".to_string());
        if choice.trim() == "2" {
            let mut rng = OsRng;
            let sk = SigningKey::generate(&mut rng);
            let vk = sk.verifying_key();
            let priv_hex = hex::encode(sk.to_bytes());
            let pub_hex = hex::encode(vk.to_bytes());
            let ts = SystemTime::now().duration_since(UNIX_EPOCH)?.as_secs();
            let created_at = chrono::DateTime::<chrono::Utc>::from(UNIX_EPOCH + std::time::Duration::from_secs(ts)).to_rfc3339();
            if let Some(first) = ks.keys.first_mut() {
                first.private_key_hex = priv_hex;
                first.public_key_hex = pub_hex;
                first.created_at = created_at;
            } else {
                ks.keys.push(KeyPair { id: 1, private_key_hex: priv_hex, public_key_hex: pub_hex, created_at });
            }
            save_keys_file(&ks)?;
            println!("{}", "🔁 Keypair replaced.".yellow());
        }
    }
    Ok(())
}

fn is_valid_hex64(s: &str) -> bool {
    if s.len() != 64 { return false; }
    s.chars().all(|c| c.is_ascii_hexdigit())
}
