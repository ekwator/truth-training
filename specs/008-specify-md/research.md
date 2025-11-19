# Research: Node Discovery & Address Exchange

**Feature**: Unified Cross-Platform Node Discovery  
**Date**: 2025-11-17  
**Status**: Complete

## Research Questions

### 1. SQLite Schema Design for Cross-Platform Compatibility

**Question**: How to ensure SQLite schema created by Rust (rusqlite) is readable by Android (Room) and vice versa?

**Decision**: Use canonical SQL DDL with explicit column types, avoid platform-specific features, and ensure AUTOINCREMENT is consistent.

**Rationale**:
- SQLite is cross-platform by design, but schema differences can cause issues
- Room requires `@PrimaryKey(autoGenerate = true)` which maps to `INTEGER PRIMARY KEY AUTOINCREMENT`
- rusqlite supports the same syntax
- Use `INTEGER` (not `INT`) for primary keys to ensure compatibility
- Avoid SQLite extensions that aren't available on all platforms

**Alternatives Considered**:
- Separate schemas per platform: Rejected - defeats purpose of unified system
- JSON-based storage: Rejected - loses query performance and ACID guarantees
- Protocol Buffers: Rejected - adds complexity, SQLite is sufficient

**References**:
- SQLite documentation: INTEGER PRIMARY KEY AUTOINCREMENT
- Room documentation: Primary Keys
- rusqlite documentation: Schema creation

---

### 2. LAN/Wi-Fi Discovery Protocols

**Question**: What protocol should be used for local network discovery (LAN/Wi-Fi)?

**Decision**: Use UDP multicast (IPv4: 224.0.0.0-239.255.255.255) with a custom application protocol for node advertisement and discovery.

**Rationale**:
- UDP multicast is supported on all platforms (Rust tokio, Android, Desktop)
- Low overhead, works on local networks without central server
- Can be implemented with standard socket APIs
- Alternative: mDNS/Bonjour (avahi, zeroconf) - more complex, requires additional dependencies
- Alternative: WebRTC - overkill for simple discovery, adds browser dependencies

**Protocol Design**:
- Multicast group: `224.0.0.251` (reserved for local use) or custom port
- Advertisement packet: JSON with node_id, address, type, timestamp, signature
- Discovery: Broadcast "HELLO" packet, nodes respond with advertisement
- Frequency: Every 30-60 seconds (configurable)

**Alternatives Considered**:
- mDNS/Bonjour: Rejected - requires platform-specific libraries (avahi on Linux, Bonjour on macOS)
- Bluetooth Low Energy: Rejected - limited range, platform-specific APIs
- HTTP polling: Rejected - requires known endpoints, doesn't scale

**References**:
- RFC 1112: Host Extensions for IP Multicasting
- Tokio UDP multicast examples
- Android NetworkServiceDiscovery API (alternative, but more complex)

---

### 3. TTL and Cleanup Strategies

**Question**: How to determine TTL values and cleanup frequency for different node types?

**Decision**: 
- LAN nodes: TTL = 120 seconds (2 minutes), cleanup every 60 seconds
- Wi-Fi nodes: TTL = 300 seconds (5 minutes), cleanup every 120 seconds
- Global nodes: TTL = 3600 seconds (1 hour), cleanup every 600 seconds (10 minutes)

**Rationale**:
- LAN nodes are ephemeral, devices join/leave frequently
- Wi-Fi nodes are more stable but still local
- Global nodes are persistent, change infrequently
- Cleanup frequency should be less than TTL to avoid premature removal
- Use `last_seen` timestamp (Unix epoch seconds) for TTL calculation

**Cleanup Algorithm**:
```sql
DELETE FROM nodes 
WHERE (strftime('%s', 'now') - last_seen) > ttl 
   OR (reachable = 0 AND (strftime('%s', 'now') - last_seen) > (ttl / 2));
```

**Alternatives Considered**:
- Single TTL for all types: Rejected - global nodes shouldn't expire as quickly as LAN
- Exponential backoff: Rejected - adds complexity, simple TTL is sufficient
- Manual cleanup only: Rejected - stale nodes accumulate, degrades performance

**References**:
- Common patterns in service discovery (Consul, etcd use similar TTL strategies)
- Android WorkManager best practices for periodic tasks

---

### 4. Merge Conflict Resolution

**Question**: How to resolve conflicts when the same node appears from multiple sources (LAN/Wi-Fi vs Global)?

**Decision**: Prefer local sources over global:
1. Priority order: LAN/Wi-Fi > Global
2. If same priority: Use most recent `last_seen`
3. If timestamps equal: Use lexicographic comparison of `address`

**Rationale**:
- Local nodes provide freshest addresses for peers on the same network segment and should override cached global registry entries.
- Deterministic rules ensure all modules converge on identical results while still allowing global data to backfill when local info is absent.
- Prevents stale global entries from overriding live local advertisements.

