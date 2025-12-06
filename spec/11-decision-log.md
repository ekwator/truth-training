# Decision Log (ADR)

Use /spec as the primary decision source before reading /docs.
Version: v1.0.0
Updated: 2025-01-XX
Spec ID: 11

## Decision Log (ADR)

## Architectural Decisions

### ADR-0001: Use SQLite and Actix-web
**Decision**: Use SQLite for local-first storage and actix-web for HTTP API
**Rationale**: SQLite provides embedded database capabilities without external dependencies, actix-web offers high-performance async HTTP handling
**Status**: Implemented

### ADR-0002: Tri-state Detection Model
**Decision**: Model detected as NULL/0/1 to capture unknown states
**Rationale**: Allows for three states: unknown (NULL), false (0), true (1) for better truth assessment
**Status**: Implemented

### ADR-0003: Expert Heuristic MVP
**Decision**: Implement simple weighted heuristic before full rating protocol
**Rationale**: Provides immediate value while allowing for future enhancement
**Status**: Implemented

### ADR-0004: Signed Sync Protocol
**Decision**: Require Ed25519 signatures for sync endpoints
**Rationale**: Ensures data integrity and peer authentication in P2P network
**Status**: Implemented

### ADR-0005: FidoNet-Inspired Architecture
**Decision**: Adopt FidoNet principles for decentralized P2P communication
**Rationale**: Store-and-forward, hub/leaf roles, and trust propagation provide robust foundation for truth verification network
**Status**: Implemented (v0.2.1-pre)

### ADR-0006: Modular CLI Architecture
**Decision**: Separate CLI into `app/` crate with `truth-core` as library dependency
**Rationale**: Enables modular testing, clean builds, and independent versioning
**Status**: Implemented (v0.2.1-pre)

### ADR-0007: Embedded Context Fields (v1.0.0)
**Decision**: Embed context fields (`category_id`, `forma_id`, `cause_id`, `develop_id`, `effect_id`) directly into `truth_events` table, removing `context_id` foreign key
**Rationale**: 
- Eliminates JOIN overhead when querying events with context information
- Improves query performance (direct field access vs. FK lookup)
- Maintains data integrity via FK constraints to reference tables
- Enables ad-hoc categorization (events can have context values without matching templates)
- Simplifies API contracts (no nested context object required)
**Alternatives Considered**:
- Keep `context_id` with JOIN: Maintains normalization but adds query complexity
- Hybrid approach (both `context_id` and embedded fields): Redundant and violates single source of truth
**Status**: Implemented (v1.0.0)
**Breaking Change**: Manual database migration required (no automatic migrations)

### ADR-0008: Context Template System (v1.0.0)
**Decision**: Implement reusable context templates with NULL-aware duplicate detection and matching
**Rationale**:
- Enables consistent event categorization across users
- Streamlines event creation (template selection auto-prefills fields)
- NULL-aware comparison allows partial templates (only non-NULL fields compared)
- Template matching displays context name in event lists when match found
**Key Features**:
- NULL-aware duplicate detection (compares only non-NULL fields)
- NULL-aware template matching (matches events to templates by non-NULL fields)
- Foreign key validation (rejects invalid references with 400 error)
- Duplicate prevention (409 Conflict when identical non-NULL fields exist)
**Status**: Implemented (v1.0.0)

### ADR-0009: Cross-Platform Discovery Protocol (v1.0.0)
**Decision**: Unified node discovery system with identical protocols, schemas, and semantics across Desktop, CLI, Server, and Android
**Rationale**:
- Ensures network view consistency across all platforms
- Enables seamless peer discovery regardless of platform
- Simplifies cross-platform testing and validation
- Reduces maintenance burden (single protocol specification)
**Key Components**:
- Standard UDP multicast address (239.255.0.1:52525) across all platforms
- Unified JSON packet format (LanAnnouncement) with Ed25519 signature verification
- Consistent TTL rules and cleanup logic
- Deterministic merge rules (Local > Global, then last_seen, then lexicographic address)
- Global registry polling with flexible response format support
**Status**: Implemented (v1.0.0-Release)

### ADR-0010: Locale-Aware Knowledge Base Seeding (v1.0.0)
**Decision**: Support locale-specific knowledge base seeding with automatic reseeding on locale change
**Rationale**:
- Enables multilingual support (Russian/English) for knowledge base content
- Maintains data consistency when locale changes
- Provides fallback to English for unsupported locales
- Preserves user locale preference across app restarts
**Implementation**:
- `seed_knowledge_base(conn, locale)` function accepts locale parameter ("en" or "ru")
- Locale read from `~/.truth-training/config.json` during database initialization
- Automatic reseeding via `reseed_knowledge_base` Tauri command when locale changes
- Fallback to "en" if config missing or locale invalid
**Status**: Implemented (v1.0.0-Develop)

