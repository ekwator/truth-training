
# Implementation Plan: Unified Node Discovery & Address Exchange

**Branch**: `008-specify-md` | **Date**: 2025-11-17 | **Spec**: `[/specs/008-specify-md/spec.md](/specs/008-specify-md/spec.md)(/[specs/008-specify-md/spec.md](specs/008-specify-md/spec.md))`
**Input**: Feature specification from `[/specs/008-specify-md/spec.md](/specs/008-specify-md/spec.md)(/[specs/008-specify-md/spec.md](specs/008-specify-md/spec.md))`

## Execution Flow (/plan command scope)
```
1. Load feature spec from Input path
   → If not found: ERROR "No feature spec at {path}"
2. Fill Technical Context (scan for NEEDS CLARIFICATION)
   → Detect Project Type from file system structure or context (web=frontend+backend, mobile=app+api)
   → Set Structure Decision based on project type
3. Fill the Constitution Check section based on the content of the constitution document.
4. Evaluate Constitution Check section below
   → If violations exist: Document in Complexity Tracking
   → If no justification possible: ERROR "Simplify approach first"
   → Ensure collective intelligence principles are preserved
   → Update Progress Tracking: Initial Constitution Check
5. Execute Phase 0 → [research.md](research.md)(r[esearch.md](esearch.md))
   → If NEEDS CLARIFICATION remain: ERROR "Resolve unknowns"
6. Execute Phase 1 → contracts, [data-model.md](data-model.md)(d[ata-model.md](ata-model.md)), [quickstart.md](quickstart.md)(q[uickstart.md](uickstart.md)), agent-specific template file (e.g., `[CLAUDE.md](CLAUDE.md)(C[LAUDE.md](LAUDE.md))` for Claude Code, `[.github/copilot-instructions.md](.github/copilot-instructions.md)(.[github/copilot-instructions.md](github/copilot-instructions.md))` for GitHub Copilot, `[GEMINI.md](GEMINI.md)(G[EMINI.md](EMINI.md))` for Gemini CLI, `[QWEN.md](QWEN.md)(Q[WEN.md](WEN.md))` for Qwen Code, or `[AGENTS.md](AGENTS.md)(A[GENTS.md](GENTS.md))` for all other agents).
7. Re-evaluate Constitution Check section
   → If new violations: Refactor design, return to Phase 1
   → Update Progress Tracking: Post-Design Constitution Check
8. Plan Phase 2 → Describe task generation approach (DO NOT create [tasks.md](tasks.md)(t[asks.md](asks.md)
9. STOP - Ready for /tasks command
```

**IMPORTANT**: The /plan command STOPS at step 7. Phases 2-4 are executed by other commands:
- Phase 2: /tasks command creates [tasks.md](tasks.md)(t[asks.md](asks.md))
- Phase 3-4: Implementation execution (manual or via tools)

## Summary
Design and implement a unified node discovery and address exchange system so every Truth Training module (Rust CLI/server, Electron desktop UI, Android app) shares a canonical SQLite schema, synchronized node lists, and consistent merge/cleanup rules. The approach combines local (LAN/Wi-Fi) UDP multicast discovery, global registry polling, TTL-driven cleanup, deterministic merging that prefers local sources over global, and cross-module sync plus validation tooling.

## Technical Context
**Language/Version**: Rust 1.75+ (CLI/Server/Desktop core), Kotlin (Android), TypeScript/React (Desktop UI), SQLite 3.x  
**Primary Dependencies**: `rusqlite`, `tokio`, `actix-web`, `reqwest`, `clap`, `ed25519-dalek`, Room (Android), WorkManager, Electron/Tauri runtime  
**Storage**: SQLite (shared) with Room entities on Android and rusqlite elsewhere  
**Testing**: `cargo test`, Rust integration tests (`tests/`), Android unit + instrumentation (JUnit/connectedAndroidTest), Desktop Jest/Vitest, CLI end-to-end scripts  
**Target Platform**: Linux/Windows/macOS (CLI, server, desktop), Android 8.0+ (API 26+)  
**Project Type**: Multi-platform monorepo (Rust workspace + Android app + Electron desktop)  
**Performance Goals**: Discovery scan <5s per network, merge <100ms for 1k nodes, cleanup <50ms, cross-module sync <2s, health check timeout 2-5s  
**Constraints**: Offline-capable LAN mode, low battery impact on mobile (WorkManager cadence), backward-compatible migrations, cryptographic node identity, deterministic merging (Local > Global)  
**Scale/Scope**: 4 modules, 1 canonical schema + 4 migration paths, 3 discovery channels (LAN/Wi-Fi/Global), ~35 implementation tasks spanning schema, network logic, UI, tests

## Constitution Check
*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### I. Separation of Concerns by Crate
✅ **PASS** – Canonical schema lives in `core/`; platform adapters reside in `app/`, `src/`, `ui/desktop/`, and `truth-android-client/`.

### II. API- and CLI-First Interfaces
✅ **PASS** – CLI gains discovery/sync commands; server exposes REST endpoints defined in `contracts/nodes-api.yaml`; desktop/mobile consume those contracts.