**Merge Algorithm**:
```rust
fn merge_nodes(local: Vec<Node>, remote: Vec<Node>) -> Vec<Node> {
    let mut merged = HashMap::new();
    for node in local {
        merged.insert(node.address.clone(), node);
    }
    for node in remote {
        let replace = should_replace(merged.get(&node.address), &node);
        if replace {
            merged.insert(node.address.clone(), node);
        }
    }
    merged.into_values().collect()
}

fn should_replace(existing: Option<&Node>, candidate: &Node) -> bool {
    match existing {
        None => true,
        Some(current) => {
            priority(candidate) > priority(current)
                || (priority(candidate) == priority(current)
                    && candidate.last_seen > current.last_seen)
                || (priority(candidate) == priority(current)
                    && candidate.last_seen == current.last_seen
                    && candidate.address < current.address)
        }
    }
}
```
*(where `priority` maps LAN/Wi-Fi → 2, Global → 1)*

**Alternatives Considered**:
- Always prefer global: Rejected – could mask fresher LAN info and delay local sync.
- Weighted scoring: Rejected – adds complexity without additional benefit after deterministic rule.
- Manual conflict resolution: Rejected – would require operator intervention, blocking automation.

**References**:
- CRDT (Conflict-free Replicated Data Types) merge strategies
- Distributed systems: eventual consistency patterns

---

### 5. Periodic Sync Patterns

**Question**: How to implement periodic discovery and sync across platforms (Rust Tokio, Android WorkManager)?

**Decision**: 
- Rust (CLI/Server/Desktop): Use `tokio::time::interval` for periodic tasks
- Android: Use WorkManager with `PeriodicWorkRequest` (minimum 15 minutes, use custom shorter intervals via chaining)
- Desktop UI: Use Tauri background tasks or Electron main process timers

**Rationale**:
- Tokio intervals are efficient, non-blocking, and integrate with async runtime
- WorkManager handles Android battery optimization and background restrictions
- PeriodicWorkRequest minimum is 15 minutes, but we can chain shorter intervals
- Alternative: AlarmManager - deprecated, WorkManager is recommended

**Implementation**:
- Rust: `tokio::spawn(async move { loop { discover_nodes().await; interval.tick().await; } })`
- Android: `PeriodicWorkRequest.Builder` with constraints (network required, battery not low)
- Desktop: Tauri command with `setInterval` or Rust background task

**Alternatives Considered**:
- Push notifications: Rejected - requires central server, adds complexity
- WebSocket keepalive: Rejected - overkill for discovery, adds connection overhead
- Manual refresh only: Rejected - doesn't meet "automatic" requirement

**References**:
- Tokio documentation: `tokio::time::interval`
- Android WorkManager guide: Periodic Work
- Tauri documentation: Background Tasks

---

### 6. Reachability Validation

**Question**: How to validate node reachability without blocking discovery?

**Decision**: Use async HTTP health check (GET /health or custom endpoint) with timeout (2-5 seconds). Run validation in background, update `reachable` flag asynchronously.

**Rationale**:
- HTTP health checks are standard, work across all platforms
- Async prevents blocking discovery cycle
- Timeout prevents hanging on unreachable nodes
- Can use existing server diagnostics endpoint if available

**Validation Strategy**:
- During discovery: Mark nodes as `reachable = 1` initially (optimistic)
- Background validation: Spawn async tasks to check reachability
- Update DB: Set `reachable = 0` if health check fails, `reachable = 1` if succeeds
- Retry logic: Retry failed nodes with exponential backoff (max 3 retries)

**Alternatives Considered**:
- ICMP ping: Rejected - requires root/admin on some platforms, blocked by firewalls
- TCP connection test: Rejected - doesn't verify application is running
- No validation: Rejected - stale nodes accumulate, degrades performance

**References**:
- HTTP health check patterns (Kubernetes, Consul)
- Async HTTP client best practices (reqwest, OkHttp)

---

## Summary of Decisions

1. **Schema**: Canonical SQLite DDL with INTEGER PRIMARY KEY AUTOINCREMENT, cross-platform compatible
2. **Discovery**: UDP multicast for LAN/Wi-Fi, HTTP polling for global endpoints
3. **TTL**: Type-specific (LAN: 2min, Wi-Fi: 5min, Global: 1hr) with periodic cleanup
4. **Merge**: Priority Local (LAN/Wi-Fi) > Global with deterministic tie-breaking
5. **Sync**: Platform-specific periodic tasks (Tokio intervals, WorkManager, Tauri tasks)
6. **Reachability**: Async HTTP health checks with timeout and retry logic

**All NEEDS CLARIFICATION markers resolved**: ✅

