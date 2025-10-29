use serde::{Deserialize, Serialize};
use std::fs;
use std::path::PathBuf;
use tauri::command;
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
        if let Ok(num) = part.parse::<u8>() {
            if num > 255 {
                return false;
            }
        } else {
            return false;
        }
    }
    
    true
}
