# Traceability Matrix
Version: v0.4.0
Updated: 2025-01-18
Spec ID: 13

### Requirements → Code Implementation

#### Core Functionality
- **Knowledge Base** → `core/src/storage.rs` (schema, seed_knowledge_base)
- **Event Tracking** → `core/src/storage.rs` (add_truth_event, set_event_detected)
- **Statements** → `core/src/storage.rs` (add_statement, getters)
- **Impacts** → `core/src/storage.rs` (add_impact)
- **Expert Heuristics** → `core/src/expert_simple.rs`; `app/src/main.rs` (assess)
- **Progress Metrics** → `core/src/storage.rs` (recalc_progress_metrics)

#### Network & API
- **HTTP API** → `src/api.rs` (all REST endpoints)
- **P2P Discovery** → `src/net.rs` (UDP beacons)
- **P2P Sync** → `src/p2p/sync.rs` (bidirectional, incremental, conflict resolution)
- **P2P Node** → `src/p2p/node.rs` (periodic sync loop)
- **Crypto Identity** → `src/p2p/encryption.rs` (Ed25519 signing/verification)
- **Local Network Statistics** → `src/api.rs` (`/api/v1/network/local`), `app/src/bin/truthctl.rs` (peers stats)

#### CLI & Management
- **CLI Commands** → `app/src/bin/truthctl.rs` (all truthctl subcommands)
- **Key Management** → `app/src/bin/truthctl.rs` (generate, import, list)
- **Peer Management** → `app/src/bin/truthctl.rs` (add, list, sync-all)
- **Node Configuration** → `app/src/bin/truthctl.rs` (init-node, config)
- **Diagnostics** → `app/src/bin/truthctl.rs` (status, diagnose, reset-data)

#### Trust & Ratings
- **Trust Propagation** → `core/src/trust_propagation.rs` (blend, EMA helpers)
- **Node Ratings** → `core/src/storage.rs` (node_ratings table)
- **Group Ratings** → `core/src/storage.rs` (group_ratings table)
- **Adaptive Propagation Priority** → `core/src/trust_propagation.rs::compute_propagation_priority`
- **Graph API** → `src/api.rs` (graph/json, graph/summary)

#### Collective Intelligence Layer
- **Collective Score** → `core-lib/src/models.rs` (`TruthEvent.collective_score`), `core-lib/src/storage.rs` (`recalc_collective_truth`)
- **API Recalculation** → `src/api.rs` (`POST /api/v1/recalc_collective`)
- **P2P Propagation** → `spec/08-p2p-sync.md` (shared among nodes)

#### Android Integration
- **JSON Signature Verification** → `src/android/verify_json.rs` (Ed25519 verification)
- **JNI Bridge** → `src/android/mod.rs` (processJsonRequest)
- **Cross-compilation** → Android/iOS via Cargo targets with `--features mobile`

### Requirements Traceability

| Requirement ID | Title | Module | Implementation | Status |
|----------------|-------|--------|----------------|--------|
| R-CI-01 | Collective Intelligence Layer | core-lib, api | `collective_score` field, `/api/v1/recalc_collective` | ✅ Implemented |
| R-TR-02 | Trust Propagation Model | core-lib | `trust_propagation.rs`, blend algorithms | ✅ Implemented |
| R-OF-03 | Offline Reliability Consistency | core-lib, p2p | Quality index, no time-based decay | ✅ Implemented |
| R-AN-04 | Android JSON Verification | android | Ed25519 signature verification | ✅ Implemented |
| R-P2P-05 | P2P Sync Protocol | p2p | Bidirectional sync, conflict resolution | ✅ Implemented |
| R-CLI-06 | CLI Management Tools | app | truthctl commands, diagnostics | ✅ Implemented |

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
- `docs/Concept_Collective_Intelligence.md` → Collective Intelligence Layer
- `docs/api_reference/API_REFERENCE.md` → API documentation
- `integration/android/README_INTEGRATION.md` → Android integration guide

### Feature Implementation Status

#### v0.4.0 Features
- ✅ **Collective Intelligence Layer**: Wisdom of the Crowd consensus mechanism
- ✅ **Offline Reliability Model**: Quality index without time-based decay
- ✅ **Android JSON Verification**: Ed25519 signature verification for mobile clients
- ✅ **Unified Documentation**: All documentation translated to English
- ✅ **Spec-Kit Traceability**: Complete requirements mapping

#### v0.3.0 Features
- ✅ **FidoNet-inspired P2P**: Store-and-forward, hub/leaf roles, peer etiquette
- ✅ **CLI Enhancement**: Full peer management, key generation, node initialization
- ✅ **Trust Propagation**: Weighted blend with adaptive quality index
- ✅ **Modular Architecture**: Clean separation between core and CLI
- ✅ **Network Sync**: Bidirectional sync with all known peers
- ✅ **Diagnostics**: Comprehensive node health checking

### Test Coverage

#### Unit Tests
- `src/api.rs` → API endpoint tests
- `src/p2p/encryption.rs` → Crypto identity tests
- `src/android/verify_json.rs` → Android verification tests
- `core-lib/src/storage.rs` → Database operation tests

#### Integration Tests
- `app/tests/truthctl_*.rs` → CLI command tests
- `tests/android_verify.rs` → Android signature verification tests
- `app/tests/truthctl_smoke.rs` → Basic functionality tests
- `app/tests/truthctl_peers_test.rs` → Peer management tests

#### Feature Tests
- `cargo test --workspace --features p2p-client-sync` → Full feature testing
- `cargo test --test android_verify` → Android verification testing