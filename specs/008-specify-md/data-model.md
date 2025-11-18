# Data Model: Node Discovery & Address Exchange

**Feature**: Unified Cross-Platform Node Discovery  
**Date**: 2025-11-17

## Entities

### Node

Represents a discoverable server or client endpoint in the Truth Training network.

**Table**: `nodes`

**Schema** (SQLite DDL):
```sql
CREATE TABLE IF NOT EXISTS nodes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    address TEXT NOT NULL,
    type TEXT NOT NULL,              -- 'LAN', 'WIFI', 'GLOBAL', 'RELAY', 'CLIENT'
    reachable INTEGER NOT NULL,       -- 0 = unreachable, 1 = reachable
    last_seen INTEGER NOT NULL,       -- Unix timestamp (seconds)
    ttl INTEGER NOT NULL,             -- Time-to-live in seconds
    source TEXT,                      -- 'local_broadcast', 'wifi_scan', 'global_registry', 'manual'
    node_id TEXT,                     -- Public key (hex) for node identity
    created_at INTEGER NOT NULL,      -- Unix timestamp (seconds)
    updated_at INTEGER NOT NULL       -- Unix timestamp (seconds)
);

CREATE INDEX IF NOT EXISTS idx_nodes_address ON nodes(address);
CREATE INDEX IF NOT EXISTS idx_nodes_last_seen ON nodes(last_seen);
CREATE INDEX IF NOT EXISTS idx_nodes_type ON nodes(type);
CREATE INDEX IF NOT EXISTS idx_nodes_reachable ON nodes(reachable);
```

**Fields**:
- `id`: Auto-incrementing primary key (INTEGER)
- `address`: Network address (e.g., `http://192.168.1.100:8080`, `https://example.com:443`)
- `type`: Node type category (`LAN`, `WIFI`, `GLOBAL`, `RELAY`, `CLIENT`)
- `reachable`: Boolean flag (0/1) indicating if node responded to last health check
- `last_seen`: Unix timestamp (seconds) of last successful contact
- `ttl`: Time-to-live in seconds before node is considered stale
- `source`: Discovery source identifier
- `node_id`: Optional public key (hex) for cryptographic node identity
- `created_at`: Timestamp when record was first created
- `updated_at`: Timestamp when record was last modified

**Validation Rules**:
- `address` must be a valid URL (http/https) or IP:port
- `type` must be one of: `LAN`, `WIFI`, `GLOBAL`, `RELAY`, `CLIENT`
- `reachable` must be 0 or 1
- `last_seen` must be a positive integer (Unix timestamp)
- `ttl` must be positive (minimum 60 seconds)
- `node_id` (if present) must be valid hex string (64 chars for ed25519 public key)

**State Transitions**:
1. **Discovery**: `reachable = 1`, `last_seen = now()`, `created_at = now()`
2. **Health Check Success**: `reachable = 1`, `last_seen = now()`, `updated_at = now()`
3. **Health Check Failure**: `reachable = 0`, `updated_at = now()`
4. **TTL Expiry**: Record deleted when `(now() - last_seen) > ttl`
5. **Merge Update**: `last_seen = max(local.last_seen, remote.last_seen)`, `updated_at = now()`

**Relationships**:
- Optional: `node_id` can reference `node_ratings.node_id` (if node has reputation)
- Optional: `node_id` can reference `users.pubkey` (if node has user account)

---

## Data Flow

### Discovery Flow
```
1. Periodic discovery cycle starts
2. Broadcast UDP multicast (LAN/Wi-Fi) OR poll global registry (Global)
3. Receive node advertisements
4. Validate node identity (signature check if node_id present)
5. Insert or update node record in local DB
6. Trigger background health check
7. Update reachable flag based on health check result
```

### Merge Flow
```
1. Receive node list from remote peer (via handshake or API)
2. For each remote node:
   a. Check if local node exists (by address)
   b. If exists: Apply merge rules (priority, last_seen, tie-break)
   c. If new: Insert with source = 'peer_sync'
3. Return merged list to peer
4. Update local DB with merged results
```

### Cleanup Flow
```
1. Periodic cleanup task runs (frequency < TTL)
2. Query nodes where (now() - last_seen) > ttl
3. Delete expired nodes
4. Query nodes where reachable = 0 AND (now() - last_seen) > (ttl / 2)
5. Delete unreachable nodes that haven't been seen recently
```

