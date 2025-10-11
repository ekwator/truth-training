## Architecture Overview

This document reflects the current `truth-core` implementation and the CLI utilities, inspired by FidoNet principles for decentralized peer-to-peer communication.

### FidoNet-Inspired Network Model

**Core Principles:**
- **Store-and-forward**: Nodes store data locally and synchronize on schedule or on-demand
- **Hub/Leaf roles**: Leaf nodes (edge) connect to hub nodes (relay/aggregator) for data propagation
- **Trust propagation**: Reputation spreads through the network via signed endorsements
- **Zone addressing**: Nodes can be addressed as `Zone:Net/Node` for hierarchical routing
- **Delayed sync**: Nodes may be offline and sync when reconnected

**Node Behavior:**
- **Isolated nodes**: Can operate independently and sync when peers become available
- **Late synchronization**: Nodes can reconstruct missing data via peer replication
- **Forward-ack exchange**: Nodes acknowledge receipt and forward data to other peers
- **Peer etiquette**: Respectful sync intervals, avoid overwhelming peers

### Functional Separation

- **truth-core**: core logic, P2P, crypto, and DB access (library only, no user I/O).
- **app (truthctl)**: administrative CLI that uses truth-core as a dependency.
- **server**: network node (HTTP + P2P) that provides API endpoints.

This separation ensures modular testing, clean builds, and independent versioning.

### Responsibilities

- Data logic and storage (SQLite via rusqlite).
- REST API (actix-web) for local UI and peer interop.
- P2P synchronization and peer discovery.
- Ed25519 signing/verification for sync endpoints.
- Trust propagation and reputation management.

### Modules

- **core-lib**: models, storage (schema + ops), expert heuristics.
- **api**: HTTP routes in `src/api.rs` (health, init/seed, events/statements, impacts, progress, get_data, sync, incremental_sync, ratings, graph) with signature verification helpers. Server health checks for API/DB/P2P are exposed via `truth_core::server_diagnostics` and can be invoked from CLI.
- **p2p**: sync flows and reconciliation in `src/p2p/sync.rs`, periodic node loop in `src/p2p/node.rs`.
- **trust layer**: `core-lib/src/trust_propagation.rs` реализует смешивание доверия (local*0.8 + remote*0.2) и временной спад для устаревших записей; интеграция вызывается в процессе `reconcile` при слиянии рейтингов.
- **p2p/encryption**: `CryptoIdentity` (Ed25519) with hex helpers and Result-based verify; header message patterns.
- **net**: UDP beacon sender/listener in `src/net.rs` for LAN peer discovery.
- **app/truthctl**: peer registry (`peers.json`), `peers add/list`, and `sync` orchestration (push or pull-only).
- **sync logs**: persistent high-level sync logs in `core-lib/src/storage.rs` (table `sync_logs`), exposed via CLI `truthctl logs show|clear`.
- **node configuration**: user-editable `~/.truthctl/config.json` managed via `truthctl config` (show/set/reset).
- **status summary**: `truthctl status` aggregates configuration, peers, and recent `sync_logs` to report node health. For runtime checks, `truthctl diagnose --server` probes `/health`, opens SQLite, and inspects P2P listener status.
- **self-healing init**: `truthctl reset-data [--reinit]` clears local state and can reinitialize node automatically, including key generation/replacement and `init-node` invocation.

### Non-goals (MVP)

- Reputation/Sybil resistance; validator weighting; global propagation semantics.
- Trust layer intentionally simple (blend + decay), applied transparently during `/sync` and `/incremental_sync`.

### Architecture Diagram

```mermaid
flowchart TD
  UI[User Interface] -->|HTTP API| API[Actix-web API]
  API --> DB[(SQLite Database)]
  API --> P2P[P2P Sync Engine]
  P2P --> ENC[CryptoIdentity<br/>Ed25519]
  P2P --> NET[UDP Discovery]
  ENC -->|sign/verify| P2P
  CLI[truthctl CLI] -->|peers.json| P2P
  CLI -->|config.json| API
  
  subgraph "FidoNet-inspired Network"
    HUB[Hub Node<br/>Relay/Aggregator]
    LEAF1[Leaf Node A]
    LEAF2[Leaf Node B]
    LEAF3[Leaf Node C]
    
    LEAF1 -->|sync| HUB
    LEAF2 -->|sync| HUB
    LEAF3 -->|sync| HUB
    HUB -->|forward| LEAF1
    HUB -->|forward| LEAF2
    HUB -->|forward| LEAF3
  end
  
  P2P -.->|connect| HUB
```
