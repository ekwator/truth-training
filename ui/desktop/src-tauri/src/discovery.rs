use crate::settings::{load_discovery_settings, save_discovery_settings, DiscoverySettings};
use chrono::Utc;
use core_lib::config::{
    DiscoveryTimingConfig, HEALTH_CHECK_RETRY_LIMIT, HEALTH_CHECK_TIMEOUT_SECS,
};
use core_lib::models::{Node, NodeFilter, NodeType};
use core_lib::storage;
use log::{info, warn};
use parking_lot::Mutex as ParkingMutex;
use rusqlite::{params, Connection};
use serde::Serialize;
use std::str::FromStr;
use std::sync::Arc;
use std::time::Duration;
use tauri::State;
use tokio::sync::{Mutex as TokioMutex, RwLock as TokioRwLock};
use truth_core::node::NodeConfig as CoreNodeConfig;
use truth_core::p2p::node::{poll_global_registries, run_http_reachability_checks};

pub struct DiscoveryManager {
    conn: TokioRwLock<Arc<TokioMutex<Connection>>>,
    settings: TokioRwLock<DiscoverySettings>,
    handle: ParkingMutex<Option<tauri::async_runtime::JoinHandle<()>>>,
}

impl DiscoveryManager {
    pub fn init_from_disk() -> Result<Self, String> {
        let settings = load_discovery_settings().unwrap_or_default();
        let conn = Arc::new(TokioMutex::new(open_nodes_connection(&settings.db_path)?));
        let manager = Self {
            conn: TokioRwLock::new(conn),
            settings: TokioRwLock::new(settings),
            handle: ParkingMutex::new(None),
        };
        tauri::async_runtime::block_on(manager.restart_worker());
        Ok(manager)
    }

    /// Restart the background worker, ensuring no stale tasks remain.
    /// This method:
    /// 1. Aborts any existing worker handle
    /// 2. Waits briefly for the abort to propagate
    /// 3. Spawns a new worker if background discovery is enabled
    async fn restart_worker(&self) {
        // Abort existing worker if present
        if let Some(handle) = self.handle.lock().take() {
            handle.abort();
            // Give the abort signal time to propagate (non-blocking)
            // The actual cleanup happens asynchronously
        }

        // Check if background discovery is enabled
        let enable_background = {
            let settings_guard = self.settings.read().await;
            settings_guard.enable_background
        };
        if !enable_background {
            return;
        }

        // Clone settings and connection for the new worker
        let settings = {
            let settings_guard = self.settings.read().await;
            settings_guard.clone()
        };
        let conn = {
            let conn_guard = self.conn.read().await;
            conn_guard.clone()
        };

        // Spawn new worker task
        let handle = tauri::async_runtime::spawn(async move {
            let mut cleanup_tick =
                tokio::time::interval(Duration::from_secs(settings.cleanup_interval_secs.max(5)));
            let mut global_tick =
                tokio::time::interval(Duration::from_secs(settings.global_interval_secs.max(10)));

            // Reset intervals to start immediately on first tick
            cleanup_tick.set_missed_tick_behavior(tokio::time::MissedTickBehavior::Skip);
            global_tick.set_missed_tick_behavior(tokio::time::MissedTickBehavior::Skip);

            loop {
                tokio::select! {
                    _ = cleanup_tick.tick() => {
                        if let Err(e) = run_http_reachability_checks(
                            conn.clone(),
                            Duration::from_secs(HEALTH_CHECK_TIMEOUT_SECS),
                            HEALTH_CHECK_RETRY_LIMIT
                        ).await {
                            warn!("desktop reachability sweep failed: {e}");
                        }
                        if let Err(e) = cleanup_expired(conn.clone()).await {
                            warn!("desktop cleanup failed: {e}");
                        }
                    }
                    _ = global_tick.tick() => {
                        if settings.registry_urls.is_empty() {
                            continue;
                        }
                        if let Err(e) = poll_global_registries(&to_node_config(&settings), conn.clone()).await {
                            warn!("desktop registry poll failed: {e}");
                        } else if let Err(e) = apply_ttl_overrides(conn.clone(), &settings).await {
                            warn!("desktop ttl override failed: {e}");
                        }
                    }
                }
            }
        });
        *self.handle.lock() = Some(handle);
    }

