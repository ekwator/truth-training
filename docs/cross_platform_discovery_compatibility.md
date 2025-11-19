# Cross-Platform Discovery Format Compatibility Review

**Date**: 2025-01-XX  
**Feature**: Unified Node Discovery & Address Exchange  
**Task Group**: 3 - Cross-Platform Review

## Executive Summary

This document reviews the compatibility of discovery formats across Desktop (Tauri), CLI (truthctl), and Android platforms. It identifies format inconsistencies, documents expected JSON payloads, and provides integration guidance.

## Discovery Format Specifications

### 1. LAN/Wi-Fi Broadcast Format (UDP Multicast)

**Platforms**: Desktop (Tauri), CLI, Server  
**Protocol**: UDP Multicast on `239.255.0.1:52525`  
**Encoding**: JSON (UTF-8)

#### Rust Implementation (`src/p2p/node.rs`)

```rust
#[derive(Debug, serde::Serialize, serde::Deserialize)]
struct LanAnnouncement {
    node_id: String,        // ed25519 public key (hex, 64 chars)
    address: String,         // Full URL: "http://host:port/api/v1"
    node_type: NodeType,     // Enum: Lan, Wifi, Global, Relay, Client
    ttl: i64,                // Time-to-live in seconds
    timestamp: i64,          // Unix timestamp (seconds)
    signature: String,        // ed25519 signature (hex, 128 chars)
}
```

#### JSON Payload Example

```json
{
  "node_id": "a1b2c3d4e5f6...",
  "address": "http://192.168.1.100:8080/api/v1",
  "node_type": "LAN",
  "ttl": 120,
  "timestamp": 1704067200,
  "signature": "0123456789abcdef..."
}
```

#### Signature Verification

The signature is computed over the payload string:
```
"{node_id}|{address}|{node_type}|{ttl}|{timestamp}"
```

Example: `"a1b2c3...|http://192.168.1.100:8080/api/v1|LAN|120|1704067200"`

**Verification**: Uses `ed25519-dalek` to verify signature with `node_id` as public key.

### 2. Global Registry Format (HTTPS)

**Platforms**: Desktop, CLI, Server  
**Protocol**: HTTPS GET  
**Encoding**: JSON

#### Registry Response Format

**Option A - Envelope**:
```json
{
  "nodes": [
    {
      "address": "https://registry.example.com:443/api/v1",
      "node_type": "GLOBAL",
      "ttl": 3600,
      "node_id": "optional-hex-public-key"
    }
  ]
}
```

**Option B - Array**:
```json
[
  {
    "address": "https://registry.example.com:443/api/v1",
    "node_type": "GLOBAL",
    "ttl": 3600,
    "node_id": "optional-hex-public-key"
  }
]
```

Both formats are supported. Missing fields use defaults:
- `node_type`: Defaults to `GLOBAL`
- `ttl`: Defaults to `NodeType::min_ttl_secs()`
- `node_id`: Optional (can be `null` or omitted)

### 3. Database Schema (Canonical SQLite)

**Platforms**: Desktop (rusqlite), CLI (rusqlite), Android (Room)  
**Table**: `nodes`

```sql
CREATE TABLE IF NOT EXISTS nodes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    address TEXT NOT NULL,
    type TEXT NOT NULL,              -- 'LAN', 'WIFI', 'GLOBAL', 'RELAY', 'CLIENT'
    reachable INTEGER NOT NULL,       -- 0 = unreachable, 1 = reachable
    last_seen INTEGER NOT NULL,       -- Unix timestamp (seconds)
    ttl INTEGER NOT NULL,             -- Time-to-live in seconds
    source TEXT,                      -- 'local_broadcast', 'wifi_scan', 'global_registry', 'manual', 'peer_sync'
    node_id TEXT,                     -- Public key (hex) for node identity
    created_at INTEGER NOT NULL,      -- Unix timestamp (seconds)
    updated_at INTEGER NOT NULL       -- Unix timestamp (seconds)
);
```

**Consistency**: ✅ All platforms use identical DDL.

## Node Type Enumeration

### Rust (`core/src/models.rs`)

