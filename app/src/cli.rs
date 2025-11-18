//! Shared helpers for truthctl subcommands (node discovery utilities).

use anyhow::{Context, Result};
use core_lib::storage;
use std::path::Path;
use std::sync::Arc;
use tokio::sync::Mutex;
use truth_core::node::NodeConfig;

pub type SharedConnection = Arc<Mutex<rusqlite::Connection>>;

/// Load ~/.truthctl/config.json (if present) and merge with overrides/env vars.
pub fn load_discovery_config(extra_registries: &[String]) -> NodeDiscoveryConfig {
    let cfg = crate::config_utils::load_config()
        .unwrap_or_else(|_| crate::config_utils::default_config());
    let mut urls = cfg.node_registries.clone();
    urls.extend(extra_registries.iter().cloned());
    if let Ok(env) = std::env::var("TRUTH_GLOBAL_REGISTRIES") {
        urls.extend(env.split(',').filter_map(|s| {
            let trimmed = s.trim();
            if trimmed.is_empty() {
                None
            } else {
                Some(trimmed.to_string())
            }
        }));
    }
    urls.sort();
    urls.dedup();

    NodeDiscoveryConfig {
        registry_urls: urls,
    }
}

#[derive(Debug, Clone)]
pub struct NodeDiscoveryConfig {
    pub registry_urls: Vec<String>,
}

impl NodeDiscoveryConfig {
    pub fn into_node_config(self) -> NodeConfig {
        NodeConfig {
            bind_host: "0.0.0.0".into(),
            bind_port: 0,
            public_host: None,
            public_port: None,
            timing: core_lib::DEFAULT_DISCOVERY_TIMING,
            global_registry_urls: self.registry_urls,
        }
    }
}

pub fn open_shared_connection(db_path: &Path) -> Result<SharedConnection> {
    let path_str = db_path
        .to_str()
        .context("database path contains invalid UTF-8")?;
    let conn = storage::open_db(path_str)?;
    storage::init_db(&conn)?;
    Ok(Arc::new(Mutex::new(conn)))
}

pub fn open_connection(db_path: &Path) -> Result<rusqlite::Connection> {
    let path_str = db_path
        .to_str()
        .context("database path contains invalid UTF-8")?;
    let conn = storage::open_db(path_str)?;
    storage::init_db(&conn)?;
    Ok(conn)
}
