## Architecture Overview

See `docs/architecture.md` for module layout and mermaid diagrams.

Core responsibilities
- Data logic and storage (SQLite via rusqlite).
- REST API (actix-web) for UI/peers.
- P2P sync and discovery.

Modules
- core-lib: models, storage (schema + ops), expert heuristics.
- server (workspace root): API, P2P, discovery, HTTP server.
- app: CLI for local testing.

Non-goals (MVP)
- Full reputation and Sybil-resistance; advanced propagation semantics.
