use dirs::home_dir;
use serde::{Deserialize, Serialize};
use std::fs;
use std::path::{Path, PathBuf};

use core_lib::config::{
    CLEANUP_INTERVAL_SECS, GLOBAL_POLL_INTERVAL_SECS, GLOBAL_TTL_SECS, LAN_TTL_SECS, WIFI_TTL_SECS,
};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct DiscoverySettings {
    pub enable_background: bool,
    pub lan_interval_secs: u64,
    pub wifi_interval_secs: u64,
    pub global_interval_secs: u64,
    pub cleanup_interval_secs: u64,
    pub lan_ttl_secs: i64,
    pub wifi_ttl_secs: i64,
    pub global_ttl_secs: i64,
    pub registry_urls: Vec<String>,
    pub db_path: String,
}

impl Default for DiscoverySettings {
    fn default() -> Self {
        Self {
            enable_background: true,
            lan_interval_secs: 30,
            wifi_interval_secs: 45,
            global_interval_secs: GLOBAL_POLL_INTERVAL_SECS,
            cleanup_interval_secs: CLEANUP_INTERVAL_SECS,
            lan_ttl_secs: LAN_TTL_SECS,
            wifi_ttl_secs: WIFI_TTL_SECS,
            global_ttl_secs: GLOBAL_TTL_SECS,
            registry_urls: Vec::new(),
            db_path: default_db_path(),
        }
    }
}

pub fn load_discovery_settings() -> Result<DiscoverySettings, String> {
    let path = settings_path()?;
    if !path.exists() {
        return Ok(DiscoverySettings::default());
    }
    let content =
        fs::read_to_string(&path).map_err(|e| format!("Failed to read discovery settings: {e}"))?;
    let mut settings: DiscoverySettings = serde_json::from_str(&content)
        .map_err(|e| format!("Failed to parse discovery settings: {e}"))?;
    if settings.db_path.trim().is_empty() {
        settings.db_path = default_db_path();
    }
    Ok(settings)
}

pub fn save_discovery_settings(settings: &DiscoverySettings) -> Result<(), String> {
    let path = settings_path()?;
    if let Some(dir) = path.parent() {
        fs::create_dir_all(dir)
            .map_err(|e| format!("Failed to create discovery settings dir: {e}"))?;
    }
    let json = serde_json::to_string_pretty(settings)
        .map_err(|e| format!("Failed to serialize discovery settings: {e}"))?;
    fs::write(&path, json).map_err(|e| format!("Failed to write discovery settings: {e}"))
}

pub fn settings_path() -> Result<PathBuf, String> {
    let home = home_dir().ok_or_else(|| "Failed to determine home directory".to_string())?;
    Ok(home.join(".truth-training").join("discovery_settings.json"))
}

fn default_db_path() -> String {
    let base = home_dir().unwrap_or_else(|| Path::new(".").to_path_buf());
    // Use a different filename for discovery DB to avoid conflicts with main app DB
    base.join(".truth-training")
        .join("discovery_nodes.sqlite")
        .display()
        .to_string()
}