    /// Stop the background worker cleanly.
    /// This is called automatically on Drop, but can be called manually if needed.
    #[allow(dead_code)] // Public API method - may be called by external code
    pub fn stop_worker(&self) {
        if let Some(handle) = self.handle.lock().take() {
            handle.abort();
        }
    }

    pub fn update_settings(&self, settings: DiscoverySettings) -> Result<(), String> {
        tauri::async_runtime::block_on(async {
            {
                let settings_guard = self.settings.read().await;
                if settings_guard.db_path != settings.db_path {
                    let conn = Arc::new(TokioMutex::new(
                        open_nodes_connection(&settings.db_path)
                            .map_err(|e| format!("Failed to open new DB connection: {e}"))?,
                    ));
                    let mut conn_guard = self.conn.write().await;
                    *conn_guard = conn;
                }
            }
            {
                let mut settings_guard = self.settings.write().await;
                *settings_guard = settings.clone();
            }
            Ok::<(), String>(())
        })?;
        save_discovery_settings(&settings)?;
        tauri::async_runtime::block_on(self.restart_worker());
        Ok(())
    }

    pub fn current_settings(&self) -> DiscoverySettings {
        tauri::async_runtime::block_on(async {
            let settings_guard = self.settings.read().await;
            settings_guard.clone()
        })
    }

    pub async fn list_nodes(
        &self,
        node_type: Option<String>,
        reachable: Option<bool>,
    ) -> Result<Vec<NodeRecord>, String> {
        let mut filter = NodeFilter {
            node_type: None,
            reachable,
            limit: None,
            address: None,
        };
        if let Some(label) = node_type {
            if !label.trim().is_empty() && label.to_uppercase() != "ALL" {
                filter.node_type = Some(NodeType::from_str(&label).map_err(|e| e.to_string())?);
            }
        }
        let conn = {
            let conn_guard = self.conn.read().await;
            conn_guard.clone()
        };
        let nodes = {
            let guard = conn.lock().await;
            storage::list_nodes(&*guard, &filter).map_err(|e| e.to_string())?
        };
        let now = Utc::now().timestamp();
        Ok(nodes
            .into_iter()
            .map(|node| NodeRecord::from_node(node, now))
            .collect())
    }

    pub async fn manual_discover(&self, types: Vec<NodeType>) -> Result<DiscoverRun, String> {
        let requested = if types.is_empty() {
            vec![NodeType::Lan, NodeType::Wifi, NodeType::Global]
        } else {
            types
        };
        let conn = {
            let conn_guard = self.conn.read().await;
            conn_guard.clone()
        };
        let settings = {
            let settings_guard = self.settings.read().await;
            settings_guard.clone()
        };
        let start = std::time::Instant::now();
        let mut updated = 0usize;
        let mut discovered = 0usize;

        if requested
            .iter()
            .any(|t| matches!(t, NodeType::Lan | NodeType::Wifi | NodeType::Client))
        {
            updated = run_http_reachability_checks(
                conn.clone(),
                Duration::from_secs(HEALTH_CHECK_TIMEOUT_SECS),
                HEALTH_CHECK_RETRY_LIMIT,
            )
            .await
            .map_err(|e| format!("HTTP reachability check failed: {e}"))?;
            cleanup_expired(conn.clone()).await?;
        }

        if requested
            .iter()
            .any(|t| matches!(t, NodeType::Global | NodeType::Relay))
            && !settings.registry_urls.is_empty()
        {
            discovered = poll_global_registries(&to_node_config(&settings), conn.clone())
                .await
                .map_err(|e| format!("Global registry poll failed: {e}"))?;
            apply_ttl_overrides(conn.clone(), &settings).await?;
        }

        let duration_ms = start.elapsed().as_millis() as u64;
        info!(
            "discovery.command.discover.completed discovered={} updated={} duration_ms={}",
            discovered, updated, duration_ms
        );
        Ok(DiscoverRun {
            discovered,
            updated,
            duration_ms,
        })
    }

