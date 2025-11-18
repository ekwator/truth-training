# Quickstart: Node Discovery & Address Exchange

**Feature**: Unified Cross-Platform Node Discovery  
**Date**: 2025-11-17

This quickstart guide validates the node discovery system through end-to-end scenarios.

## Prerequisites

- Rust toolchain (1.75+)
- SQLite 3.x
- Android SDK (for Android testing)
- Node.js (for Desktop UI testing)

## Scenario 1: Schema Validation Across Modules

**Goal**: Verify that all modules (CLI, Server, Desktop, Android) can read/write the same SQLite database.

### Steps

1. **Create database with CLI**:
   ```bash
   cd /path/to/truth-training
   cargo run --bin truthctl -- db init --db test_nodes.db
   cargo run --bin truthctl -- db migrate --db test_nodes.db
   ```

2. **Verify schema**:
   ```bash
   cargo run --bin truthctl -- db schema --db test_nodes.db | grep -A 20 "CREATE TABLE.*nodes"
   ```

3. **Insert test node via CLI**:
   ```bash
   cargo run --bin truthctl -- nodes add \
     --address "http://192.168.1.100:8080" \
     --type LAN \
     --ttl 120 \
     --db test_nodes.db
   ```

4. **Verify node via Server API**:
   ```bash
   # Start server with same DB
   cargo run --bin truth_core_server -- --db test_nodes.db --port 8080
   
   # Query nodes
   curl http://localhost:8080/api/v1/nodes
   ```

5. **Verify node via Desktop UI**:
   - Open Desktop UI
   - Connect to `test_nodes.db`
   - Navigate to "Nodes" view
   - Verify test node appears

6. **Verify node via Android**:
   - Copy `test_nodes.db` to Android device
   - Open Android app
   - Navigate to "Nodes" screen
   - Verify test node appears

**Expected Result**: All modules can read the same node record without errors.

---

## Scenario 2: LAN Discovery Cycle

**Goal**: Verify that nodes discover each other on local network via UDP multicast.

### Steps

1. **Start Server A** (Node 1):
   ```bash
   cargo run --bin truth_core_server -- --db node1.db --port 8080 --bind 0.0.0.0
   ```

2. **Start Server B** (Node 2):
   ```bash
   cargo run --bin truth_core_server -- --db node2.db --port 8081 --bind 0.0.0.0
   ```

3. **Trigger discovery on Node 1**:
   ```bash
   curl -X POST http://localhost:8080/api/v1/nodes/discover \
     -H "Content-Type: application/json" \
     -d '{"types": ["LAN"]}'
   ```

4. **Wait 5 seconds** (discovery cycle completes)

5. **Check Node 1's node list**:
   ```bash
   curl http://localhost:8080/api/v1/nodes?type=LAN
   ```

6. **Check Node 2's node list**:
   ```bash
   curl http://localhost:8081/api/v1/nodes?type=LAN
   ```

**Expected Result**: Both nodes discover each other and appear in each other's node lists with `type=LAN` and `source=local_broadcast`.

---

## Scenario 3: Node Synchronization (Handshake)

**Goal**: Verify that nodes exchange and merge node lists during handshake.

### Steps

1. **Prepare Node 1** (add known nodes):
   ```bash
   cargo run --bin truthctl -- nodes add \
     --address "http://192.168.1.100:8080" \
     --type GLOBAL \
     --ttl 3600 \
     --db node1.db
   ```

2. **Prepare Node 2** (add different known nodes):
   ```bash
   cargo run --bin truthctl -- nodes add \
     --address "http://192.168.1.200:8080" \
     --type GLOBAL \
     --ttl 3600 \
     --db node2.db
   ```

3. **Sync Node 2's list to Node 1**:
   ```bash
   # Get Node 2's node list
   NODE2_LIST=$(curl -s http://localhost:8081/api/v1/nodes)
   
   # Send to Node 1 for sync
   curl -X POST http://localhost:8080/api/v1/nodes/sync \
     -H "Content-Type: application/json" \
     -d "{\"nodes\": $NODE2_LIST}"
   ```

4. **Verify merged list on Node 1**:
   ```bash
   curl http://localhost:8080/api/v1/nodes
   ```

