# Discovery Nodes Database Schema

**Document Version:** v1.1.0  
**Status:** Specification  
**Updated:** 2025-12-28  
**Status:** Approved

**Database File**: `discovery_nodes.sqlite`  
**Location**: `~/.truth-training/discovery_nodes.sqlite` (Linux/macOS) or `%USERPROFILE%\.truth-training\discovery_nodes.sqlite` (Windows)  
**Purpose**: Stores discovered peer nodes in the Truth Training network for Desktop UI discovery worker

## Overview

The `discovery_nodes.sqlite` database is a separate database file used exclusively by the Desktop UI discovery worker to store discovered peer nodes. It is separate from the main application database (`truth_training.sqlite`) to avoid conflicts and allow independent management of discovery data.

**Important**: This database contains only network discovery metadata (node addresses, types, TTL, reachability status). It does **not** store any user actions, personal data, or identifiable information. All data is network-related and ephemeral (TTL-based cleanup).

## Schema

The database uses the canonical Truth schema from `core/src/storage.rs`, which includes the `nodes` table and other network-related tables.

For complete technical details about the network tables schema, including SQL definitions, indexes, views, and triggers, see [docs/model_core_network_tables.sql](model_core_network_tables.sql).

### Table: nodes

The `nodes` table stores discovered peer nodes in the Truth Training network.

| Column      | Type     | Notes                                                                                  |
|-------------|----------|----------------------------------------------------------------------------------------|
| id          | INTEGER  | Primary key (`AUTOINCREMENT`)                                                          |
| address     | TEXT     | Full URL or `ip:port` of the peer (e.g., `http://192.168.1.100:8080/api/v1`)          |
| type        | TEXT     | Node type: `LAN`, `WIFI`, `GLOBAL`, `RELAY`, `CLIENT` (UPPERCASE)                     |
| reachable   | INTEGER  | `0/1` flag updated by health checks (0 = unreachable, 1 = reachable)                   |
| last_seen   | INTEGER  | UNIX epoch seconds of the last successful contact or discovery                         |
| ttl         | INTEGER  | Time-to-live (seconds) before the record is considered stale                          |
| source      | TEXT     | Discovery source: `local_broadcast`, `wifi_scan`, `global_registry`, `manual`, `peer_sync` (snake_case) |
| node_id     | TEXT     | Optional Ed25519 public key (hex, 64 characters) for cryptographic node identity       |
| created_at  | INTEGER  | Creation timestamp (UNIX epoch seconds)                                                |
| updated_at | INTEGER  | Last modification timestamp (UNIX epoch seconds)                                        |

### Schema Management Tables

#### schema_version Table

Tracks database schema versions for version control and migration tracking.

| Column | Type | Description |
|--------|------|-------------|
| version | TEXT | Schema version (primary key) |
| applied_at | INTEGER | Time when version was applied (UNIX epoch seconds) |
| description | TEXT | Description of the version |

**Note**: This table is used for database migration management and is not part of the functional data model.

### Additional Tables

The database also includes the full canonical Truth schema from `core/src/storage.rs`, including:
- Node ratings and trust tables (`node_ratings`, `group_ratings`)
- Node performance metrics (`node_performance`)
- Authentication and session management (`active_tokens`)
- Peer synchronization history (`peer_synchronization`)
- Low-level synchronization logs (`sync_operations`, `sync_attempts`)

**Note**: While the full schema is created, the discovery worker primarily uses only the `nodes` table. Other tables are present for schema consistency but are not actively used by the discovery worker.

## Privacy and Confidentiality

**✅ Privacy-Compliant Design**:

1. **No User Actions Stored**: The database does not store any user actions, clicks, navigation, or interaction history.
2. **No Personal Data**: No names, emails, phone numbers, or other personally identifiable information (PII) are stored.
3. **No IP Tracking**: While `address` field contains network addresses, these are:
   - Ephemeral (TTL-based cleanup)
   - Network discovery metadata only
   - Not linked to user identity
   - Automatically pruned when stale
4. **No Device Fingerprinting**: No MAC addresses, device IDs, or hardware identifiers are stored.
5. **No Analytics**: No usage analytics, telemetry, or tracking data is stored.

**Data Stored**:
- Network discovery metadata (peer node addresses, types, TTL)
- Reachability status (health check results)
- Discovery timestamps (last_seen, created_at, updated_at)
- Optional cryptographic node identifiers (public keys, not linked to user identity)

**Data Not Stored**:
- User actions or interactions
- Personal information
- Device identifiers
- Usage patterns
- Analytics or telemetry

## Database Initialization

The database is initialized by the Desktop UI discovery worker (`DiscoveryManager`) using:

```rust
storage::open_db(path)  // Calls init_db() which creates the full canonical schema
```

The schema is created from `core/src/storage.rs::SCHEMA_SQL`, ensuring consistency across all platforms.

## TTL and Cleanup

Nodes are automatically pruned when:
- TTL expires (`last_seen + ttl < current_time`)
- Node becomes unreachable (health check failures)
- Manual cleanup is triggered

Default TTL values (from `core/src/config.rs`):
- LAN nodes: 300 seconds (5 minutes)
- Wi-Fi nodes: 600 seconds (10 minutes)
- Global/Relay nodes: 3600 seconds (1 hour)

## Cross-Platform Compatibility

The `nodes` table schema is identical across all platforms:
- **Desktop/CLI/Server** (Rust): Uses `rusqlite` with schema from `core/src/storage.rs`
- **Android** (Kotlin): Uses Room with `NodeEntity` matching the canonical schema
- **Discovery Worker** (Desktop): Uses separate `discovery_nodes.sqlite` file

## Related Documentation

- [Data Schema](Data_Schema.md) - Main application database schema
- [Cross-Platform Discovery Compatibility](cross_platform_discovery_compatibility.md) - Discovery protocol details
- [Android Discovery Architecture](android_discovery_architecture.md) - Android-specific implementation
- [Network Tables SQL Schema](model_core_network_tables.sql) - Complete SQL schema and implementation details

---

_Version: v1.0.0_