### III. Cryptographic Integrity
✅ **PASS** – Node advertisements and handshakes continue to use `ed25519-dalek` signatures; merge rules rely on cryptographic node IDs when available.

### IV. Integration Testing Across Layers
✅ **PASS** – Plan mandates CLI↔Server, Desktop↔Server, Android↔Server, and LAN discovery integration tests plus contract tests per endpoint.

### V. Observability, Versioning & Simplicity
✅ **PASS** – Structured logging for discovery cycles, explicit SQLite migrations, TTL metrics per sync; stays with SQLite (YAGNI).

### VI. Truth as Emergent Consensus
✅ **PASS** – Shared discovery state keeps nodes aligned, enabling collective intelligence to function across environments.

### VII. Dynamic Reputation & Weighted Consensus
⚠️ **NOTE** – Discovery itself is topology-focused; reputation remains in `node_ratings`. No changes to weighting logic required.

### VIII. Self-Correcting Information Ecosystem
✅ **PASS** – TTL cleanup, reachability checks, and deterministic merges ensure stale data self-heals.

### IX. Cross-Platform Collective Intelligence
✅ **PASS** – Feature explicitly enforces schema and behavioral parity across CLI, server, desktop, and Android.

**Initial Constitution Check**: ✅ PASS

## Project Structure

### Documentation (this feature)
```
specs/[###-feature]/
├── [plan.md](plan.md)(p[lan.md](lan.md))              # This file (/plan command output)
├── [research.md](research.md)(r[esearch.md](esearch.md))          # Phase 0 output (/plan command)
├── [data-model.md](data-model.md)(d[ata-model.md](ata-model.md))        # Phase 1 output (/plan command)
├── [quickstart.md](quickstart.md)(q[uickstart.md](uickstart.md))        # Phase 1 output (/plan command)
├── contracts/           # Phase 1 output (/plan command)
└── [tasks.md](tasks.md)(t[asks.md](asks.md))             # Phase 2 output (/tasks command - NOT created by /plan)
```

### Source Code (repository root)
```
core/                          # Shared core library
├── src/
│   ├── storage.rs             # SQLite schema + migrations (add nodes table)
│   ├── models.rs              # Node entity models
│   └── lib.rs
└── Cargo.toml

app/                           # CLI application
├── src/
│   ├── cli.rs                 # Node discovery & sync commands
│   └── bin/truthctl.rs
└── Cargo.toml

src/                           # Server (HTTP API + P2P)
├── api.rs                     # Node exchange endpoints
├── p2p/
│   └── node.rs                # Discovery + handshake logic
└── main.rs

ui/desktop/
├── src-tauri/                 # Rust backend workers
└── src/                       # Electron/Tauri frontend (React)

truth-android-client/
├── app/src/main/java/com/truth/training/client/
│   ├── data/database/         # Room entities + migrations
│   ├── data/repository/       # Node repository
│   └── worker/                # WorkManager periodic sync
└── build.gradle.kts

tests/
├── contract/                  # OpenAPI-derived tests
├── integration/               # Cross-module sync tests
└── cli_sync.rs                # CLI validation flows
```

**Structure Decision**: Multi-platform monorepo anchored on `core/` schema with platform adapters per module; ensures canonical data + separation of concerns.

## Phase 0: Outline & Research
**Research Output**: `[/[specs/008-specify-md/research.md](specs/008-specify-md/research.md)](research.md)(/[specs/008-specify-md/research.md](research.md))` (updated 2025-11-17)

Key findings:
1. **Canonical SQLite Schema** – Use identical DDL (INTEGER PRIMARY KEY AUTOINCREMENT, indexed `address`, `last_seen`, `type`, `reachable`). Avoid platform-specific SQLite features to keep Room + rusqlite compatible.
2. **LAN/Wi-Fi Discovery** – Adopt UDP multicast advertisements (JSON payload signed with ed25519). Frequency: 30s broadcast, 60s listening window. Works across Rust + Android sockets without extra deps.
3. **Global Discovery** – Poll relay/registry endpoints via HTTPS hourly, validate reachability with async HTTP health checks (2–5s timeout, retries capped at 3).
4. **TTL & Cleanup** – Default TTLs: LAN 120s, Wi-Fi 300s, Global 3600s. Cleanup intervals under half TTL; unreachable nodes removed if `reachable=0` for > ttl/2.
5. **Merge Determinism** – Prefer Local (LAN/Wi-Fi) over Global; tie-break by `last_seen`, then lexicographic address. Ensures all modules converge consistently.
6. **Periodic Sync** – Rust uses `tokio::time::interval`; Android uses WorkManager (15 min min, chain for shorter). Desktop leverages Tauri background tasks.
7. **Reachability** – Async HTTP GET `/health` with fallback to ping-like check; mark `reachable=0` on failure, re-verify with exponential backoff.

Outstanding Clarification: Explicit SLA for discovery intervals still confirmed via research defaults; awaiting product approval (tracked under FR-012).

