# Decision Log (ADR)

- 0001-use-sqlite-and-actix-web.md: Use SQLite for local-first and actix-web for HTTP.
- 0002-tri-state-detected.md: Model detected as NULL/0/1 to capture unknown.
- 0003-expert-heuristic-mvp.md: Implement simple weighted heuristic before full rating protocol.
- 0004-signed-sync.md: Require Ed25519 signatures for sync endpoints.

Summary (2025-10):
- Migrated signature verification to Result-based API via `CryptoIdentity::verify_from_hex` with explicit error types.
- Implemented async sync architecture with `/sync` and `/incremental_sync`, timestamped message patterns, and conflict resolution by latest timestamp.
- Added `truthctl` CLI and file-based peer registry `peers.json` to manage peers and trigger syncs.