5. **Get merged list returned by Node 1** (should include Node 1's original nodes):
   ```bash
   # The sync response contains merged list
   curl -X POST http://localhost:8080/api/v1/nodes/sync \
     -H "Content-Type: application/json" \
     -d "{\"nodes\": $NODE2_LIST}" | jq '.merged'
   ```

**Expected Result**: 
- Node 1's database contains both its original nodes and Node 2's nodes
- Sync response returns merged list including Node 1's nodes (for Node 2 to update)

---

## Scenario 4: TTL-Based Cleanup

**Goal**: Verify that stale nodes are automatically removed after TTL expires.

### Steps

1. **Add node with short TTL**:
   ```bash
   cargo run --bin truthctl -- nodes add \
     --address "http://192.168.1.100:8080" \
     --type LAN \
     --ttl 60 \
     --db test_cleanup.db
   ```

2. **Verify node exists**:
   ```bash
   cargo run --bin truthctl -- nodes list --db test_cleanup.db
   ```

3. **Manually set last_seen to past** (simulate expired node):
   ```bash
   sqlite3 test_cleanup.db \
     "UPDATE nodes SET last_seen = strftime('%s', 'now', '-120 seconds') WHERE address = 'http://192.168.1.100:8080'"
   ```

4. **Trigger cleanup**:
   ```bash
   cargo run --bin truthctl -- nodes cleanup --db test_cleanup.db
   ```

5. **Verify node removed**:
   ```bash
   cargo run --bin truthctl -- nodes list --db test_cleanup.db
   ```

**Expected Result**: Expired node is deleted from database.

---

## Scenario 5: Reachability Health Checks

**Goal**: Verify that nodes are marked as unreachable when health checks fail.

### Steps

1. **Add reachable node**:
   ```bash
   # Start test server
   cargo run --bin truth_core_server -- --db test_server.db --port 8090
   
   # Add node pointing to test server
   cargo run --bin truthctl -- nodes add \
     --address "http://localhost:8090" \
     --type LAN \
     --ttl 120 \
     --db test_health.db
   ```

2. **Trigger health check**:
   ```bash
   curl http://localhost:8090/api/v1/nodes/health
   ```

3. **Verify node marked reachable**:
   ```bash
   cargo run --bin truthctl -- nodes list --db test_health.db | grep "localhost:8090"
   # Should show reachable=1
   ```

4. **Stop test server**:
   ```bash
   # Kill server process
   ```

5. **Trigger health check again**:
   ```bash
   # Use API endpoint (if available) or wait for background check
   # Or manually trigger via CLI
   cargo run --bin truthctl -- nodes health-check --db test_health.db
   ```

6. **Verify node marked unreachable**:
   ```bash
   cargo run --bin truthctl -- nodes list --db test_health.db | grep "localhost:8090"
   # Should show reachable=0
   ```

**Expected Result**: Node reachability status updates based on health check results.

---

## Scenario 6: Cross-Platform Database Compatibility

**Goal**: Verify that Android Room database is readable by Rust CLI/Server.

### Steps

1. **Create database with Android app**:
   - Open Android app
   - Let Room create database with migrations
   - Add test node via UI

2. **Copy database to development machine**:
   ```bash
   adb pull /data/data/com.truth.training.client/databases/truth_database test_android.db
   ```

3. **Verify schema with CLI**:
   ```bash
   cargo run --bin truthctl -- db schema --db test_android.db | grep -A 20 "CREATE TABLE.*nodes"
   ```

4. **Query nodes with CLI**:
   ```bash
   cargo run --bin truthctl -- nodes list --db test_android.db
   ```

5. **Add node via CLI**:
   ```bash
   cargo run --bin truthctl -- nodes add \
     --address "http://192.168.1.50:8080" \
     --type WIFI \
     --ttl 300 \
     --db test_android.db
   ```

6. **Copy database back to Android**:
   ```bash
   adb push test_android.db /data/data/com.truth.training.client/databases/truth_database
   ```

7. **Verify node appears in Android app**

**Expected Result**: Database created by Android is fully compatible with Rust modules, and vice versa.

---

## Scenario 7: Merge Conflict Resolution

**Goal**: Verify that merge logic correctly handles nodes from multiple sources.

### Steps

1. **Add same address as LAN on Node 1**:
   ```bash
   cargo run --bin truthctl -- nodes add \
     --address "http://192.168.1.100:8080" \
     --type LAN \
     --ttl 120 \
     --db node1_merge.db
   ```

2. **Add same address as GLOBAL on Node 2**:
   ```bash
   cargo run --bin truthctl -- nodes add \
     --address "http://192.168.1.100:8080" \
     --type GLOBAL \
     --ttl 3600 \
     --db node2_merge.db
   ```

3. **Sync Node 2 to Node 1**:
   ```bash
   NODE2_LIST=$(curl -s http://localhost:8082/api/v1/nodes)
   curl -X POST http://localhost:8081/api/v1/nodes/sync \
     -H "Content-Type: application/json" \
     -d "{\"nodes\": $NODE2_LIST}"
   ```

4. **Verify LAN node wins** (local priority):
   ```bash
   cargo run --bin truthctl -- nodes list --db node1_merge.db | grep "192.168.1.100"
   # Should show type=LAN (local record overrides global)
   ```

**Expected Result**: Merge logic applies priority rules (LAN/Wi-Fi > Global) so the local record remains authoritative and global data only backfills missing entries.

---

## Validation Checklist

After running all scenarios, verify:

- [ ] All modules can read/write same database
- [ ] LAN discovery finds nodes on local network
- [ ] Node synchronization merges lists correctly
- [ ] TTL cleanup removes expired nodes
- [ ] Health checks update reachability status
- [ ] Android database is compatible with Rust modules
- [ ] Merge conflict resolution follows priority rules
- [ ] No schema mismatches between modules
- [ ] All indexes are created correctly
- [ ] Migrations run successfully on all platforms

## Troubleshooting

### Database locked errors
- Ensure only one process accesses database at a time
- Use connection pooling in server
- Close database connections properly

### Discovery not finding nodes
- Check firewall settings (UDP multicast)
- Verify network interface is correct
- Check multicast group address and port

### Schema mismatches
- Run migrations on all modules
- Verify SQL DDL is identical across platforms
- Check AUTOINCREMENT vs autoGenerate mapping

### Health checks timing out
- Increase timeout values
- Verify server endpoints are accessible
- Check network connectivity

