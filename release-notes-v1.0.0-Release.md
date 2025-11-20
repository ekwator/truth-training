## v1.0.0-Release — Unified Discovery & Sync Release

**Cross-Platform Node Discovery System**

This release delivers a fully unified cross-platform node discovery and synchronization system across Rust Desktop (Tauri), CLI (truthctl), Android, and Server platforms. All components now share identical discovery protocols, JSON schemas, SQLite database schemas, and synchronization semantics.

### Major Features

#### Unified Cross-Platform Node Discovery
- Identical discovery protocols across Desktop, CLI, Server, and Android
- Unified data formats with shared JSON schemas and SQLite database schemas
- Conflict-free merge logic for node deduplication
- Seamless network view across all platforms

#### LAN/Wi-Fi UDP Multicast Support
- Standard UDP multicast address (239.255.0.1:52525) across all platforms
- Unified packet format (LanAnnouncement JSON) with ed25519 signature verification
- Cross-platform compatibility verified between Desktop (Rust) and Android (Kotlin)
- Background discovery workers for Desktop (Tauri/Tokio) and Android (WorkManager)

#### Global Registry Polling
- HTTPS registry polling with consistent JSON payload formats
- Supports both envelope (`{nodes: [...]}`) and array response formats
- Unified error handling and retry logic across all platforms
- Configurable registry URLs with graceful handling of empty/malformed URLs

#### Node Reachability & TTL Cleanup Logic
- HTTP reachability checks with unified timeout and retry logic
- TTL-driven automatic cleanup of stale/unreachable nodes
- Consistent TTL rules and cleanup semantics across all platforms
- Deterministic merge rules for node deduplication (Local > Global, then last_seen, then lexicographic address)

#### Android Integration (Room DB, Worker, UI)
- **Room Database**: Complete `NodeEntity` and `NodeDao` with canonical SQLite schema matching Desktop/CLI
- **WorkManager**: `NodeSyncWorker` for periodic background sync (every 15 minutes)
- **Compose UI**: `NodesScreen` with node list, filters, and manual actions (refresh, discover, cleanup, health check)
- **Discovery Repository**: High-level discovery operations with TTL logic and deduplication
- **UDP Multicast Client**: `LanDiscoveryClient` for local network discovery
- Full integration into `MainNavigation.kt` for seamless user access

#### Desktop/CLI/Server Integration
- **Desktop**: Tauri backend worker (`DiscoveryManager`) with settings persistence, React UI (`NodesPanel`)
- **CLI**: Complete `truthctl nodes ...` command suite (list/add/remove/discover/sync/cleanup/health-check/validate)
- **Server**: HTTP API endpoints, background schedulers, global registry polling
- **Settings Persistence**: Discovery intervals, TTL, and registry URL configuration across all platforms

#### Real-Device E2E Testing
- Cross-device E2E tests (Linux Desktop ↔ Android device, CLI ↔ Android ↔ Desktop)
- All 107 Android instrumentation tests pass on real device
- UDP multicast compatibility verified via packet roundtrip tests
- Full test suite executed on physical Android device (RMX3261 - 11)

### Data Format Unification

#### Unified JSON Schemas
- Identical JSON enum encoding (NodeType: UPPERCASE, NodeSource: snake_case)
- Case-insensitive deserialization supported
- Compatible packet exchange between Desktop ↔ Android

#### Unified Database Schema
- Canonical SQLite schema with identical table structure, indices, and constraints
- Android migration `MIGRATION_2_3` creates `nodes` table matching Desktop/CLI schema
- Same indices: `address`, `last_seen`, `type`, `reachable`
- Compatible migrations across all platforms

#### Synchronization System
- Peer-to-peer incremental sync via `/api/v1/nodes/sync`
- Bidirectional sync with conflict resolution
- Manual sync triggers via CLI and UI
- Sync statistics and error handling

### Quality & Validation

#### Testing Coverage
- Comprehensive CLI E2E test suite (`tests/cli_sync.rs`)
- TTL behavior consistency tests (`tests/integration/test_ttl_consistency.rs`)
- JSON enum serialization tests (`tests/integration/test_json_enum_serialization.rs`)
- Android instrumentation tests (NodeDaoTest, NodeDiscoveryTest, NodeSyncWorkerTest)
- Cross-platform compatibility tests (Desktop ↔ Android format conversion)

#### Code Quality
- Zero Rust compiler warnings across all modules
- Complete documentation (architecture, compatibility, navigation, device testing)
- Structured logging and metrics for observability
- Discovery event counters (nodes discovered, updated, pruned)

### Breaking Changes

**None** - This release is fully backward compatible with previous v1.0.0 baseline releases.

### Version Bump

All crates and components aligned to v1.0.0-Release:
- `core_lib`: v1.0.0
- `truth_core`: v1.0.0
- `app` (truthctl): v1.0.0
- `truth-ui-desktop`: v1.0.0
- `truth-android-client`: v1.0.0

### Full Changelog

See [CHANGELOG.md](https://github.com/ekwator/truth-training/blob/master/CHANGELOG.md#100-release--unified-discovery--sync-release) for complete details.