```rust
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "UPPERCASE")]
pub enum NodeType {
    Lan,      // Serializes as "LAN"
    Wifi,     // Serializes as "WIFI"
    Global,   // Serializes as "GLOBAL"
    Relay,    // Serializes as "RELAY"
    Client,   // Serializes as "CLIENT"
}
```

**String Parsing**:
- `"LAN"` → `NodeType::Lan`
- `"WIFI"` or `"WI-FI"` → `NodeType::Wifi`
- `"GLOBAL"` → `NodeType::Global`
- `"RELAY"` or `"SERVER"` → `NodeType::Relay`
- `"CLIENT"` → `NodeType::Client`

### Android (`truth-android-client/app/src/main/java/com/truth/training/client/data/models/NodeType.kt`)

**Status**: ✅ **IMPLEMENTED**

```kotlin
enum class NodeType(val value: String) {
    LAN("LAN"),
    WIFI("WIFI"),
    GLOBAL("GLOBAL"),
    RELAY("RELAY"),
    CLIENT("CLIENT");
    
    companion object {
        fun fromString(s: String?): NodeType? {
            return when (s?.uppercase()) {
                "LAN" -> LAN
                "WIFI", "WI-FI" -> WIFI
                "GLOBAL" -> GLOBAL
                "RELAY", "SERVER" -> RELAY
                "CLIENT" -> CLIENT
                else -> null
            }
        }
    }
}
```

**String Parsing** (matches Rust):
- `"LAN"` → `NodeType.LAN`
- `"WIFI"` or `"WI-FI"` → `NodeType.WIFI`
- `"GLOBAL"` → `NodeType.GLOBAL`
- `"RELAY"` or `"SERVER"` → `NodeType.RELAY`
- `"CLIENT"` → `NodeType.CLIENT`

**Storage**: Stored as TEXT in database (matches Rust serialization).

## Node Source Enumeration

### Rust (`core/src/models.rs`)

```rust
pub enum NodeSource {
    LocalBroadcast,   // "local_broadcast"
    WifiScan,         // "wifi_scan"
    GlobalRegistry,   // "global_registry"
    Manual,           // "manual"
    PeerSync,         // "peer_sync"
}
```

**Database Storage**: Stored as TEXT, can be `NULL`.

### Android (`truth-android-client/app/src/main/java/com/truth/training/client/data/models/NodeSource.kt`)

**Status**: ✅ **IMPLEMENTED**

```kotlin
enum class NodeSource(val value: String) {
    LOCAL_BROADCAST("local_broadcast"),
    WIFI_SCAN("wifi_scan"),
    GLOBAL_REGISTRY("global_registry"),
    MANUAL("manual"),
    PEER_SYNC("peer_sync");
    
    companion object {
        fun fromString(s: String?): NodeSource? {
            return when (s?.lowercase()) {
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
```

**String Parsing** (matches Rust):
- `"local_broadcast"` → `NodeSource.LOCAL_BROADCAST`
- `"wifi_scan"` → `NodeSource.WIFI_SCAN`
- `"global_registry"` → `NodeSource.GLOBAL_REGISTRY`
- `"manual"` → `NodeSource.MANUAL`
- `"peer_sync"` → `NodeSource.PEER_SYNC`

**Storage**: Stored as TEXT in database, can be `NULL` (matches Rust).

## TTL Fields and Defaults

### Default TTL Values (`core/src/config.rs`)

| Node Type | Default TTL | Minimum TTL |
|-----------|------------|--------------|
| LAN       | 120 sec    | 60 sec       |
| WIFI      | 300 sec    | 120 sec      |
| GLOBAL    | 3600 sec   | 300 sec      |
| RELAY     | 3600 sec   | 300 sec      |
| CLIENT    | 600 sec    | 120 sec      |

### TTL Validation Rules

1. **Minimum Enforcement**: TTL values below `NodeType::min_ttl_secs()` are automatically adjusted upward.
2. **Cleanup Logic**: Nodes are pruned when `(now() - last_seen) > ttl`.
3. **Unreachable Nodes**: Nodes with `reachable = 0` for more than `ttl / 2` are candidates for deletion.

**Consistency**: ✅ All platforms use the same TTL constants from `core/src/config.rs`.

## Error Codes and Handling

### Discovery Errors

