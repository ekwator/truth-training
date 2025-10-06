## Architecture Overview

This document reflects the current `truth-core` implementation and the CLI utilities.

Responsibilities
- Data logic and storage (SQLite via rusqlite).
- REST API (actix-web) for local UI and peer interop.
- P2P synchronization and peer discovery.
- Ed25519 signing/verification for sync endpoints.

Modules
- core-lib: models, storage (schema + ops), expert heuristics.
- api: HTTP routes in `src/api.rs` (health, init/seed, events/statements, impacts, progress, get_data, sync, incremental_sync) with signature verification helpers.
- p2p: sync flows and reconciliation in `src/p2p/sync.rs`, periodic node loop in `src/p2p/node.rs`.
- p2p/encryption: `CryptoIdentity` (Ed25519) with hex helpers and Result-based verify; header message patterns.
- net: UDP beacon sender/listener in `src/net.rs` for LAN peer discovery.
- app/truthctl: peer registry (`peers.json`), `peers add/list`, and `sync` orchestration (push or pull-only).

Non-goals (MVP)
- Reputation/Sybil resistance; validator weighting; global propagation semantics.

Overview
```mermaid
flowchart TD
  UI -->|HTTP| API
  API --> DB[(SQLite)]
  API --> P2P[Sync Engine]
  P2P --> ENC[CryptoIdentity]
  P2P --> NET[Discovery]
  ENC -->|sign/verify| P2P
  CLI[truthctl] -->|peers.json| P2P
```
