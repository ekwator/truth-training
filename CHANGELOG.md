# Changelog

## [0.4.0-pre] — Collective Intelligence Layer (Current)
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
