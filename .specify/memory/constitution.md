# Truth-Evaluation Platform Constitution

## Core Principles

### I. Separation of Concerns by Crate
The system is organized into three crates with clear responsibilities and stable contracts:
- `core` — domain logic, SQLite persistence, and data models; self-contained, independently testable, and documented.
- `server` — Actix Web HTTP API and P2P synchronization layer; exposes API and peer protocols.
- `app` — CLI and future cross-platform UI for administration and monitoring.

Each crate maintains its own tests and versioning; shared logic belongs in `core`.

### II. API- and CLI-First Interfaces
All capabilities are accessible via HTTP API (`server`) and command-line (`app`).
- Text/JSON I/O: stdin/args → stdout; errors → stderr; JSON and human-readable outputs supported.
- CLI is a first-class client of the API; examples double as contract tests.
- Planned TypeScript UI must reuse the same contracts.

### III. Cryptographic Integrity (NON-NEGOTIABLE)
All inter-node communication and sensitive API operations must be signed and verifiable.
- Uses `ed25519_dalek` for signing and verifying messages.
- Nodes authenticate requests via public-key headers and request signatures.
- Deterministic serialization for signed payloads; unsigned/invalid requests are rejected.

### IV. Integration Testing Across Layers
Coherence between API, storage, and P2P is enforced via integration tests.
- Contract tests for new/changed endpoints, schemas, and P2P message formats.
- End-to-end flows: node bootstrap, peer discovery, synchronization, conflict handling.
- Signature validation and replay protection are covered by tests.

### V. Observability, Versioning & Simplicity
- Structured logging and trace IDs; text I/O ensures debuggability.
- Semantic versioning per crate (MAJOR.MINOR.PATCH); document breaking changes.
- Prefer simple designs and SQLite; evolve via explicit migrations (YAGNI).

## Architecture and Technology Stack

### Programming Languages
- Rust — core logic, P2P communication, API.
- TypeScript (planned) — cross-platform interface and admin tools.
- SQL (SQLite) — local embedded data storage.

### Crates
1. `core` — domain logic, storage, and models.
2. `server` — Actix Web API and P2P synchronization.
3. `app` — CLI and future cross-platform UI shell.

### Key Modules
- `p2p/` — node synchronization, peer discovery, crypto identity (Node, Sync, Encryption).
- `api.rs` — HTTP endpoints for TruthEvent, Impact, and signature validation.
- `main.rs` — initializes database, spawns node, starts HTTP server.
- `core/storage.rs` — CRUD and data seeding for domain entities.

### Encryption and Identity
- `ed25519_dalek` for signatures; nodes authenticate via public-key headers and signatures.
- Keys are per-node; rotation follows a governed process; never log private material.

### Future Direction
- Add Electron/Tauri or web-based UI for visualization and admin control.
- Extend `app` crate to integrate with the P2P layer via API.

### Documentation References
- `spec/01-product-vision.md`
- `spec/02-requirements.md`
- `spec/03-architecture.md`
- `spec/04-data-model.md`
- `spec/05-api.md`
- `spec/06-expert-system.md`
- `spec/07-event-rating-protocol.md`
- `spec/08-p2p-sync.md`
- `spec/09-ux-guidelines.md`
- `spec/10-roadmap.md`
- `spec/11-decision-log.md`
- `spec/12-open-questions.md`
- `spec/13-traceability.md`
- `spec/14-quality-gates.md`
- `spec/15-prompts-and-automation.md`
- `spec/16-test-plan.md`

### Primary Goal
Ensure coherence between API, storage, and P2P layers; maintain cryptographic integrity of communications; provide a foundation for a portable, extensible truth‑evaluation platform.

## Development Workflow and Quality Gates

- Test-first: follow `spec/16-test-plan.md`; Red-Green-Refactor cycle enforced.
- Quality gates: see `spec/14-quality-gates.md`; CI runs fmt, clippy, unit/integration tests; coverage thresholds enforced.
- Reviews: PRs must verify compliance with this constitution and relevant specs.
- Traceability: keep `spec/13-traceability.md` in sync; link commits/PRs to requirements.
- Breaking changes: require updates to `spec/03-architecture.md`, `spec/04-data-model.md`, `spec/05-api.md`, `spec/08-p2p-sync.md` and a migration plan.
- Storage migrations: provide forward/backward migrations and seed updates in `core`.
- API/P2P contracts: add/extend contract tests; bump versions; document in `spec/11-decision-log.md`.
- Release process: per-crate semver bump, changelog entry, artifacts; maintain compatibility notes in specs.

## Governance

This constitution supersedes other practices. Amendments require documentation, approval, and a migration strategy.
- All PRs/reviews must assert compliance and link to updated specs.
- Complexity must be justified; prefer simple, observable solutions.
- Cryptographic/protocol changes require security review and test evidence.
- Use `spec/15-prompts-and-automation.md` for automation and agent guidance.

**Version**: 1.0.0 | **Ratified**: 2025-10-06 | **Last Amended**: 2025-10-06
