# API Contracts: Node Discovery & Address Exchange

This directory contains OpenAPI 3.0 specifications for the Node Discovery API.

## Files

- `nodes-api.yaml`: Complete OpenAPI specification for node discovery, synchronization, and health check endpoints.

## Endpoints Summary

### Node Management
- `GET /nodes` - List all known nodes (with filters)
- `POST /nodes` - Register a new node manually
- `GET /nodes/{id}` - Get node by ID
- `PUT /nodes/{id}` - Update node
- `DELETE /nodes/{id}` - Delete node

### Discovery & Sync
- `POST /nodes/discover` - Trigger discovery cycle
- `POST /nodes/sync` - Synchronize node lists with peer

### Health Checks
- `GET /nodes/health` - Check reachability of nodes

## Contract Testing

Contract tests should be generated from this specification to ensure:
1. Request/response schemas match specification
2. Required fields are validated
3. Enum values are enforced
4. Error responses follow schema

See `tests/contract/` for generated contract tests.

## Usage

### Generate Client SDKs
```bash
# Using openapi-generator
openapi-generator generate -i contracts/nodes-api.yaml -g rust -o clients/rust
openapi-generator generate -i contracts/nodes-api.yaml -g kotlin -o clients/kotlin
openapi-generator generate -i contracts/nodes-api.yaml -g typescript -o clients/typescript
```

### Validate Specification
```bash
# Using swagger-cli
swagger-cli validate contracts/nodes-api.yaml
```

### View Documentation
```bash
# Using swagger-ui
docker run -p 8080:8080 -e SWAGGER_JSON=/specs/nodes-api.yaml -v $(pwd)/contracts:/specs swaggerapi/swagger-ui
```

## Sync Handshake Contract

**Task T050**: This section documents the cross-platform sync handshake protocol used by Desktop, Server, CLI, and Android.

### Overview

The sync handshake ensures that all platforms (Desktop/Tauri, CLI, Server, Android) converge to the same merged node inventory using deterministic merge rules. The handshake uses the `merge_node_lists` helper function from `core/src/sync.rs` to ensure consistency.

### Protocol Flow

1. **Client Initiates Sync**: Client (CLI, Desktop, or Android) sends a `POST /api/v1/nodes/sync` request with its local node list.

2. **Server Merges**: Server receives the request, loads its local nodes, and calls `merge_node_lists(local_nodes, incoming_nodes)` from `core/src/sync.rs`.

3. **Server Persists**: Server updates its database with the merged result using `storage::upsert_node_by_address()`.

4. **Server Responds**: Server returns the merged list, along with counts of `local_added` and `local_updated` nodes.

5. **Client Updates**: Client receives the merged list and updates its local database accordingly.

### Merge Rules (Deterministic)

The merge follows these priority rules (implemented in `core/src/sync.rs::should_replace()`):

1. **Type Priority**: Local sources (LAN, Wi-Fi) have higher priority than Global sources.
   - Priority order: `LAN > WIFI > GLOBAL > RELAY > CLIENT`
   - A LAN node will **never** be replaced by a Global node, even if the Global node has a newer `last_seen` timestamp.

2. **Timestamp Tiebreaker**: For nodes with the same type priority, the node with the newer `last_seen` timestamp wins.

3. **Address Tiebreaker**: For nodes with the same type and `last_seen`, lexicographic ordering of `address` is used (deterministic but arbitrary).

4. **New Nodes**: Nodes with addresses not present in the local database are always added.

### Request Format

```json
POST /api/v1/nodes/sync
Content-Type: application/json

{
  "nodes": [
    {
      "address": "http://192.168.1.100:8080",
      "type": "LAN",
      "reachable": true,
      "last_seen": 1000,
      "ttl": 120,
      "source": "local_broadcast",
      "node_id": "abc123..."
    }
  ]
}
```

### Response Format

```json
{
  "merged": [
    {
      "id": 1,
      "address": "http://192.168.1.100:8080",
      "type": "LAN",
      "reachable": true,
      "last_seen": 1000,
      "ttl": 120,
      "source": "local_broadcast",
      "node_id": "abc123...",
      "created_at": 1000,
      "updated_at": 1000
    }
  ],
  "local_added": 1,
  "local_updated": 0
}
```

### Implementation Details

- **Server Endpoint**: `src/api.rs::sync_nodes_http()` calls `merge_node_lists()` from `core/src/sync.rs`.
- **CLI Command**: `app/src/bin/truthctl.rs::NodesCmd::Sync` sends nodes to `/api/v1/nodes/sync`.
- **Desktop**: Tauri backend can call the same endpoint or use local merge helpers.
- **Android**: `DiscoveryRepository` can call `/api/v1/nodes/sync` or implement local merge logic matching `merge_node_lists()`.

### Cross-Platform Compatibility

All platforms must use the same merge logic to ensure convergence:

- **Rust (Server/CLI/Desktop)**: Uses `core/src/sync.rs::merge_node_lists()`.
- **Android (Kotlin)**: Should implement equivalent logic in `DiscoveryRepository` or call the server endpoint.
- **JavaScript (Desktop UI)**: Can call Tauri commands that use Rust merge helpers.

### Testing

See `tests/integration/test_sync_handshake.rs` for:
- Verification that server uses `merge_node_lists()` correctly.
- Tests for merge priority rules (Local > Global).
- Tests for `last_seen` tiebreaker.
- Cross-platform convergence verification.

### Example Scenario

**Initial State**:
- **Server DB**: Has node `http://192.168.1.100:8080` with type `LAN`, `last_seen=1000`.
- **Client DB**: Has same address with type `GLOBAL`, `last_seen=2000`.

**After Sync**:
- Server merges: LAN (priority 1) vs GLOBAL (priority 3) → LAN wins.
- Result: Node remains as `LAN` with `last_seen=1000` (not updated to 2000).
- Client receives merged list and updates its database to match.

This ensures all platforms converge to the same state: `LAN` node with `last_seen=1000`.

_Version: v1.0.0_