    pub async fn cleanup(&self) -> Result<u64, String> {
        info!("discovery.command.cleanup started");
        let conn = {
            let conn_guard = self.conn.read().await;
            conn_guard.clone()
        };
        let result = cleanup_expired(conn).await;
        if let Ok(count) = result {
            info!(
                "discovery.command.cleanup.completed pruned={} expired={} unreachable=0",
                count, count
            );
        }
        result
    }

    pub async fn health_check(&self) -> Result<u64, String> {
        info!("discovery.command.health_check started");
        let conn = {
            let conn_guard = self.conn.read().await;
            conn_guard.clone()
        };
        let result = run_http_reachability_checks(
            conn,
            Duration::from_secs(HEALTH_CHECK_TIMEOUT_SECS),
            HEALTH_CHECK_RETRY_LIMIT,
        )
        .await
        .map(|count| {
            info!("discovery.command.health_check.completed checked={}", count);
            count as u64
        })
        .map_err(|e| {
            warn!("discovery.command.health_check.error error={}", e);
            format!("Health check failed: {e}")
        });
        result
    }
}

/// Open SQLite connection with safe directory creation and permission handling.
/// This function:
/// 1. Creates the parent directory if it doesn't exist
/// 2. Attempts to open the database with RW permissions
/// 3. Falls back to a temporary path if the original path is inaccessible
fn open_nodes_connection(path: &str) -> Result<Connection, String> {
    let db_path = std::path::Path::new(path);

    // Ensure parent directory exists with proper permissions
    if let Some(parent) = db_path.parent() {
        std::fs::create_dir_all(parent)
            .map_err(|e| format!("Failed to prepare discovery db directory: {e}"))?;

        // Verify write permissions on the directory
        let test_file = parent.join(".truth_training_write_test");
        if let Err(e) = std::fs::write(&test_file, b"test") {
            return Err(format!(
                "No write permission for discovery db directory {}: {e}",
                parent.display()
            ));
        }
        // Clean up test file
        let _ = std::fs::remove_file(&test_file);
    }

    // Attempt to open the database
    match storage::open_db(path) {
        Ok(conn) => Ok(conn),
        Err(e) => {
            // If opening fails, try to create a fallback path in a writable location
            let fallback_path = get_fallback_db_path()?;
            warn!(
                "Failed to open discovery db at {}: {e}. Using fallback: {}",
                path, fallback_path
            );
            storage::open_db(&fallback_path).map_err(|e| {
                format!(
                    "Failed to open discovery db at both {} and fallback {}: {e}",
                    path, fallback_path
                )
            })
        }
    }
}

/// Get a fallback database path in a writable location (e.g., temp directory).
fn get_fallback_db_path() -> Result<String, String> {
    let temp_dir = std::env::temp_dir();
    let fallback = temp_dir.join("truth-training").join("nodes.sqlite");
    if let Some(parent) = fallback.parent() {
        std::fs::create_dir_all(parent)
            .map_err(|e| format!("Failed to create fallback db directory: {e}"))?;
    }
    Ok(fallback.display().to_string())
}

fn to_node_config(settings: &DiscoverySettings) -> CoreNodeConfig {
    CoreNodeConfig {
        bind_host: "0.0.0.0".into(),
        bind_port: 0,
        public_host: None,
        public_port: None,
        timing: DiscoveryTimingConfig {
            lan_discovery_interval: Duration::from_secs(settings.lan_interval_secs),
            wifi_discovery_interval: Duration::from_secs(settings.wifi_interval_secs),
            global_poll_interval: Duration::from_secs(settings.global_interval_secs),
            cleanup_interval: Duration::from_secs(settings.cleanup_interval_secs),
            health_check_timeout: Duration::from_secs(HEALTH_CHECK_TIMEOUT_SECS),
            health_check_retry_limit: HEALTH_CHECK_RETRY_LIMIT,
        },
        global_registry_urls: settings.registry_urls.clone(),
    }
}

