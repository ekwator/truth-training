//! Discovery event logging and metrics.
//!
//! Provides structured logging for node discovery events including:
//! - Discovery counters (nodes discovered, updated, pruned)
//! - TTL cleanup statistics
//! - Reachability check results
//!
//! Implements T048: Shared logging/metrics for discovery events.

use log::{info, warn};
use serde::{Deserialize, Serialize};

/// Discovery event metrics for observability.
/// Reserved for future structured logging implementation.
#[allow(dead_code)]
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct DiscoveryMetrics {
    /// Total nodes discovered via LAN/Wi-Fi multicast
    pub lan_nodes_discovered: u64,
    /// Total nodes discovered via global registry polling
    pub global_nodes_discovered: u64,
    /// Total nodes updated (existing nodes refreshed)
    pub nodes_updated: u64,
    /// Total nodes pruned (TTL expired or unreachable)
    pub nodes_pruned: u64,
    /// Total reachability checks performed
    pub reachability_checks: u64,
    /// Total reachability checks that succeeded
    pub reachability_successes: u64,
    /// Total reachability checks that failed
    pub reachability_failures: u64,
    /// Last cleanup timestamp (Unix seconds)
    pub last_cleanup: Option<i64>,
    /// Last global registry poll timestamp (Unix seconds)
    pub last_global_poll: Option<i64>,
}

impl Default for DiscoveryMetrics {
    fn default() -> Self {
        Self {
            lan_nodes_discovered: 0,
            global_nodes_discovered: 0,
            nodes_updated: 0,
            nodes_pruned: 0,
            reachability_checks: 0,
            reachability_successes: 0,
            reachability_failures: 0,
            last_cleanup: None,
            last_global_poll: None,
        }
    }
}

/// Log a node discovery event.
#[allow(dead_code)] // Intentionally unused - provided for future structured logging
pub fn log_node_discovered(source: &str, node_id: &str, address: &str, node_type: &str) {
    info!(
        "discovery.node.discovered source={} node_id={} address={} node_type={}",
        source, node_id, address, node_type
    );
}

/// Log a node update event.
#[allow(dead_code)] // Intentionally unused - provided for future structured logging
pub fn log_node_updated(node_id: &str, address: &str, reason: &str) {
    info!(
        "discovery.node.updated node_id={} address={} reason={}",
        node_id, address, reason
    );
}

/// Log TTL cleanup statistics.
#[allow(dead_code)] // Intentionally unused - provided for future structured logging
pub fn log_cleanup_stats(pruned_count: u64, expired_count: u64, unreachable_count: u64) {
    info!(
        "discovery.cleanup.completed pruned={} expired={} unreachable={}",
        pruned_count, expired_count, unreachable_count
    );
}

/// Log reachability check results.
#[allow(dead_code)] // Intentionally unused - provided for future structured logging
pub fn log_reachability_check(address: &str, reachable: bool, duration_ms: u64) {
    let status = if reachable { "success" } else { "failure" };
    info!(
        "discovery.reachability.check address={} status={} duration_ms={}",
        address, status, duration_ms
    );
}

/// Log reachability check batch summary.
#[allow(dead_code)] // Intentionally unused - provided for future structured logging
pub fn log_reachability_batch(total: u64, successful: u64, failed: u64, duration_ms: u64) {
    info!(
        "discovery.reachability.batch total={} successful={} failed={} duration_ms={}",
        total, successful, failed, duration_ms
    );
}

/// Log global registry polling results.
#[allow(dead_code)] // Intentionally unused - provided for future structured logging
pub fn log_global_registry_poll(
    registry_url: &str,
    nodes_found: usize,
    nodes_added: usize,
    duration_ms: u64,
) {
    info!(
        "discovery.registry.poll url={} found={} added={} duration_ms={}",
        registry_url, nodes_found, nodes_added, duration_ms
    );
}

/// Log global registry polling error.
#[allow(dead_code)] // Intentionally unused - provided for future structured logging
pub fn log_global_registry_error(registry_url: &str, error: &str) {
    warn!(
        "discovery.registry.error url={} error={}",
        registry_url, error
    );
}

/// Log LAN discovery announcement sent.
#[allow(dead_code)] // Intentionally unused - provided for future structured logging
pub fn log_lan_announcement_sent(node_id: &str, address: &str) {
    info!(
        "discovery.lan.announcement.sent node_id={} address={}",
        node_id, address
    );
}

/// Log LAN discovery announcement received.
#[allow(dead_code)] // Intentionally unused - provided for future structured logging
pub fn log_lan_announcement_received(node_id: &str, address: &str, valid: bool) {
    if valid {
        info!(
            "discovery.lan.announcement.received node_id={} address={}",
            node_id, address
        );
    } else {
        warn!(
            "discovery.lan.announcement.rejected node_id={} address={} reason=invalid_signature",
            node_id, address
        );
    }
}

/// Log discovery worker lifecycle events.
#[allow(dead_code)] // Intentionally unused - provided for future structured logging
pub fn log_worker_started(worker_type: &str) {
    info!("discovery.worker.started type={}", worker_type);
}

/// Log discovery worker lifecycle events.
#[allow(dead_code)] // Intentionally unused - provided for future structured logging
pub fn log_worker_stopped(worker_type: &str) {
    info!("discovery.worker.stopped type={}", worker_type);
}

/// Log discovery worker error.
#[allow(dead_code)] // Intentionally unused - provided for future structured logging
pub fn log_worker_error(worker_type: &str, error: &str) {
    warn!(
        "discovery.worker.error type={} error={}",
        worker_type, error
    );
}
