# Changelog

## [1.0.0-Release] — Unified Discovery & Sync Release

**Release Date**: 2025-01-XX  
**Status**: Stable Production Release

### 🎉 Cross-Platform Discovery Milestone

This release delivers a fully unified cross-platform node discovery and synchronization system across Rust Desktop (Tauri), CLI (truthctl), Android, and Server platforms. All components now share identical discovery protocols, JSON schemas, SQLite database schemas, and synchronization semantics.

### 🔍 Unified Node Discovery System

#### LAN/Wi-Fi UDP Multicast Discovery
- **Standard Multicast Address**: 239.255.0.1:52525 across all platforms
- **Unified Packet Format**: Identical JSON structure (LanAnnouncement) with ed25519 signature verification
- **Cross-Platform Compatibility**: Verified packet exchange between Desktop (Rust) ↔ Android (Kotlin)
- **Background Workers**: 
  - Desktop: Tokio interval-based worker in `ui/desktop/src-tauri/src/discovery.rs`
  - Android: WorkManager periodic sync every 15 minutes
  - Server: Background schedulers with configurable intervals

#### Global Registry Polling
- **HTTPS Polling**: Consistent JSON payload formats across all platforms
- **Flexible Response Formats**: Supports both envelope (`{nodes: [...]}`) and array formats
- **Error Handling**: Graceful handling of empty/malformed registry URLs
- **Configurable URLs**: Registry URL configuration with default fallback values

#### Node Reachability & TTL Cleanup
- **HTTP Reachability Checks**: Unified timeout and retry logic across all platforms
- **TTL-Based Cleanup**: Automatic removal of stale/unreachable nodes
- **Consistent TTL Rules**: Same defaults and cleanup logic across Desktop, CLI, Server, and Android
- **Deterministic Merge Rules**: Node deduplication (Local > Global, then last_seen, then lexicographic address)

### 📱 Android Integration

#### Room Database Integration
- **NodeEntity & NodeDao**: Complete Room database integration with canonical SQLite schema
- **Migration MIGRATION_2_3**: Creates `nodes` table matching Desktop/CLI schema
- **Full CRUD Operations**: Upsert, TTL queries, and cleanup operations
- **Android Instrumentation Tests**: NodeDaoTest.kt with comprehensive test coverage

#### Discovery Repository
- **High-Level Operations**: Global registry polling (HTTP GET) and UDP/LAN discovery integration
- **TTL Logic & Deduplication**: Consistent with Desktop/CLI implementations
- **JSON Format Conversion**: Helpers for cross-platform compatibility
- **Cross-Platform Tests**: Compatibility tests (Desktop ↔ Android format conversion)

#### WorkManager Background Sync
- **NodeSyncWorker**: Periodic sync every 15 minutes
- **TTL Decrement & Cleanup**: Automatic node lifecycle management
- **Reachability Checks**: HTTP health checks for discovered nodes
- **Database Updates**: Updates via DiscoveryRepository
- **Worker Lifecycle Tests**: NodeSyncWorkerTest.kt

#### UDP Multicast LAN Discovery Client
- **LanDiscoveryClient**: Standard multicast address (239.255.0.1:52525)
- **Node Broadcasting**: Android node identity/address broadcasting
- **Listener Implementation**: Receives announcements from other nodes
- **Payload Conversion**: Converts to NodeRecord format
- **Signature Verification**: ed25519 via Ed25519CryptoManager

#### Compose UI Integration
- **NodesScreen.kt**: Complete node list UI with filters and actions
- **NodesViewModel.kt**: State management for discovery operations
- **Manual Actions**: Refresh, discover, cleanup, and health check buttons
- **Navigation Integration**: Integrated into MainNavigation.kt

### 🖥️ Desktop Integration

#### Background Discovery Worker
- **DiscoveryManager**: Lifecycle management (start/stop/restart) with clean shutdown
- **Tokio Integration**: Interval-based worker in `ui/desktop/src-tauri/src/discovery.rs`
- **Drop Trait**: Clean shutdown implementation
- **Core Bridge**: Integration with core discovery functions

#### React UI (NodesPanel)
- **Node List Display**: Reachability badges and TTL countdown indicators
- **Manual Actions**: Refresh, discover, cleanup, and health check buttons
- **Tauri Commands**: Integration with Tauri backend commands

#### Settings Persistence
- **Settings Storage**: Discovery settings in `ui/desktop/src-tauri/src/settings.rs`
- **Configurable Options**: Interval, TTL, and registry URL configuration
- **Settings UI**: Integration into Dashboard and Settings pages
- **Default Values**: Fallback values for out-of-the-box operation

