use anyhow::Result;
use serde::{Deserialize, Serialize};
use std::fs;
use std::path::PathBuf;

fn config_path() -> Result<PathBuf> {
    let dir = dirs::home_dir()
        .ok_or_else(|| anyhow::anyhow!("no HOME"))?
        .join(".truthctl");
    fs::create_dir_all(&dir)?;
    Ok(dir.join("config.json"))
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Config {
    pub node_name: String,
    pub port: u16,
    pub db_path: String,
    pub public_key: String,
    pub private_key: String,
    #[serde(default)]
    pub auto_peer: bool,
    #[serde(default)]
    pub p2p_enabled: bool,
}

pub fn default_config() -> Config {
    Config {
        node_name: "node".to_string(),
        port: 8080,
        db_path: "truth.db".to_string(),
        public_key: String::new(),
        private_key: String::new(),
        auto_peer: false,
        p2p_enabled: true,
    }
}

pub fn load_config() -> Result<Config> {
    let path = config_path()?;
    if !path.exists() {
        return Ok(default_config());
    }
    let text = fs::read_to_string(path)?;
    let cfg: Config = serde_json::from_str(&text)?;
    Ok(cfg)
}

pub fn save_config(config: &Config) -> Result<()> {
    let path = config_path()?;
    let json = serde_json::to_string_pretty(config)?;
    fs::write(path, json)?;
    Ok(())
}