**Rust (`src/p2p/sync.rs` - `SyncError`)**:
- `Other(String)` - Generic error with message
- Signature verification failures return `SyncError::Other("announcement signature invalid: {e}")`
- Invalid node_id format returns `SyncError::Other("invalid node_id hex: {e}")`

**HTTP API Errors** (`src/api.rs`):
- `400 Bad Request` - Invalid request body or validation failure
- `404 Not Found` - Node not found
- `409 Conflict` - Address already exists with different type
- `500 Internal Server Error` - Database or internal error

**Consistency**: ✅ Error handling is consistent across Desktop and CLI (both use Rust).

### Android Error Handling

**Status**: ✅ **IMPLEMENTED**

Error handling implemented in:
- `LanDiscoveryClient.kt` - UDP multicast receive failures (logged, continue listening)
- `LanDiscoveryClient.kt` - Signature verification failures (logged, announcement rejected)
- `DiscoveryRepository.kt` - Database write failures (Result.failure() returned)
- `NodeSyncWorker.kt` - Transient error detection (retry vs failure)

## Unreachable Nodes Logic

### Reachability Check

**Platforms**: Desktop, CLI, Server  
**Endpoint**: `GET {address}/api/v1/nodes/health`  
**Timeout**: 5 seconds (configurable)  
**Retries**: 3 attempts with exponential backoff

### State Transitions

1. **Discovery**: `reachable = 1`, `last_seen = now()`
2. **Health Check Success**: `reachable = 1`, `last_seen = now()`, `updated_at = now()`
3. **Health Check Failure**: `reachable = 0`, `updated_at = now()`
4. **TTL Expiry**: Record deleted when `(now() - last_seen) > ttl`
5. **Unreachable Cleanup**: Nodes with `reachable = 0` AND `(now() - last_seen) > (ttl / 2)` are deleted

**Consistency**: ✅ Logic is identical across Desktop, CLI, and Server (shared in `src/p2p/node.rs`).

## Platform-Specific Implementation Status

### ✅ Desktop (Tauri)

**Status**: **FULLY IMPLEMENTED**

- ✅ UDP multicast advertiser/listener
- ✅ Global registry polling
- ✅ HTTP reachability checks
- ✅ TTL cleanup
- ✅ Database integration (rusqlite)
- ✅ Settings persistence
- ✅ Background worker lifecycle

**Files**:
- `ui/desktop/src-tauri/src/discovery.rs` - DiscoveryManager
- `ui/desktop/src/components/NodesPanel.tsx` - UI

### ✅ CLI (truthctl)

**Status**: **FULLY IMPLEMENTED**

- ✅ Node list/add/remove commands
- ✅ Discovery trigger (`truthctl nodes discover`)
- ✅ Sync command (`truthctl nodes sync`)
- ✅ Cleanup command (`truthctl nodes cleanup`)
- ✅ Health check command (`truthctl nodes health-check`)
- ✅ Validate command (`truthctl nodes validate`)

**Files**:
- `app/src/bin/truthctl.rs` - CLI commands
- `app/src/cli.rs` - Shared utilities

### ✅ Server (HTTP API)

**Status**: **FULLY IMPLEMENTED**

- ✅ REST API endpoints (`/nodes`, `/nodes/{id}`, `/nodes/discover`, `/nodes/sync`, `/nodes/health`)
- ✅ Background discovery workers
- ✅ TTL cleanup scheduler
- ✅ Global registry polling

**Files**:
- `src/api.rs` - HTTP handlers
- `src/p2p/node.rs` - Discovery logic
- `src/main.rs` - Background tasks

### ✅ Android

**Status**: **FULLY IMPLEMENTED**

**Implementation**:
- ✅ UDP multicast listener (`239.255.0.1:52525`)
- ✅ Global registry polling (HTTP GET)
- ✅ HTTP reachability checks
- ✅ TTL cleanup
- ✅ Database integration (Room)
- ✅ Background worker (WorkManager)
- ✅ UI integration (Compose)