---

## Cross-Platform Compatibility

### Rust (rusqlite)
```rust
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Node {
    pub id: Option<i64>,
    pub address: String,
    pub node_type: String,  // "LAN" | "WIFI" | "GLOBAL" | "RELAY" | "CLIENT"
    pub reachable: bool,
    pub last_seen: i64,
    pub ttl: i64,
    pub source: Option<String>,
    pub node_id: Option<String>,
    pub created_at: i64,
    pub updated_at: i64,
}
```

### Android (Room)
```kotlin
@Entity(tableName = "nodes")
data class Node(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "address")
    val address: String,
    
    @ColumnInfo(name = "type")
    val type: String,  // "LAN" | "WIFI" | "GLOBAL" | "RELAY" | "CLIENT"
    
    @ColumnInfo(name = "reachable")
    val reachable: Int,  // 0 or 1
    
    @ColumnInfo(name = "last_seen")
    val lastSeen: Long,  // Unix timestamp (seconds)
    
    @ColumnInfo(name = "ttl")
    val ttl: Long,
    
    @ColumnInfo(name = "source")
    val source: String?,
    
    @ColumnInfo(name = "node_id")
    val nodeId: String?,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)
```

**Schema Consistency**: Both implementations use identical SQL DDL, ensuring cross-platform compatibility. Room's `@PrimaryKey(autoGenerate = true)` maps to SQLite's `INTEGER PRIMARY KEY AUTOINCREMENT`.

---

## Migration Strategy

### Version 1 → Version 2 (Add nodes table)

**Rust (core/src/storage.rs)**:
```rust
// Add to SCHEMA_SQL constant
CREATE TABLE IF NOT EXISTS nodes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    address TEXT NOT NULL,
    type TEXT NOT NULL,
    reachable INTEGER NOT NULL,
    last_seen INTEGER NOT NULL,
    ttl INTEGER NOT NULL,
    source TEXT,
    node_id TEXT,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_nodes_address ON nodes(address);
CREATE INDEX IF NOT EXISTS idx_nodes_last_seen ON nodes(last_seen);
CREATE INDEX IF NOT EXISTS idx_nodes_type ON nodes(type);
CREATE INDEX IF NOT EXISTS idx_nodes_reachable ON nodes(reachable);
```

**Android (Room Migration)**:
```kotlin
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS nodes (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                address TEXT NOT NULL,
                type TEXT NOT NULL,
                reachable INTEGER NOT NULL,
                last_seen INTEGER NOT NULL,
                ttl INTEGER NOT NULL,
                source TEXT,
                node_id TEXT,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            )
        """.trimIndent())
        database.execSQL("CREATE INDEX IF NOT EXISTS idx_nodes_address ON nodes(address)")
        database.execSQL("CREATE INDEX IF NOT EXISTS idx_nodes_last_seen ON nodes(last_seen)")
        database.execSQL("CREATE INDEX IF NOT EXISTS idx_nodes_type ON nodes(type)")
        database.execSQL("CREATE INDEX IF NOT EXISTS idx_nodes_reachable ON nodes(reachable)")
    }
}
```

**Validation**: After migration, run schema validation to ensure all modules can read the same database.

---

## Constraints and Business Rules

1. **Uniqueness**: `address` should be unique per `type` (same address can exist as both LAN and GLOBAL)
2. **TTL Minimums**: 
   - LAN: 60 seconds minimum
   - Wi-Fi: 120 seconds minimum
   - Global: 300 seconds minimum
3. **Reachability**: Nodes marked `reachable = 0` for more than `ttl/2` are candidates for deletion
4. **Merge Priority**: Global > Wi-Fi > LAN (when same address from multiple sources)
5. **Node Identity**: If `node_id` is present, it must be a valid ed25519 public key (64 hex chars)

---

## Performance Considerations

- **Indexes**: Address, last_seen, type, and reachable are indexed for fast queries
- **Cleanup Frequency**: Run cleanup less frequently than TTL to avoid premature deletion
- **Batch Operations**: Merge operations should batch DB writes for efficiency
- **Async Health Checks**: Health checks run in background to avoid blocking discovery

