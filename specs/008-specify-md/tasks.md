# Tasks: Unified Node Discovery & Address Exchange

**Input**: Design documents from `/specs/008-specify-md/`  
**Prerequisites**: plan.md, research.md, data-model.md, contracts/, quickstart.md

## Phase 3.1: Setup
- [x] T001 Define shared discovery interval & TTL configuration (FR-012) in `core/src/config.rs` (or new module) and document defaults in `docs/Data_Schema.md` + `docs/CLI_Usage.md`, ensuring spec + plan values are codified.
- [x] T002 Update workspace dependencies for multicast + networking (root `Cargo.toml`, `app/Cargo.toml`, `truth-android-client/build.gradle.kts`, `ui/desktop/package.json`) and run `cargo check`/`npm install` to confirm compatibility.

## Phase 3.2: Tests First (TDD)
Contract tests (one per endpoint, distinct files → [P]):
- [x] T003 [P] Add GET `/nodes` contract test in `tests/contract/nodes/test_get_nodes.rs` validating filters and pagination schema.
- [x] T004 [P] Add POST `/nodes` contract test in `tests/contract/nodes/test_create_node.rs` covering required fields, conflict handling, and validation errors.
- [x] T005 [P] Add POST `/nodes/discover` contract test in `tests/contract/nodes/test_discover_nodes.rs` asserting response counters and type filtering.
- [x] T006 [P] Add POST `/nodes/sync` contract test in `tests/contract/nodes/test_sync_nodes.rs` covering merged payload semantics.
- [x] T007 [P] Add GET `/nodes/{id}` contract test in `tests/contract/nodes/test_get_node_by_id.rs` for 200/404 behaviors.
- [x] T008 [P] Add PUT `/nodes/{id}` contract test in `tests/contract/nodes/test_update_node.rs` verifying reachable/ttl updates.
- [x] T009 [P] Add DELETE `/nodes/{id}` contract test in `tests/contract/nodes/test_delete_node.rs` ensuring 204/404 handling.
- [x] T010 [P] Add GET `/nodes/health` contract test in `tests/contract/nodes/test_nodes_health.rs` checking payload structure and optional node_id filtering.

Integration tests (Quickstart scenarios → [P]):
- [x] T011 [P] Scenario 1 – schema/migration parity test in `tests/integration/test_schema_parity.rs` (creates DB via CLI, reopens via server/desktop/android stubs).
- [x] T012 [P] Scenario 2 – LAN discovery multicast test in `tests/integration/test_lan_discovery.rs`.
- [x] T013 [P] Scenario 3 – handshake sync flow test in `tests/integration/test_sync_handshake.rs`.
- [x] T014 [P] Scenario 4 – TTL cleanup test in `tests/integration/test_ttl_cleanup.rs`.
- [x] T015 [P] Scenario 5 – reachability health-check test in `tests/integration/test_node_health.rs`.
- [x] T016 [P] Scenario 6 – cross-platform DB compatibility test in `tests/integration/test_android_db_parity.rs`.
- [x] T017 [P] Scenario 7 – merge priority (local > global) test in `tests/integration/test_merge_priority.rs`.

## Phase 3.3: Core Implementation
- [x] T018 [P] Implement `Node` struct + serde helpers in `core/src/models.rs` (fields, enums, validation helpers).
- [x] T019 Extend `core/src/storage.rs` to create the `nodes` table, indices, and migration hooks (idempotent + versioned).
- [x] T020 Add node repository APIs in `core/src/storage.rs` (CRUD, search/filter, TTL-expired queries) used by CLI/server.
- [x] T021 Implement deterministic merge + priority (Local > Global) helpers in `core/src/sync.rs`, exposing reusable functions for other crates.
- [x] T022 Wire configurable discovery intervals/TTL defaults into `core/src/lib.rs` (or dedicated config module) and export to `app`/`server`.
- [x] T023 Implement UDP multicast advertise/listen service for LAN/Wi-Fi in `src/p2p/node.rs`, emitting signed payloads per research.
- [x] T024 Implement global registry polling + HTTP reachability checks (2–5s timeout, retries) in `src/p2p/node.rs` / `src/net.rs`.
- [x] T025 Add async TTL cleanup + reachability scheduler in `src/main.rs` (Tokio tasks using new repo APIs).

### HTTP API endpoints (`src/api.rs`; keep order → sequential, no [P])
- [x] T026 Implement GET `/nodes` handler wired to repository filters + pagination.
- [x] T027 Implement POST `/nodes` handler with validation, conflict detection, and audit logging.
- [x] T028 Implement POST `/nodes/discover` handler triggering LAN/Wi-Fi/global discovery workflows.
- [x] T029 Implement POST `/nodes/sync` handler performing merge + returning `merged` payload.
- [x] T030 Implement GET `/nodes/{id}` handler returning Node detail or 404.
- [x] T031 Implement PUT `/nodes/{id}` handler (reachable, ttl, source updates).
- [x] T032 Implement DELETE `/nodes/{id}` handler removing node + associated metrics.
- [x] T033 Implement GET `/nodes/health` handler returning reachability snapshot.

### CLI (`app/src/cli.rs`, `app/src/bin/truthctl.rs`)
- [x] T034 Expose node repository wiring + config loading in `app/src/cli.rs`.
- [x] T035 Implement `truthctl nodes list/add/remove` commands with structured output.
- [x] T036 Implement `truthctl nodes discover/sync` commands bridging to server API or local UDP.
- [x] T037 Implement `truthctl nodes cleanup/health-check` commands invoking repo TTL + HTTP health routines.
- [x] T038 Implement `truthctl nodes validate` command (FR-010) verifying schema parity, migrations, and recent sync status.

