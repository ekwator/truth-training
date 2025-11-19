package com.truth.training.client.data.models

/**
 * Node type enumeration matching Rust NodeType enum.
 * 
 * Implements cross-platform compatibility:
 * - Rust: core/src/models.rs::NodeType
 * - Android: This enum
 * 
 * String values match Rust serialization (UPPERCASE):
 * - "LAN" | "WIFI" | "GLOBAL" | "RELAY" | "CLIENT"
 * 
 * Reference:
 * - docs/cross_platform_discovery_compatibility.md
 * - core/src/models.rs::NodeType
 */
enum class NodeType(val value: String) {
    LAN("LAN"),
    WIFI("WIFI"),
    GLOBAL("GLOBAL"),
    RELAY("RELAY"),
    CLIENT("CLIENT");
    
    companion object {
        /**
         * Parse string to NodeType.
         * Matches Rust implementation: core/src/models.rs::NodeType::from_str()
         * 
         * @param s String value (case-insensitive)
         * @return NodeType or null if invalid
         */
        fun fromString(s: String?): NodeType? {
            if (s == null) return null
            return when (s.uppercase()) {
                "LAN" -> LAN
                "WIFI", "WI-FI" -> WIFI
                "GLOBAL" -> GLOBAL
                "RELAY", "SERVER" -> RELAY
                "CLIENT" -> CLIENT
                else -> null
            }
        }
        
        /**
         * Get minimum TTL for node type (in seconds).
         * Matches Rust: core/src/models.rs::NodeType::min_ttl_secs()
         */
        fun minTtlSecs(type: NodeType): Long {
            return when (type) {
                LAN -> 60L
                WIFI -> 120L
                GLOBAL -> 300L
                RELAY -> 300L
                CLIENT -> 120L
            }
        }
        
        /**
         * Get default TTL for node type (in seconds).
         */
        fun defaultTtlSecs(type: NodeType): Long {
            return when (type) {
                LAN -> 120L
                WIFI -> 300L
                GLOBAL -> 3600L
                RELAY -> 3600L
                CLIENT -> 600L
            }
        }
    }
}

