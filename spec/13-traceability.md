## Traceability Matrix (v0.2.7-pre)

### Requirements → Code Implementation

#### Core Functionality
- **Knowledge Base** → `core-lib/src/storage.rs` (schema, seed_knowledge_base)
- **Event Tracking** → `core-lib/src/storage.rs` (add_truth_event, set_event_detected)
- **Statements** → `core-lib/src/storage.rs` (add_statement, getters)
- **Impacts** → `core-lib/src/storage.rs` (add_impact)
- **Expert Heuristics** → `core-lib/src/expert_simple.rs`; `app/src/main.rs` (assess)
- **Progress Metrics** → `core-lib/src/storage.rs` (recalc_progress_metrics)

#### Network & API
- **HTTP API** → `src/api.rs` (all REST endpoints)
- **P2P Discovery** → `src/net.rs` (UDP beacons)
- **P2P Sync** → `src/p2p/sync.rs` (bidirectional, incremental, conflict resolution)
- **P2P Node** → `src/p2p/node.rs` (periodic sync loop)
- **Crypto Identity** → `src/p2p/encryption.rs` (Ed25519 signing/verification)
 - **Local Network Statistics** → `src/api.rs` (`/api/v1/network/local`), `app/src/bin/truthctl.rs` (peers stats), docs in `README.md`

#### CLI & Management
- **CLI Commands** → `app/src/bin/truthctl.rs` (all truthctl subcommands)
| Feature                        | Spec File          | Module / File                | API / CLI Reference                   |
|--------------------------------|--------------------|------------------------------|--------------------------------------|
| Peer History Logging           | 08-p2p-sync.md     | core-lib/src/storage.rs      | truthctl peers history               |
| Local Network Statistics       | 03-architecture.md | src/api.rs, app/bin/truthctl | /api/v1/network/local, peers stats   |
| Quality & Trust Visualization  | 03-architecture.md | core-lib/src/models.rs       | /graph/json, truthctl graph show     |
- **Key Management** → `app/src/bin/truthctl.rs` (generate, import, list)
- **Peer Management** → `app/src/bin/truthctl.rs` (add, list, sync-all)
- **Node Configuration** → `app/src/bin/truthctl.rs` (init-node, config)
- **Diagnostics** → `app/src/bin/truthctl.rs` (status, diagnose, reset-data)

#### Trust & Ratings
- **Trust Propagation** → `core-lib/src/trust_propagation.rs` (blend, decay)
- **Node Ratings** → `core-lib/src/storage.rs` (node_ratings table)
- **Group Ratings** → `core-lib/src/storage.rs` (group_ratings table)
- **Graph API** → `src/api.rs` (graph/json, graph/summary)

### Documentation → Specification Mapping

#### Legacy Docs → Spec Kit
- `docs/Technical_Specification.md` → `spec/02-requirements.md`
- `docs/Data_Schema.md` → `spec/04-data-model.md`
- `docs/event_rating_protocol.md` → `spec/07-event-rating-protocol.md`
- `docs/p2p_release.md` → `spec/08-p2p-sync.md`
- `docs/ui_guidelines.md` → `spec/09-ux-guidelines.md`

#### New Documentation
- `README.md` → `spec/03-architecture.md` (FidoNet principles)
- `docs/CLI_Usage.md` → `spec/10-cli.md` (CLI specification)
- `spec/README.md` → Updated Spec Kit index
- `spec/11-decision-log.md` → Updated ADR log

### Feature Implementation Status

#### v0.2.1-pre Features
- ✅ **FidoNet-inspired P2P**: Store-and-forward, hub/leaf roles, peer etiquette
- ✅ **CLI Enhancement**: Full peer management, key generation, node initialization
- ✅ **Trust Propagation**: Weighted blend with temporal decay
- ✅ **Modular Architecture**: Clean separation between core and CLI
- ✅ **Network Sync**: Bidirectional sync with all known peers
- ✅ **Diagnostics**: Comprehensive node health checking

#### v0.2.0 Features
- ✅ **Stable Sync**: `/sync` and `/incremental_sync` endpoints
- ✅ **Rating Integration**: Node and group rating system
- ✅ **Graph API**: Filtered graph endpoints with summaries
- ✅ **Conflict Resolution**: Timestamp-based with trust weighting
- ✅ **Audit Logging**: Persistent sync logs for monitoring

### Test Coverage

#### Unit Tests
- `src/api.rs` → API endpoint tests
- `src/p2p/encryption.rs` → Crypto identity tests
- `core-lib/src/storage.rs` → Database operation tests

#### Integration Tests
- `app/tests/truthctl_*.rs` → CLI command tests
- `app/tests/truthctl_smoke.rs` → Basic functionality tests
- `app/tests/truthctl_peers_test.rs` → Peer management tests

#### Feature Tests
- `cargo test --workspace --features p2p-client-sync` → Full feature testing
