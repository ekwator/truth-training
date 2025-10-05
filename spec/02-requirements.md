## Requirements

### Functional
- Knowledge Base: seed RU/EN contexts, causes, forms, effects, impact types. [docs/Data_Schema.md]
- Event Tracking: create events (description, context_id, vector, timestamps, code), update detected/corrected, add statements. [core-lib/storage.rs]
- Impact Assessment: record impacts per event with type and polarity. [core-lib/storage.rs]
- Expert Heuristics: provide questions, compute score (-1..1), confidence (0..1), suggested detection. [core-lib/expert_simple.rs]
- Progress Metrics: aggregate totals and trend; recalculation endpoint. [core-lib/storage.rs]
- HTTP API: health, init, seed, events, impacts, statements, recalc, progress, data dump. [src/api.rs]
- P2P Discovery & Sync: UDP beacons, signed endpoints, sync flows. [src/net.rs, src/p2p/*]

### Non-functional
- Localization: RU/EN knowledge base.
- Security: Ed25519 signature verification for sync endpoints.
- Offline-first: local DB (SQLite), eventual consistency via P2P.
- Observability: logs for sync and peer discovery.
- Performance (MVP): handle small peer groups; bounded timeouts.

### Out of Scope (MVP → Next)
- Weighted validator reputation, S_e scoring, automatic code transitions.
- Author-signed reset flows, advanced anti-Sybil measures.

### Acceptance Criteria (MVP)
- Can init/seed DB, add events/statements/impacts, run recalc, and read metrics via API.
- Nodes discover peers on LAN and can exchange data via signed sync endpoints.