### Desktop (Tauri/Electron)
- [x] T039 Implement background discovery worker in `ui/desktop/src-tauri/src/discovery.rs` (Tokio interval + bridge to core).
- [x] T040 Update React UI (`ui/desktop/src/components/NodesPanel.tsx`) to display node list, reachability badge, TTL countdown, and manual refresh.
- [x] T041 Add desktop settings persistence for discovery intervals/TTL overrides in `ui/desktop/src-tauri/src/settings.rs` and connect to UI.

### Android (`truth-android-client`)
- [x] T042 Add `NodeEntity` + DAO + migration (Room) in `app/src/main/java/com/truth/training/client/data/database/`.
- [x] T043 Update `NodeRepository` + serializers in `app/src/main/java/com/truth/training/client/data/repository/NodeRepository.kt` to map to canonical schema.
- [x] T044 Implement `NodeSyncWorker` (WorkManager) covering discovery cadence + sync in `app/src/main/java/com/truth/training/client/worker/`.
- [x] T045 Implement Wi-Fi/LAN discovery client (multicast + network callbacks) in `app/src/main/java/com/truth/training/client/network/LanDiscoveryClient.kt`.
- [x] T046 Update Android UI (e.g., `ui/nodes/NodesScreen.kt`) to render node list, reachability, manual sync actions.
- [x] T047 Add Android instrumentation tests in `app/src/androidTest/java/com/truth/training/client/NodeDiscoveryTest.kt` covering WorkManager + DB compatibility.

## Phase 3.4: Integration & Observability
- [x] T048 Implement shared logging/metrics for discovery events (counters, TTL cleanup stats) in `src/main.rs`, `app/src/cli.rs`, and `ui/desktop/src-tauri/src/logging.rs`.
- [x] T049 Update `tests/cli_sync.rs` to cover new CLI commands end-to-end (list/discover/sync/cleanup/validate).
- [x] T050 Ensure Desktop ↔ Server ↔ Android sync handshake uses new merge helpers (update `tests/integration/cli_sync.rs` or new test) and document handshake contract in `contracts/README.md`.

## Phase 3.6: Post-Integration Hardening & Reliability
- [x] T055 Fix `lan_announcement_roundtrip` test feature flag gating (`#[cfg(feature = "desktop")]`).
- [x] T056 Integrate Android discovery UI screen into `MainNavigation.kt` with proper ViewModel factory.
- [x] T057 Clean up Rust compiler warnings (unused variables, functions) in `src/p2p/node.rs` and `ui/desktop/src-tauri/src/logging.rs`.
- [x] T058 Add tests for empty/malformed registry URLs in CLI commands (`tests/cli_sync.rs`).
- [x] T059 Create TTL behavior consistency tests (`tests/integration/test_ttl_consistency.rs`).
- [x] T060 Create JSON enum serialization tests (`tests/integration/test_json_enum_serialization.rs`).
- [x] T061 Verify UDP multicast compatibility between Desktop (Rust) and Android (Kotlin) with packet roundtrip tests.
- [ ] T062 Add cross-device E2E tests (Linux Desktop ↔ Android device, CLI ↔ Android ↔ Desktop).
- [x] T063 Update documentation with Android navigation instructions, device test instructions, and feature flag usage.
- [ ] T064 Update CI to run desktop feature tests (`cargo test --features desktop`).

## Phase 3.5: Polish & Documentation
- [ ] T051 [P] Update documentation (`README.md`, `docs/Data_Schema.md`, `docs/CLI_Usage.md`, `docs/architecture.md`) with schema, discovery flow, CLI usage, and Android/Desktop behaviors.
- [ ] T052 [P] Add performance regression tests/benchmarks for discovery + merge in `tests/perf/test_discovery_perf.rs` ensuring targets (<5s scan, <100ms merge).
- [ ] T053 [P] Execute quickstart scenarios end-to-end and capture results in `test-results/reports/node_discovery.md`, attaching logs/screenshots from each platform.
- [ ] T054 Run final CLI-driven verification (fresh DB, migrations, sync across modules) and record evidence in `test-results/reports/final_validation.md`.

## Dependencies
- T002 depends on T001 (config values referenced in deps).
- Tests T003–T017 must be implemented and failing before any implementation tasks (T018+).
- T019 depends on T018; T020 depends on T019; T021–T025 depend on repository availability.
- API tasks T026–T033 depend on T019–T021; CLI tasks T034–T038 depend on both repository (T019–T021) and API availability (T026–T033).
- Desktop tasks T039–T041 depend on core discovery services (T023–T025) and API endpoints (T026–T033).
- Android tasks T042–T047 depend on schema work (T018–T020) and discovery services (T023–T025).
- Observability tasks T048–T050 depend on prior implementation + tests.
- Polish tasks T051–T054 depend on completion of all prior work.

## Parallel Execution Example
```
# After T001–T002 complete, run contract + integration tests in parallel:
task-agent run T003
task-agent run T004
task-agent run T005
task-agent run T006
task-agent run T007
task-agent run T008
task-agent run T009
task-agent run T010
task-agent run T011
task-agent run T012
task-agent run T013
task-agent run T014
task-agent run T015
task-agent run T016
task-agent run T017

# Later, once core schema (T018–T021) is done, run Android-specific tasks together:
task-agent run T042
task-agent run T043
task-agent run T044
task-agent run T045
task-agent run T046
task-agent run T047
```

## Notes
- [P] tasks touch distinct files/modules; remove [P] if edits converge.
- Ensure contract/integration tests fail first, then make them pass via implementation tasks.
- Use structured logging + metrics per constitution.
- Capture evidence (logs, screenshots) for validation tasks (T053–T054).

