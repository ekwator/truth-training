package com.truth.training.client.data.models

/**
 * Node source enumeration matching Rust NodeSource enum.
 * 
 * Implements cross-platform compatibility:
 * - Rust: core/src/models.rs::NodeSource
 * - Android: This enum
 * 
 * String values match Rust serialization (snake_case):
 * - "local_broadcast" | "wifi_scan" | "global_registry" | "manual" | "peer_sync"
 * 
 * Reference:
 * - docs/cross_platform_discovery_compatibility.md
 * - core/src/models.rs::NodeSource
 */
enum class NodeSource(val value: String) {
    LOCAL_BROADCAST("local_broadcast"),
    WIFI_SCAN("wifi_scan"),
    GLOBAL_REGISTRY("global_registry"),
    MANUAL("manual"),
    PEER_SYNC("peer_sync");
    
    companion object {
        /**
         * Parse string to NodeSource.
         * Matches Rust implementation: core/src/models.rs::NodeSource::from_str()
         * 
         * @param s String value (case-insensitive)
         * @return NodeSource or null if invalid
         */
        fun fromString(s: String?): NodeSource? {
            if (s == null) return null
            return when (s.lowercase()) {
                "local_broadcast" -> LOCAL_BROADCAST
                "wifi_scan" -> WIFI_SCAN
                "global_registry" -> GLOBAL_REGISTRY
                "manual" -> MANUAL
                "peer_sync" -> PEER_SYNC
                else -> null
            }
        }
    }
}

