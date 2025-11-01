# Changelog

## [1.0.0] — First Stable Baseline — Context Fields Embedded
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
  - Constitution compliance documentation (`docs/Constitution-Compliance.md`)
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
- Architecture docs updated (`docs/ARCHITECTURE.md`, `spec/03-architecture.md`, `spec/11-decision-log.md`)
- Feature-gated P2P sync (`p2p-client-sync`)

## [0.2.0] — Ratings System & Verified P2P Sync
- Ratings system: `node_ratings` and `group_ratings` with merge/conflict resolution
- Graph API: `/graph/json` (filtered) and `/graph/summary` (aggregated)
- Verified P2P sync: headers (`X-Public-Key`, `X-Signature`, `X-Timestamp`, `X-Ratings-Hash`), reconciliation, hash check
- Docs and Spec Kit aligned; tests green (`cargo check`, `clippy`, `test`)
