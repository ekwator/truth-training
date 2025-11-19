//! Shared configuration for node discovery cadences and TTL rules (FR-012).
//! Values are derived from the Unified Node Discovery specification.

use std::time::Duration;

/// Default TTL (seconds) per node type.
pub const LAN_TTL_SECS: i64 = 120;
pub const WIFI_TTL_SECS: i64 = 300;
pub const GLOBAL_TTL_SECS: i64 = 3600;
pub const RELAY_TTL_SECS: i64 = 3600;
pub const CLIENT_TTL_SECS: i64 = 600;

/// Default discovery/cleanup cadences in seconds.
pub const LAN_DISCOVERY_INTERVAL_SECS: u64 = 30;
pub const WIFI_DISCOVERY_INTERVAL_SECS: u64 = 45;
pub const GLOBAL_POLL_INTERVAL_SECS: u64 = 3600; // 1 hour
pub const CLEANUP_INTERVAL_SECS: u64 = 60;
pub const HEALTH_CHECK_TIMEOUT_SECS: u64 = 5;
pub const HEALTH_CHECK_RETRY_LIMIT: u8 = 3;

/// Helper describing all timing knobs in a single struct.
#[derive(Debug, Clone, Copy)]
pub struct DiscoveryTimingConfig {
    pub lan_discovery_interval: Duration,
    pub wifi_discovery_interval: Duration,
    pub global_poll_interval: Duration,
    pub cleanup_interval: Duration,
    pub health_check_timeout: Duration,
    pub health_check_retry_limit: u8,
}

pub const DEFAULT_DISCOVERY_TIMING: DiscoveryTimingConfig = DiscoveryTimingConfig {
    lan_discovery_interval: Duration::from_secs(LAN_DISCOVERY_INTERVAL_SECS),
    wifi_discovery_interval: Duration::from_secs(WIFI_DISCOVERY_INTERVAL_SECS),
    global_poll_interval: Duration::from_secs(GLOBAL_POLL_INTERVAL_SECS),
    cleanup_interval: Duration::from_secs(CLEANUP_INTERVAL_SECS),
    health_check_timeout: Duration::from_secs(HEALTH_CHECK_TIMEOUT_SECS),
    health_check_retry_limit: HEALTH_CHECK_RETRY_LIMIT,
};

/// Returns the default TTL (seconds) for a given node type label.
pub fn default_ttl_for(node_type: &str) -> i64 {
    match node_type.to_uppercase().as_str() {
        "LAN" => LAN_TTL_SECS,
        "WIFI" | "WI-FI" => WIFI_TTL_SECS,
        "GLOBAL" => GLOBAL_TTL_SECS,
        "RELAY" | "SERVER" => RELAY_TTL_SECS,
        "CLIENT" => CLIENT_TTL_SECS,
        _ => GLOBAL_TTL_SECS,
    }
}

/// Returns whether a TTL value satisfies minimum requirements for the provided node type.
pub fn validate_ttl(node_type: &str, ttl: i64) -> bool {
    ttl >= default_ttl_for(node_type)
}