**Files**:
- `truth-android-client/app/src/main/java/com/truth/training/client/data/database/entities/NodeEntity.kt` - Room entity
- `truth-android-client/app/src/main/java/com/truth/training/client/data/database/daos/NodeDao.kt` - DAO
- `truth-android-client/app/src/main/java/com/truth/training/client/data/repository/DiscoveryRepository.kt` - Repository
- `truth-android-client/app/src/main/java/com/truth/training/client/network/LanDiscoveryClient.kt` - UDP multicast
- `truth-android-client/app/src/main/java/com/truth/training/client/worker/NodeSyncWorker.kt` - Background worker
- `truth-android-client/app/src/main/java/com/truth/training/client/ui/compose/nodes/NodesScreen.kt` - UI
- `truth-android-client/app/src/main/java/com/truth/training/client/data/models/NodeType.kt` - NodeType enum
- `truth-android-client/app/src/main/java/com/truth/training/client/data/models/NodeSource.kt` - NodeSource enum

**Reference**: See `docs/android_discovery_architecture.md` for detailed architecture documentation.

## Android Implementation Summary

### ✅ All Tasks Completed (T042-T047)

All Android integration tasks have been fully implemented:

1. **T042**: ✅ NodeEntity + NodeDao + migration (Room)
   - File: `entities/NodeEntity.kt`, `daos/NodeDao.kt`
   - Migration: `MIGRATION_2_3` in `TruthDatabaseMigrations.kt`

2. **T043**: ✅ DiscoveryRepository
   - File: `repository/DiscoveryRepository.kt`
   - Methods: `upsertNode()`, `pollGlobalRegistries()`, `runReachabilityChecks()`, `pruneStaleNodes()`

3. **T044**: ✅ NodeSyncWorker (WorkManager)
   - File: `worker/NodeSyncWorker.kt`
   - Periodic sync: Every 15 minutes
   - Operations: Registry polling, reachability checks, cleanup

4. **T045**: ✅ UDP Multicast LAN Discovery
   - File: `network/LanDiscoveryClient.kt`
   - Address: `239.255.0.1:52525`
   - Signature verification: ed25519 via `Ed25519CryptoManager`

5. **T046**: ✅ Android UI Integration
   - Files: `ui/compose/nodes/NodesScreen.kt`, `NodesViewModel.kt`
   - Features: Node list, filters, manual actions

6. **T047**: ✅ Android Instrumentation Tests
   - File: `androidTest/NodeDiscoveryTest.kt`
   - Coverage: DAO, Repository, Worker, cross-platform compatibility

### Additional Components

- **NodeType Enum**: `data/models/NodeType.kt` - Matches Rust enum
- **NodeSource Enum**: `data/models/NodeSource.kt` - Matches Rust enum
- **Architecture Documentation**: `docs/android_discovery_architecture.md`

**Reference**: See `docs/android_discovery_architecture.md` for detailed architecture documentation.

## Compatibility Matrix

| Feature | Desktop | CLI | Server | Android |
|---------|---------|-----|--------|---------|
| UDP Multicast Discovery | ✅ | ✅ | ✅ | ✅ |
| Global Registry Polling | ✅ | ✅ | ✅ | ✅ |
| HTTP Reachability Checks | ✅ | ✅ | ✅ | ✅ |
| TTL Cleanup | ✅ | ✅ | ✅ | ✅ |
| Database Schema | ✅ | ✅ | ✅ | ✅ |
| Node Type Enum | ✅ | ✅ | ✅ | ✅ |
| Node Source Enum | ✅ | ✅ | ✅ | ✅ |
| Signature Verification | ✅ | ✅ | ✅ | ✅ |
| Error Handling | ✅ | ✅ | ✅ | ✅ |

## Recommendations

1. **Cross-Platform Compatibility**: ✅ All platforms (Desktop, CLI, Server, Android) are fully integrated and compatible.
2. **Format Consistency**: ✅ All platforms use identical JSON formats, database schemas, and enums.
3. **Testing**: ✅ Cross-platform compatibility tests implemented in `NodeDiscoveryTest.kt`.
4. **Documentation**: ✅ Complete architecture documentation in `docs/android_discovery_architecture.md`.

## References

- `specs/008-specify-md/data-model.md` - Canonical schema
- `specs/008-specify-md/contracts/nodes-api.yaml` - API specification
- `src/p2p/node.rs` - Rust discovery implementation
- `core/src/models.rs` - Shared data models
- `core/src/config.rs` - TTL and timing constants

