use core_lib::storage as truth_storage;
use dirs;
use log::info;
use rusqlite::Connection;
use serde::{Deserialize, Serialize};
use std::fs;
use std::path::PathBuf;
use tauri::{command, State};

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct AppConfig {
    pub mode: String, // "core" or "http"
    pub server_ip: String,
    pub server_port: u16,
    #[serde(default)]
    pub nearby_sync: bool,
    #[serde(default = "default_nearby_interval_ms")]
    pub nearby_interval_ms: u64,
    #[serde(default = "default_locale")]
    pub locale: String,
}

impl Default for AppConfig {
    fn default() -> Self {
        Self {
            mode: "core".to_string(),
            server_ip: "127.0.0.1".to_string(),
            server_port: 8080,
            nearby_sync: false,
            nearby_interval_ms: default_nearby_interval_ms(),
            locale: default_locale(),
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
    if config.nearby_interval_ms < 500 || config.nearby_interval_ms > 60_000 {
        return Err("nearby_interval_ms must be between 500 and 60000".to_string());
    }

    let config_path = get_config_path()?;

    // Ensure directory exists
    if let Some(parent) = config_path.parent() {
        fs::create_dir_all(parent)
            .map_err(|e| format!("Failed to create config directory: {}", e))?;
    }

    let content = serde_json::to_string_pretty(&config)
        .map_err(|e| format!("Failed to serialize config: {}", e))?;

    fs::write(&config_path, content).map_err(|e| format!("Failed to write config file: {}", e))?;

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
        Err(e) => Ok(CoreStatus {
            ok: false,
            message: format!("HTTP connection failed: {}", e),
        }),
    }
}

#[command]
pub async fn init_app(db: State<'_, crate::storage::Db>) -> Result<CoreStatus, String> {
    // 1) Read current config to preserve locale setting
    let current_config = get_app_config().await.unwrap_or_else(|_| AppConfig::default());
    let locale = current_config.locale.clone();
    
    // 2) Reset config to defaults but preserve locale
    let mut default_config = AppConfig::default();
    default_config.locale = locale.clone();
    write_default_config(&default_config)?;

    // 3) Reset database using the current connection with preserved locale
    {
        let mut conn = db.0.lock();
        reset_database(&mut conn, &locale)?;
    }

    info!("init_app completed successfully: dropped legacy tables, recreated Truth schema, and seeded knowledge base with locale: {}", locale);

    Ok(CoreStatus {
        ok: true,
        message: format!("Initialized config and database (locale: {})", locale),
    })
}

#[command]
pub async fn reseed_knowledge_base(db: State<'_, crate::storage::Db>) -> Result<CoreStatus, String> {
    // Get current locale from config
    let config = get_app_config().await.unwrap_or_else(|_| AppConfig::default());
    let locale = config.locale.clone();
    
    // Clear existing knowledge base data and reseed with current locale
    {
        let mut conn = db.0.lock();
        
        // Clear existing data
        conn.execute("DELETE FROM category", [])
            .map_err(|e| format!("Failed to clear categories: {}", e))?;
        conn.execute("DELETE FROM cause", [])
            .map_err(|e| format!("Failed to clear causes: {}", e))?;
        conn.execute("DELETE FROM develop", [])
            .map_err(|e| format!("Failed to clear develops: {}", e))?;
        conn.execute("DELETE FROM effect", [])
            .map_err(|e| format!("Failed to clear effects: {}", e))?;
        conn.execute("DELETE FROM forma", [])
            .map_err(|e| format!("Failed to clear formas: {}", e))?;
        conn.execute("DELETE FROM impact_type", [])
            .map_err(|e| format!("Failed to clear impact_types: {}", e))?;
        
        // Reseed with current locale
        truth_storage::seed_knowledge_base(&mut conn, &locale)
            .map_err(|e| format!("Failed to reseed knowledge base: {}", e))?;
    }
    
    info!("Knowledge base reseeded with locale: {}", locale);
    
    Ok(CoreStatus {
        ok: true,
        message: format!("Knowledge base reseeded with locale: {}", locale),
    })
}

fn get_config_path() -> Result<PathBuf, String> {
    let home_dir = dirs::home_dir().ok_or("Failed to get home directory")?;

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

fn default_nearby_interval_ms() -> u64 {
    3_000
}

fn default_locale() -> String {
    "en".to_string()
}

fn write_default_config(config: &AppConfig) -> Result<(), String> {
    let cfg_path = get_config_path()?;
    if let Some(parent) = cfg_path.parent() {
        fs::create_dir_all(parent)
            .map_err(|e| format!("Failed to create config directory: {}", e))?;
    }
    let content = serde_json::to_string_pretty(config)
        .map_err(|e| format!("Failed to serialize default config: {}", e))?;
    fs::write(&cfg_path, content).map_err(|e| format!("Failed to write default config: {}", e))?;
    Ok(())
}

const LEGACY_TABLES: &[&str] = &["events", "impacts", "summaries", "judgments", "logs"];

fn reset_database(conn: &mut Connection, locale: &str) -> Result<(), String> {
    conn.execute_batch("PRAGMA foreign_keys = OFF;")
        .map_err(|e| format!("Failed to disable foreign keys: {}", e))?;

    for table in LEGACY_TABLES {
        conn.execute(&format!("DROP TABLE IF EXISTS {}", table), [])
            .map_err(|e| format!("Failed to drop legacy table '{}': {}", table, e))?;
    }

    conn.execute_batch(
        r#"
        PRAGMA wal_checkpoint(TRUNCATE);
        VACUUM;
        PRAGMA foreign_keys = ON;
        PRAGMA journal_mode=WAL;
    "#,
    )
    .map_err(|e| format!("Failed to clean up WAL/VACUUM: {}", e))?;

    truth_storage::init_db(conn)
        .map_err(|e| format!("Failed to initialize Truth schema: {}", e))?;
    truth_storage::seed_knowledge_base(conn, locale)
        .map_err(|e| format!("Failed to seed knowledge base: {}", e))?;
    truth_storage::assert_no_legacy_tables(conn)
        .map_err(|e| format!("Legacy tables still present: {}", e))?;

    Ok(())
}

/// Reusable helper for integration tests to exercise the reset logic.
#[cfg(test)]
pub fn reset_database_for_tests(conn: &mut Connection) -> Result<(), String> {
    reset_database(conn, "en")
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::commands::test_support as support;

    #[test]
    fn reset_database_removes_legacy_tables_and_recreates_truth_schema() {
        let mut conn = support::memory_conn_with_legacy();
        reset_database_for_tests(&mut conn).expect("reset database");

        assert!(support::legacy_tables_absent(&conn));
        for table in [
            "truth_events",
            "statements",
            "impact",
            "progress_metrics",
            "context",
            "category",
            "cause",
            "develop",
            "effect",
            "forma",
            "impact_type",
            "schema_version",
        ] {
            assert!(
                support::table_exists(&conn, table),
                "table '{}' should exist after reset",
                table
            );
        }
    }

    #[test]
    fn reset_database_is_idempotent() {
        let mut conn = support::memory_conn_with_legacy();
        reset_database_for_tests(&mut conn).expect("first reset");
        reset_database_for_tests(&mut conn).expect("second reset");
        assert!(support::legacy_tables_absent(&conn));
    }
}
