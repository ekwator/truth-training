# Changelog

## 0.2.3-pre
- Distributed trust propagation in P2P sync:
  - Blend local/remote trust (0.8/0.2), time decay after 7 days
  - Trust deltas in SyncResult; logs show propagated changes
  - CLI: `truthctl ratings trust [--verbose]`, enhanced peers sync-all output
- Docs/spec updated: CLI usage, event rating protocol, architecture
- Version bump across workspace

## 0.2.1-pre
- New CLI tool: `truthctl` (subcommands: sync, verify, ratings, status)
- Clean separation: `core-lib` ↔ `app` ↔ P2P node; `truthctl` moved to `app/`
- Architecture docs updated (`docs/ARCHITECTURE.md`, `spec/03-architecture.md`, `spec/11-decision-log.md`)
- Feature-gated P2P sync (`p2p-client-sync`)

## 0.2.0
- Ratings system: `node_ratings` and `group_ratings` with merge/conflict resolution
- Graph API: `/graph/json` (filtered) and `/graph/summary` (aggregated)
- Verified P2P sync: headers (`X-Public-Key`, `X-Signature`, `X-Timestamp`, `X-Ratings-Hash`), reconciliation, hash check
- Docs and Spec Kit aligned; tests green (`cargo check`, `clippy`, `test`)