### 💻 CLI Integration

#### Complete Command Suite
- **`truthctl nodes list`**: Table and JSON output formats with filtering (type, reachability, source)
- **`truthctl nodes add/remove`**: Manual node addition with validation, removal by address or node_id
- **`truthctl nodes discover`**: Local (LAN/Wi-Fi) and global registry discovery with type filtering
- **`truthctl nodes sync`**: Incremental sync with peer servers, bidirectional sync support
- **`truthctl nodes cleanup`**: TTL-based node pruning
- **`truthctl nodes health-check`**: Reachability verification
- **`truthctl nodes validate`**: Schema parity verification and migration status check

### 🔄 Synchronization System

#### Peer-to-Peer Sync
- **Incremental Sync**: Via `/api/v1/nodes/sync` endpoint
- **Bidirectional Sync**: Conflict resolution with deterministic merge rules
- **Manual Triggers**: Sync initiation via CLI and UI
- **Sync Statistics**: Detailed statistics and error handling

### 🧪 Testing & Quality Assurance

#### Comprehensive Test Coverage
- **CLI E2E Tests**: Complete test suite in `tests/cli_sync.rs` (list/discover/sync/cleanup/validate commands)
- **TTL Consistency Tests**: `tests/integration/test_ttl_consistency.rs` (TTL defaults, minimum enforcement, cleanup rules)
- **JSON Enum Serialization Tests**: `tests/integration/test_json_enum_serialization.rs` (NodeType/NodeSource encoding)
- **Android Instrumentation Tests**: NodeDaoTest, NodeDiscoveryTest, NodeSyncWorkerTest
- **Cross-Platform Compatibility Tests**: Desktop ↔ Android format conversion verification

#### Real-Device E2E Testing
- **Cross-Device Tests**: Linux Desktop ↔ Android device, CLI ↔ Android ↔ Desktop
- **Device Testing**: All 107 Android instrumentation tests pass on real device (RMX3261 - 11)
- **UDP Multicast Compatibility**: Verified via `tests/integration/test_udp_multicast_compatibility.rs`
- **Packet Roundtrip Tests**: Full packet format compatibility confirmed between Rust and Kotlin

#### Code Quality
- **Zero Compiler Warnings**: All Rust modules clean
- **Structured Logging**: Discovery event counters (nodes discovered, updated, pruned)
- **Observability**: TTL cleanup statistics, reachability check results, global registry polling metrics

### 📚 Documentation

- **Architecture Documentation**: [docs/android_discovery_architecture.md](docs/android_discovery_architecture.md) (navigation and device testing)
- **Compatibility Documentation**: [docs/cross_platform_discovery_compatibility.md](docs/cross_platform_discovery_compatibility.md) (full implementation status)
- **Post-Integration Hardening**: [docs/post_integration_hardening.md](docs/post_integration_hardening.md) (complete hardening phase)
- **CLI Usage**: [docs/CLI_Usage.md](docs/CLI_Usage.md) (command reference)
- **README Updates**: Added "Running Tests" section with all platform instructions

### 🔧 Bug Fixes & Improvements

- **Android Navigation**: Fixed integration (NodesScreen now accessible via navigation)
- **Test Feature Flags**: Fixed `lan_announcement_roundtrip` test feature flag gating
- **Compiler Warnings**: Fixed unused variable warnings in `src/p2p/node.rs`
- **Error Handling**: Improved handling for empty/malformed registry URLs
- **Logging**: Enhanced structured logging across all discovery components

### 📦 Version Information

All components aligned to v1.0.0-Release:
- `core_lib`: v1.0.0
- `truth_core`: v1.0.0
- `app` (truthctl): v1.0.0
- `truth-ui-desktop`: v1.0.0
- `truth-android-client`: v1.0.0

### 🔗 References

- **Cross-Platform Compatibility**: [docs/cross_platform_discovery_compatibility.md](docs/cross_platform_discovery_compatibility.md)
- **Android Architecture**: [docs/android_discovery_architecture.md](docs/android_discovery_architecture.md)
- **Post-Integration Hardening**: [docs/post_integration_hardening.md](docs/post_integration_hardening.md)
- **CLI Usage**: [docs/CLI_Usage.md](docs/CLI_Usage.md)

---

## [1.0.0] — First Stable Baseline — Cross-Platform Unified Release

**Release Date**: 2025-11-02  
**Status**: Stable Production Release

### 🎉 Cross-Platform Milestone

