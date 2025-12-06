# Test Plan

Use /spec as the primary decision source before reading /docs.
Version: v1.0.0
Updated: 2025-01-XX
Spec ID: 16

## Test Plan

### Storage
- init_db creates all tables idempotently
- seed_knowledge_base inserts RU/EN references (locale-aware)
- Embedded context fields (category_id, forma_id, cause_id, develop_id, effect_id) stored correctly
- Foreign key validation rejects invalid references

### API
- /init, /seed return 200
- /api/v1/info returns node information (node_name, version, p2p_enabled, db_path, peer_count)
- /api/v1/stats returns database statistics (events, statements, impacts, node_ratings, group_ratings, avg_trust_score, avg_quality_index)
- /events POST creates with embedded context fields (rejects context_id); GET requires signature (401 on invalid)
- /impacts POST inserts row
- /recalc inserts metrics row
- /get_data returns arrays
- /sync with valid headers returns SyncResult; missing/invalid headers → 400/401
- /incremental_sync with valid headers returns SyncResult
- /contexts GET lists templates, POST creates with duplicate detection (409 Conflict), GET /by-name/{name}, POST /match, POST /from-event

### Context Template System (v1.0.0)
- Template creation with embedded fields (category_id, forma_id, cause_id, develop_id, effect_id)
- Duplicate detection (non-NULL field comparison)
- Template matching (NULL-aware, partial matches valid)
- Template creation from event
- Foreign key validation for all context fields

### Expert
- evaluate_answers extremes produce +/-1 with confidence ~1
- unknown answers reduce confidence

### P2P & Discovery (v1.0.0)
- UDP multicast discovery (239.255.0.1:52525) discovers peers on localhost network
- Global registry polling (HTTPS GET) with envelope and array formats
- TTL-based cleanup of stale nodes
- sync_with_peer performs GET /get_data (signed client-side), parses SyncData
- push_local_data posts signed /sync; reconcile merges data by timestamps
- incremental_sync_with_peer posts signed /incremental_sync with deltas
- Cross-platform discovery compatibility (Desktop/CLI/Server/Android)

### Cross-Platform Tests
- JSON schema equivalence (Rust/Kotlin/TypeScript)
- Enum compatibility (NodeType, NodeSource across platforms)
- Embedded context fields consistency
- Discovery protocol compatibility
- Sync data format consistency

### Desktop UI Tests
- Jest unit tests (components, stores, validation)
- React integration tests (create-event flow, context editor, template matching)
- Playwright E2E tests (navigation, event creation, template management)
- Offline queue tests
- Performance tests (navigation <200ms, pagination <100ms)

### Android Tests
- JUnit unit tests (ViewModels, Repositories, DTO validation)
- Instrumentation tests (physical device required for discovery, JNI, network)
- Room database tests (queries <50ms)
- Performance benchmarks (cold start <1.3s, UI rendering <200ms)
- Cross-platform compatibility tests

### Security
- verify_signature success on correct message; failure on tampered message
- consistent message construction for `sync_request`, `sync_push`, `incremental_sync`
- Ed25519 signature verification across platforms (Rust/Android/Desktop)
- JSON signature formats match across ecosystems

### Test Coverage Requirements
- Rust Core: ≥90%
- Desktop UI: ≥85%
- Android: ≥70% (unit + instrumentation combined)
- Server API: ≥90%

See [CONTRIBUTING.md](../CONTRIBUTING.md) for detailed testing requirements.

_Version: v1.0.0_

- See [docs/README.md](../docs/README.md) for detailed explanations.

- See [spec/README.md](README.md) for detailed explanations.