## Phase 1: Design & Contracts
**Artifacts Generated**:
- `[/[specs/008-specify-md/data-model.md](specs/008-specify-md/data-model.md)](data-model.md)(/[specs/008-specify-md/data-model.md](data-model.md))` – Node entity schema, validation rules, migration strategy, Room/Rust struct parity.
- `/specs/008-specify-md/contracts/nodes-api.yaml` – OpenAPI spec for node listing, discovery trigger, sync, CRUD, health.
- `[/[specs/008-specify-md/contracts/README.md](specs/008-specify-md/contracts/README.md)](contracts/README.md)(/[specs/008-specify-md/contracts/README.md](contracts/README.md))` – Contract usage + testing guidance.
- `[/[specs/008-specify-md/quickstart.md](specs/008-specify-md/quickstart.md)](quickstart.md)(/[specs/008-specify-md/quickstart.md](quickstart.md))` – Seven scenario validation guide covering schema parity, LAN discovery, sync, TTL cleanup, reachability, cross-platform DB, merge priority (local-over-global).
- Agent context updated via `.specify/scripts/bash/update-agent-context.sh cursor`.

Design highlights:
1. **Entities** – `nodes` table with `id`, `address`, `type`, `reachable`, `last_seen`, `ttl`, `source`, `node_id`, timestamps + indices for performance.
2. **APIs** – REST endpoints map to CLI commands; sync endpoint returns merged list; discovery trigger accepts type filters.
3. **Tests** – Contract tests stubbed per endpoint; integration tests planned per quickstart scenarios; instrumentation required for Android WorkManager.
4. **Merge Logic** – Documented priority Local > Global, `last_seen` tie-breaker, deterministic fallback to lexicographic ordering.

## Phase 2: Task Planning Approach
*This section describes what the /tasks command will do - DO NOT execute during /plan*

**Task Generation Strategy**:
- Base template: `[.specify/templates/tasks-template.md](.specify/templates/tasks-template.md)(.[specify/templates/tasks-template.md](specify/templates/tasks-template.md))`
- Inputs: `[data-model.md](data-model.md)(d[ata-model.md](ata-model.md))`, `contracts/nodes-api.yaml`, `[quickstart.md](quickstart.md)(q[uickstart.md](uickstart.md))`, research findings.
- Schema & migrations:
  - Rust `core/src/storage.rs` migration (nodes table + indexes) [P]
  - Android Room migration + DAO update [P]
  - Desktop/Server DB adapters wired to canonical schema
- Model layer:
  - Rust struct + repository updates [P]
  - Android entity + DAO definitions [P]
- Discovery logic:
  - LAN UDP multicast worker
  - Wi-Fi scan integration (Android)
  - Global registry polling service
- Merge/Cleanup:
  - Merge service enforcing Local>Global priority
  - TTL cleanup schedulers per module
- API & CLI:
  - `/nodes` CRUD endpoints + contract tests
  - `/nodes/discover`, `/nodes/sync`, `/nodes/health` endpoints
  - CLI commands for list/add/discover/sync/cleanup/health-validate
- Desktop & Android integration:
  - Background workers, UI surfacing of nodes, instrumentation tests
- Testing & validation:
  - Contract/unit tests per endpoint
  - Integration tests per quickstart scenario
  - CLI verification script + documentation updates

**Ordering Strategy**:
- TDD: Schema → models → contract tests → implementation → integration tests.
- Dependency order: Core schema first, then server/CLI, followed by Desktop/Android.
- Parallelizable items marked [P] in tasks (e.g., platform-specific migrations).
- Documentation and validation tasks trail implementation to capture final behavior.

**Estimated Output**: ~32 ordered tasks (8 schema/model, 10 discovery/merge, 8 API/CLI, 4 platform/UI, 4 testing/documentation).

**IMPORTANT**: This phase is executed by the /tasks command, NOT by /plan

## Phase 3+: Future Implementation
*These phases are beyond the scope of the /plan command*

**Phase 3**: Task execution (/tasks command creates [tasks.md](tasks.md)(t[asks.md](asks.md)  
**Phase 4**: Implementation (execute [tasks.md](tasks.md)(t[asks.md](asks.md)) following constitutional principles)  
**Phase 5**: Validation (run tests, execute [quickstart.md](quickstart.md)(q[uickstart.md](uickstart.md)), performance validation)

## Complexity Tracking
No deviations from constitutional constraints identified at this stage.

## Progress Tracking
*This checklist is updated during execution flow*

**Phase Status**:
- [x] Phase 0: Research complete (/plan command)
- [x] Phase 1: Design complete (/plan command)
- [x] Phase 2: Task planning complete (/plan command - describe approach only)
- [ ] Phase 3: Tasks generated (/tasks command)
- [ ] Phase 4: Implementation complete
- [ ] Phase 5: Validation passed

**Gate Status**:
- [x] Initial Constitution Check: PASS
- [x] Post-Design Constitution Check: PASS
- [ ] All NEEDS CLARIFICATION resolved (FR-012 discovery interval targets awaiting product confirmation)
- [x] Complexity deviations documented (none outstanding)

---
*Based on Constitution v2.0.0 - See `[/memory/constitution.md](/memory/constitution.md)(/[memory/constitution.md](memory/constitution.md))`*

_Version: v1.0.0_