This release represents the first unified stable version across all platforms:
- ✅ **Core/Server/CLI**: v1.0.0 (stable)
- ✅ **Desktop UI**: v1.0.0 (stable)
- ✅ **Android Client**: v1.0.0 (stable) — **NEW**

### 📱 Android Client v1.0.0 (New)

The Android client has been completely rewritten to achieve full feature parity with Desktop v1.0.0:

- **Offline-First Architecture**: Room database with automatic background sync
- **Jetpack Compose UI**: Modern Material 3 design with 9 complete screens
- **96% Test Coverage**: Comprehensive unit, integration, and performance tests
- **Performance Targets Met**: All benchmarks under thresholds
- **Full Feature Parity**: Events, Context Templates, Judgments, Impacts, P2P sync

See [truth-android-client/CHANGELOG.md](truth-android-client/CHANGELOG.md) for detailed Android-specific changes.

### 🖥️ Desktop UI & Core (v1.0.0)

## [1.0.0-desktop] — Desktop UI — Context Fields Embedded
- **Breaking Change**: Removed `context_id` foreign key from events; embedded context fields directly in events
- **Data Model Refactor**:
  - Events now store `category_id`, `forma_id`, `cause_id`, `develop_id`, `effect_id` directly (nullable FK references)
  - Removed dependency on `context_id` foreign key lookup
  - Improved query performance (no JOINs required for context information)
- **Context Template System**:
  - New Context Editor UI screen for template management
  - Template selection auto-prefills event form fields
  - NULL-aware duplicate detection (compares only non-NULL fields)
  - NULL-aware template matching for display (consistent with duplicate detection)
  - "[Create Template]" button for events without matching templates
- **API Enhancements**:
  - Updated `POST /events` accepts embedded fields, rejects `context_id`
  - New `GET /contexts` endpoint for listing templates
  - New `POST /contexts` endpoint for creating templates with duplicate detection
  - New `GET /contexts/by-name/{name}` endpoint
  - New `POST /contexts/match` endpoint for template matching
  - New `POST /contexts/from-event` endpoint for creating templates from events
- **Validation & Data Integrity**:
  - Foreign key validation rejects invalid references immediately (400 error)
  - Duplicate template detection prevents creation (409 Conflict)
  - All FK references validated before persistence
- **UI Improvements**:
  - Context template dropdown selector on NewEvent page
  - Field prefilling from templates (modifiable before save)
  - Context name display in event list when template matched
  - New ContextEditor page for template creation and editing
- **Migration Notes**:
  - **BREAKING**: Manual database migration required (no automatic migrations)
  - Existing events with `context_id` must be migrated manually
  - Database schema changes: remove `context_id`, add five embedded fields
- **Version Bump**: All crates (core_lib, truth_core, app, desktop UI) bumped to v1.0.0

## [0.5.0] — Constitution Compliance (v2.1.0) — Truth Without Author
- **Constitution Alignment**: Full compliance with v2.1.0 principles implemented
- **Anonymous Confession Mode**: 
  - Plaintext-at-rest storage policy with UI warnings
  - CLI `confess` subcommand for anonymous event submission
  - Desktop UI toggle for confession mode with risk disclosure banner
- **Truth Without Author**:
  - Removed author metadata from `TruthEvent` model (legacy fields deprecated)
  - Transport envelope-based validation via `X-Public-Key`, `X-Signature`, `X-Timestamp` headers
  - Envelope signature verification middleware with anti-replay protection
- **Ternary Judgments**:
  - Updated judgment model to support `confirm`/`reject`/`abstain` signals
  - API endpoint `/api/v1/judgments` for ternary assessment submission
  - UI filter and statistics for judgment recognition types
- **Independent Confirmations**:
  - Anti-fraud enforcement via distinct envelope sender nodes
  - Node reputation tracking based on historical accuracy
  - Status weight and decay score computation for consensus
- **Wi‑Fi Direct / LAN-Based Nearby Sync**:
  - UDP broadcast discovery on port 35878 for local mesh nodes
  - Bidirectional HTTP sync with discovered peers
  - CLI flags `--nearby-sync` and `--nearby-interval-ms` for runtime control
  - Desktop UI settings panel for nearby sync configuration
  - Server-side persisted configuration that survives restarts
  - Dynamic start/stop via API endpoints `/api/v1/nearby_sync/start` and `/api/v1/nearby_sync/stop`
- **Core Library Enhancements**:
  - Weight computation (`status_weight`) and decay scoring (`decay_score`) algorithms
  - `NodeReputation` model for distributed trust tracking
  - `SyncEnvelope` model for P2P transport verification