### ADR-0011: No User Action Logging (Confidentiality Principle) (v1.0.0)
**Decision**: Prohibit logging, tracking, or persistent storage of user actions, navigation patterns, clicks, or behavioral data
**Rationale**:
- Core architectural requirement for anonymous trust network
- Ensures complete privacy and anonymity for users
- Prevents user identification through behavioral patterns
- Aligns with project's fundamental principle: "truth travels without identity"
**Enforcement**:
- Database schemas do not include tables for user action logging
- No telemetry or analytics collection
- Only system-level logs (errors, sync operations) temporarily stored for debugging
- All stored data (events, judgments, contexts) is anonymous and cannot be traced to users
**Status**: Implemented (v1.0.0)
**Note**: This principle is enforced across all platforms (Desktop UI, Android, Server, CLI) and violations result in PR rejection

### ADR-0012: Unified Cross-Platform Database Schema (v1.0.0)
**Decision**: Use canonical SQLite schema shared across Desktop, CLI, Server, and Android with identical table structure, indices, and constraints
**Rationale**:
- Ensures data compatibility across all platforms
- Enables seamless data exchange and synchronization
- Simplifies cross-platform testing and validation
- Reduces schema drift and migration complexity
**Implementation**:
- Canonical schema exported from `core/src/storage.rs` via `export_schema_sql()`
- Desktop uses schema directly via `truth_storage::export_schema_sql()`
- Android uses shared SQL asset (`app/src/main/assets/schema.sql`) derived from core
- All platforms validate schema on database open
- Legacy tables (`events`, `impacts`, `summaries`, `logs`) are dropped during initialization
**Status**: Implemented (v1.0.0)

## Implementation Summary

### v1.0.0 - First Stable Release
- **Embedded Context Fields**: Removed `context_id` FK, added five embedded fields (`category_id`, `forma_id`, `cause_id`, `develop_id`, `effect_id`) directly to `truth_events` table
- **Context Template System**: NULL-aware duplicate detection and matching, template creation/editing UI, API endpoints for template management
- **Cross-Platform Discovery**: Unified UDP multicast (239.255.0.1:52525), global registry polling, TTL-based cleanup across Desktop/CLI/Server/Android
- **Locale-Aware Knowledge Base**: Russian/English support with automatic reseeding on locale change
- **Confidentiality Enforcement**: Removed user action logging, no persistent tracking, no telemetry collection
- **Unified Schema**: Canonical SQLite schema shared across all platforms, legacy table removal

### v1.0.0-Release - Unified Discovery & Sync
- **Cross-Platform Node Discovery**: Identical discovery protocols, JSON schemas, and database schemas across all platforms
- **Android Integration**: Room database, WorkManager sync, Compose UI, UDP multicast client
- **Desktop/CLI Integration**: Tauri backend worker, React UI, complete CLI command suite
- **Real-Device E2E Testing**: Cross-device tests verified on physical Android device

### v0.2.8-pre - Adaptive Propagation Priority
- Introduced `propagation_priority` EMA logic combining trust, quality_index, relay_success_rate
- Stored in `node_ratings` and duplicated in `node_metrics` for visualization
- P2P sync exchanges and blends priority; API/CLI expose averages and per-node values
- Docs updated across README, CLI usage, architecture, sync spec

### v0.2.1-pre - FidoNet-Inspired P2P Network
- **FidoNet principles**: Store-and-forward, hub/leaf roles, trust propagation, zone addressing
- **Peer etiquette**: Respectful sync intervals, exponential backoff, graceful degradation
- **CLI enhancement**: Full peer management, key generation, node initialization, network sync
- **Trust propagation**: Weighted blend (local*0.8 + remote*0.2) with temporal decay
- **Modular architecture**: Clean separation between core library and CLI application

### v0.2.0 - Stable Sync and Rating Integration
- **Signature verification**: Result-based API with explicit error types
- **Async sync architecture**: `/sync` and `/incremental_sync` with timestamped message patterns
- **Conflict resolution**: Latest timestamp wins, with trust-weighted influence
- **Peer registry**: File-based `peers.json` for peer management
- **Audit logging**: Persistent sync logs for diagnostics and monitoring

### Technical Details
- **Verification**: Precise `VerifyError` variants (hex decode, parse, verify failure)
- **Message patterns**: `sync_push:{ts}:{ratings_hash}` and `incremental_sync:{ts}:{ratings_hash}`
- **Reconciliation**: Idempotent operations with conflict resolution by timestamp
- **CLI integration**: Reuses `CryptoIdentity` signing logic from server codepaths
- **Feature gating**: `p2p-client-sync` feature for conditional compilation

_Version: v1.0.0_

- See [docs/README.md](../docs/README.md) for detailed explanations.

- See [spec/README.md](README.md) for detailed explanations.
