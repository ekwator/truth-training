# Decision Log (ADR)

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

## Implementation Summary (2025-10)

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