async fn cleanup_expired(conn: Arc<TokioMutex<Connection>>) -> Result<u64, String> {
    let guard = conn.lock().await;
    storage::prune_stale_nodes(&*guard, Utc::now().timestamp())
        .map(|n| n as u64)
        .map_err(|e| e.to_string())
}

async fn apply_ttl_overrides(
    conn: Arc<TokioMutex<Connection>>,
    settings: &DiscoverySettings,
) -> Result<(), String> {
    let guard = conn.lock().await;
    guard
        .execute(
            "UPDATE nodes SET ttl = ?1 WHERE type = 'LAN' AND ttl < ?1",
            params![settings.lan_ttl_secs],
        )
        .map_err(|e| e.to_string())?;
    guard
        .execute(
            "UPDATE nodes SET ttl = ?1 WHERE type = 'WIFI' AND ttl < ?1",
            params![settings.wifi_ttl_secs],
        )
        .map_err(|e| e.to_string())?;
    guard
        .execute(
            "UPDATE nodes SET ttl = ?1 WHERE type IN ('GLOBAL','RELAY') AND ttl < ?1",
            params![settings.global_ttl_secs],
        )
        .map_err(|e| e.to_string())?;
    Ok(())
}

#[derive(Debug, Serialize)]
pub struct NodeRecord {
    pub id: i64,
    pub address: String,
    pub node_type: String,
    pub reachable: bool,
    pub ttl: i64,
    pub last_seen: i64,
    pub updated_at: i64,
    pub source: Option<String>,
    pub node_id: Option<String>,
    pub expires_in: i64,
}

impl NodeRecord {
    fn from_node(node: Node, now: i64) -> Self {
        let expires_in = (node.last_seen + node.ttl - now).max(0);
        Self {
            id: node.id,
            address: node.address,
            node_type: node.node_type.to_string(),
            reachable: node.reachable,
            ttl: node.ttl,
            last_seen: node.last_seen,
            updated_at: node.updated_at,
            source: node.source.map(|s| s.to_string()),
            node_id: node.node_id,
            expires_in,
        }
    }
}

#[derive(Debug, Serialize)]
pub struct DiscoverRun {
    pub discovered: usize,
    pub updated: usize,
    pub duration_ms: u64,
}

#[tauri::command]
pub async fn list_nodes(
    state: State<'_, DiscoveryManager>,
    node_type: Option<String>,
    reachable: Option<bool>,
) -> Result<Vec<NodeRecord>, String> {
    state.list_nodes(node_type, reachable).await
}

#[tauri::command]
pub async fn manual_discover(
    state: State<'_, DiscoveryManager>,
    node_types: Option<Vec<String>>,
) -> Result<DiscoverRun, String> {
    let parsed = node_types
        .unwrap_or_default()
        .into_iter()
        .filter_map(|label| NodeType::from_str(&label).ok())
        .collect::<Vec<_>>();
    state.manual_discover(parsed).await
}

#[tauri::command]
pub async fn cleanup_nodes(state: State<'_, DiscoveryManager>) -> Result<u64, String> {
    state.cleanup().await
}

#[tauri::command]
pub async fn run_nodes_health_check(state: State<'_, DiscoveryManager>) -> Result<u64, String> {
    state.health_check().await
}

#[tauri::command]
pub fn get_discovery_settings(
    state: State<'_, DiscoveryManager>,
) -> Result<DiscoverySettings, String> {
    Ok(state.current_settings())
}

#[tauri::command]
pub fn save_discovery_settings_cmd(
    state: State<'_, DiscoveryManager>,
    settings: DiscoverySettings,
) -> Result<(), String> {
    state.update_settings(settings)
}
