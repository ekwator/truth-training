package com.truth.training.client.data.database.entities

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.PrimaryKey
import androidx.room.Index

/**
 * Room entity for nodes table matching canonical SQLite schema.
 * 
 * Schema compatibility:
 * - Desktop (rusqlite): ✅ Uses identical DDL
 * - CLI (rusqlite): ✅ Uses identical DDL
 * - Android (Room): ✅ Fully integrated
 * 
 * Reference: 
 * - specs/008-specify-md/data-model.md
 * - docs/cross_platform_discovery_compatibility.md
 */
@Entity(
    tableName = "nodes",
    indices = [
        Index(value = ["address"], name = "idx_nodes_address"),
        Index(value = ["last_seen"], name = "idx_nodes_last_seen"),
        Index(value = ["type"], name = "idx_nodes_type"),
        Index(value = ["reachable"], name = "idx_nodes_reachable")
    ]
)
data class NodeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "address")
    val address: String,
    
    @ColumnInfo(name = "type")
    val type: String,  // "LAN" | "WIFI" | "GLOBAL" | "RELAY" | "CLIENT"
    
    @ColumnInfo(name = "reachable")
    val reachable: Int,  // 0 = unreachable, 1 = reachable
    
    @ColumnInfo(name = "last_seen")
    val lastSeen: Long,  // Unix timestamp (seconds)
    
    @ColumnInfo(name = "ttl")
    val ttl: Long,  // Time-to-live in seconds
    
    @ColumnInfo(name = "source")
    val source: String?,  // "local_broadcast" | "wifi_scan" | "global_registry" | "manual" | "peer_sync"
    
    @ColumnInfo(name = "node_id")
    val nodeId: String?,  // ed25519 public key (hex, 64 chars)
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long,  // Unix timestamp (seconds)
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long   // Unix timestamp (seconds)
)