- **CLI Improvements**:
  - `truthctl confess` command for anonymous confessions with plaintext warning
  - `truthctl judge` command for ternary judgment submission
- **Desktop UI Updates**:
  - Confession mode toggle on NewEvent page with plaintext-at-rest warning banner
  - "Confession" badge indicator on event cards
  - Recognition filter on Events page (confirm/reject/abstain)
  - Settings page controls for nearby sync (enable/disable, broadcast interval)
  - Ternary judgment assessment types throughout UI
- **API Enhancements**:
  - Anonymous event submission via `POST /events` (author fields forbidden)
  - Schema hardening with `#[serde(deny_unknown_fields)]` on requests
  - Configuration endpoints for nearby sync settings
- **Testing & Documentation**:
  - Contract tests for `/events` and `/judgments` endpoints
  - Integration tests for quickstart compliance scenarios
  - Constitution compliance documentation ([docs/Constitution-Compliance.md](docs/Constitution-Compliance.md))
- **Cross-Platform Fixes**:
  - Conditional compilation for `get_if_addrs` (excluded on Windows and Android)
  - Windows fallback for local IP detection
  - Android build compatibility improvements
  - Thread safety fixes for SQLite connection sharing across async tasks

## [0.4.2] — Desktop Integration & Cross-Platform Builds
- **Desktop UI**: Complete Tauri integration with React frontend
- **Cross-Platform Builds**: Production-ready packages for Linux (DEB/RPM), Windows (NSIS/MSI), macOS (DMG)
- **CI/CD Pipeline**: Automated builds for desktop, Android, and iOS platforms
- **Icon System**: Dynamic icon generation for all platforms (PNG/ICO formats)
- **Build System**: Comprehensive troubleshooting documentation and dependency management
- **Mobile Integration**: Android and iOS client frameworks with Rust FFI bridges
- **Documentation**: Complete build instructions and troubleshooting guides
- **Version Management**: Unified versioning across all components

## [0.4.0-pre] — Collective Intelligence Layer
- Introduced Semantic Correlation Layer for contextual claim evaluation
- Enhanced API with semantic scoring and trust correlation
- Integrated offline reliability module for P2P nodes
- Refactored app-core boundaries for future modular UI integration
- Implemented continuous integration workflows with test suites
- Added comprehensive desktop UI integration (Tauri/React)
- Production-ready builds for Linux DEB/RPM packages
- Enhanced security model with Ed25519 signature verification

## [0.3.0] — Core Stabilization & Crypto Verification
- Unified crypto engine with message signing and key verification
- Improved P2P sync consistency with verified headers
- Enhanced truthctl diagnostics and structured logging
- Expanded test coverage for distributed trust metrics
- Documentation alignment (/spec/, /docs/)
- Strengthened security model with cryptographic verification
- Improved error handling and resilience

## [0.2.7-pre] — Local Peer Analytics & Sync History
- Added peer_history table and metrics
- Added /api/v1/network/local endpoint
- Added CLI commands: peers stats, peers history
- Updated Spec Kit traceability and documentation
- Enables decentralized peer diagnostics without global scoring

## [0.2.3-pre] — Distributed Trust Propagation
- Distributed trust propagation in P2P sync:
  - Blend local/remote trust (0.8/0.2), time decay after 7 days
  - Trust deltas in SyncResult; logs show propagated changes
  - CLI: `truthctl ratings trust [--verbose]`, enhanced peers sync-all output
- Docs/spec updated: CLI usage, event rating protocol, architecture
- Version bump across workspace

## [0.2.1-pre] — CLI Tool & Architecture Separation
- New CLI tool: `truthctl` (subcommands: sync, verify, ratings, status)
- Clean separation: `core` ↔ `app` ↔ P2P node; `truthctl` moved to `app/`
- Architecture docs updated ([docs/ARCHITECTURE.md](docs/ARCHITECTURE.md), [spec/03-architecture.md](spec/03-architecture.md), [spec/11-decision-log.md](spec/11-decision-log.md))
- Feature-gated P2P sync (`p2p-client-sync`)

## [0.2.0] — Ratings System & Verified P2P Sync
- Ratings system: `node_ratings` and `group_ratings` with merge/conflict resolution
- Graph API: `/graph/json` (filtered) and `/graph/summary` (aggregated)
- Verified P2P sync: headers (`X-Public-Key`, `X-Signature`, `X-Timestamp`, `X-Ratings-Hash`), reconciliation, hash check
- Docs and Spec Kit aligned; tests green (`cargo check`, `clippy`, `test`)
