use serde::{Deserialize, Serialize};
use std::fs;
use std::path::PathBuf;
use tauri::{command, State};
use dirs;

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct AppConfig {
    pub mode: String, // "core" or "http"
    pub server_ip: String,
    pub server_port: u16,
}

impl Default for AppConfig {
    fn default() -> Self {
        Self {
            mode: "core".to_string(),
            server_ip: "127.0.0.1".to_string(),
            server_port: 8080,
        }
    }
}

#[derive(Debug, Serialize, Deserialize)]
pub struct CoreStatus {
    pub ok: bool,
    pub message: String,
}

#[command]
pub async fn get_app_config() -> Result<AppConfig, String> {
    let config_path = get_config_path()?;
    
    if !config_path.exists() {
        // Return default config if file doesn't exist
        return Ok(AppConfig::default());
    }
    
    let content = fs::read_to_string(&config_path)
        .map_err(|e| format!("Failed to read config file: {}", e))?;
    
    let config: AppConfig = serde_json::from_str(&content)
        .map_err(|e| format!("Failed to parse config file: {}", e))?;
    
    Ok(config)
}

#[command]
pub async fn save_app_config(config: AppConfig) -> Result<(), String> {
    // Validate config
    if !["core", "http"].contains(&config.mode.as_str()) {
        return Err("Invalid mode. Must be 'core' or 'http'".to_string());
    }
    
    if !is_valid_ip(&config.server_ip) {
        return Err("Invalid IP address format".to_string());
    }
    
    // u16 cannot exceed 65535; only check for zero (invalid)
    if config.server_port == 0 {
        return Err("Invalid port. Must be between 1 and 65535".to_string());
    }
    
    let config_path = get_config_path()?;
    
    // Ensure directory exists
    if let Some(parent) = config_path.parent() {
        fs::create_dir_all(parent)
            .map_err(|e| format!("Failed to create config directory: {}", e))?;
    }
    
    let content = serde_json::to_string_pretty(&config)
        .map_err(|e| format!("Failed to serialize config: {}", e))?;
    
    fs::write(&config_path, content)
        .map_err(|e| format!("Failed to write config file: {}", e))?;
    
    Ok(())
}

#[command]
pub async fn core_status() -> Result<CoreStatus, String> {
    // Simulate core status check
    // In a real implementation, this would check if the embedded core is running
    Ok(CoreStatus {
        ok: true,
        message: "Core is running".to_string(),
    })
}

#[command]
pub async fn test_http_connection(ip: String, port: u16) -> Result<CoreStatus, String> {
    // Validate inputs
    if !is_valid_ip(&ip) {
        return Err("Invalid IP address format".to_string());
    }
    
    // u16 cannot exceed 65535; only check for zero (invalid)
    if port == 0 {
        return Err("Invalid port. Must be between 1 and 65535".to_string());
    }
    
    // Test HTTP connection
    let url = format!("http://{}:{}/status", ip, port);
    
    match reqwest::get(&url).await {
        Ok(response) => {
            if response.status().is_success() {
                Ok(CoreStatus {
                    ok: true,
                    message: "HTTP connection successful".to_string(),
                })
            } else {
                Ok(CoreStatus {
                    ok: false,
                    message: format!("HTTP server responded with status: {}", response.status()),
                })
            }
        }
        Err(e) => {
            Ok(CoreStatus {
                ok: false,
                message: format!("HTTP connection failed: {}", e),
            })
        }
    }
}

#[command]
pub async fn init_app(db: State<'_, crate::storage::Db>) -> Result<CoreStatus, String> {
    // 1) Reset config to defaults (overwrite)
    let default_cfg = AppConfig::default();
    let cfg_path = get_config_path()?;
    if let Some(parent) = cfg_path.parent() {
        fs::create_dir_all(parent)
            .map_err(|e| format!("Failed to create config directory: {}", e))?;
    }
    let content = serde_json::to_string_pretty(&default_cfg)
        .map_err(|e| format!("Failed to serialize default config: {}", e))?;
    fs::write(&cfg_path, content)
        .map_err(|e| format!("Failed to write default config: {}", e))?;

    // 2) Reset database using the current connection
    let conn = db.0.lock();
    // Remove data from known tables and vacuum. This avoids file handle issues across platforms.
    let sql_cleanup = r#"
        PRAGMA foreign_keys = OFF;
        DELETE FROM judgments;
        DELETE FROM impacts;
        DELETE FROM summaries;
        DELETE FROM logs;
        DELETE FROM events;
        PRAGMA wal_checkpoint(TRUNCATE);
        VACUUM;
        PRAGMA foreign_keys = ON;
    "#;
    conn.execute_batch(sql_cleanup)
        .map_err(|e| format!("Failed to reset database: {}", e))?;

    // 3) Recreate schema to ensure integrity (idempotent)
    // storage::Db::initialize uses the same schema; here we run its batch again.
    let sql_schema = r#"
            PRAGMA journal_mode=WAL;
            CREATE TABLE IF NOT EXISTS events (
              id TEXT PRIMARY KEY,
              title TEXT NOT NULL,
              description TEXT,
              context_id TEXT NOT NULL,
              start_date TEXT,
              end_date TEXT,
              created_at TEXT NOT NULL,
              updated_at TEXT,
              status TEXT NOT NULL
            );
            CREATE TABLE IF NOT EXISTS impacts (
              id TEXT PRIMARY KEY,
              event_id TEXT NOT NULL,
              impact_level INTEGER NOT NULL CHECK(impact_level >= 1 AND impact_level <= 5),
              notes TEXT,
              created_at TEXT NOT NULL,
              FOREIGN KEY(event_id) REFERENCES events(id)
            );
            CREATE TABLE IF NOT EXISTS summaries (
              id TEXT PRIMARY KEY,
              event_id TEXT NOT NULL UNIQUE,
              summary_text TEXT,
              recommendations TEXT,
              updated_at TEXT NOT NULL,
              FOREIGN KEY(event_id) REFERENCES events(id)
            );
            CREATE TABLE IF NOT EXISTS judgments (
              id TEXT PRIMARY KEY,
              event_id TEXT NOT NULL,
              assessment TEXT NOT NULL,
              confidence_level REAL NOT NULL,
              reasoning TEXT,
              submitted_at TEXT NOT NULL,
              FOREIGN KEY(event_id) REFERENCES events(id)
            );
            CREATE TABLE IF NOT EXISTS logs (
              id TEXT PRIMARY KEY,
              timestamp TEXT NOT NULL,
              source TEXT NOT NULL,
              level TEXT NOT NULL,
              message TEXT NOT NULL
            );
        "#;
    conn.execute_batch(sql_schema)
        .map_err(|e| format!("Failed to recreate schema: {}", e))?;

    Ok(CoreStatus { ok: true, message: "Initialized config and database".to_string() })
}

fn get_config_path() -> Result<PathBuf, String> {
    let home_dir = dirs::home_dir()
        .ok_or("Failed to get home directory")?;
    
    Ok(home_dir.join(".truth-training").join("config.json"))
}

fn is_valid_ip(ip: &str) -> bool {
    // Simple IP validation regex: ^\d{1,3}(\.\d{1,3}){3}$
    let parts: Vec<&str> = ip.split('.').collect();
    if parts.len() != 4 {
        return false;
    }
    
    for part in parts {
        if part.parse::<u8>().is_err() {
            return false;
        }
    }
    
    true
}
