# Truth Training Platform Constitution

## Philosophical Foundation

The "Truth Training" concept emerged as a response to the crisis of trust and evolved into a philosophical model of collective human self-learning through shared cognition. Its foundation lies at the intersection of science, ethics, and philosophy — from probability theory and behavioral psychology to ancient ideas of harmony and the relativity of observation.

In this view, truth is not declared — it is born through collaboration, correction, and convergence among many minds. Truth is a process, not an object, emerging from continuous interaction among many observers — like rhythm arising from multiple beats.

### Truth as Anonymous Confession

The system enables individuals to express truth without fear of judgment — anonymously and cryptographically verified. Like a digital form of confession, it allows a person to share, acknowledge, or correct information within a collective context. The act of revealing truth itself becomes part of the training — a feedback loop of ethical learning.

### Core Philosophical Principles

**Collective Intelligence as a Neural System of Society**: Each participant acts as a neuron — transmitting, receiving, and adapting — while the network learns through feedback.

**Trust as a Quantifiable Parameter**: Reputation becomes a measurable property, similar to energy or density, integrating ethics with information theory.

**The Wisdom of the Crowd as Natural Selection of Information**: Averaging independent inputs filters out extremes, leaving stable and reproducible approximations of truth.

**Ethics as Algorithm**: Instead of moral judgment or punishment, the system relies on adaptive feedback loops — reducing influence for error, rewarding correction.

**Harmony of Perception as the Goal**: Truth is not an absolute endpoint but a state of dynamic balance achieved through constant exchange and recalibration.

**Truth Without Author**: The platform separates truth from authorship. Validation occurs through distributed resonance — multiple confirmations and rejections — rather than authority or identity.

## Core Technical Principles

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

## Collective Intelligence Principles

### VI. Truth as Emergent Consensus
Truth is not stored but converged upon through continuous exchanges. The system should not "store truth" but converge toward it over time. Repeated confirmations strengthen an event; repeated rejections weaken it.

### VII. Dynamic Reputation and Weighted Consensus
Each validator's influence (weight) depends on the accuracy of their past judgments. The collective judgment becomes a weighted consensus of experience rather than a mere vote count.

### VIII. Self-Correcting Information Ecosystem
The system promotes learning and correction rather than punishment — turning truth verification into a form of cognitive training. Each transmission or confirmation has semantic meaning — a signal of trust, correction, or decay.

### IX. Cross-Platform Collective Intelligence
The system maintains platform parity between desktop and mobile implementations, ensuring that collective intelligence can emerge across all access points while respecting platform-specific constraints and capabilities.

## Societal Applications

### I. Anti-Fraud and Integrity Detection

The accumulation of verified events enables probabilistic modeling of truth. Repeated independent confirmations form statistical weight, allowing the system to estimate the authenticity of new claims. Fraud, misinformation, or manipulation attempts decay over time as inconsistent signals lose coherence.

### II. Digital Conscience and Ethical Reflection

By allowing anonymous truth submission and collective verification, the system acts as a distributed moral network — replacing institutional confession with self-organized introspection.

### III. Decentralized Civic Dialogue

Truth Training can function as a decentralized replacement for traditional voting. Instead of casting binary votes, participants express judgments on factual events. Social consensus emerges as a real-time measure of collective reasoning rather than a periodic count.

### IV. Local Mesh of Truth Exchange

Even without central connectivity, nodes can exchange and synchronize events over short-range or opportunistic connections (Wi-Fi, Bluetooth, etc.). This makes the network resilient — similar to LoRa mesh systems but using standard communication layers. Truth spreads naturally, hop by hop, without requiring additional hardware.

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
- [spec/01-product-vision.md](spec/01-product-vision.md)
- [spec/02-requirements.md](spec/02-requirements.md)
- [spec/03-architecture.md](spec/03-architecture.md)
- [spec/04-data-model.md](spec/04-data-model.md)
- [spec/05-api.md](spec/05-api.md)
- [spec/06-expert-system.md](spec/06-expert-system.md)
- [spec/07-event-rating-protocol.md](spec/07-event-rating-protocol.md)
- [spec/08-p2p-sync.md](spec/08-p2p-sync.md)
- [spec/09-ux-guidelines.md](spec/09-ux-guidelines.md)
- [spec/10-roadmap.md](spec/10-roadmap.md)
- [spec/11-decision-log.md](spec/11-decision-log.md)
- [spec/12-open-questions.md](spec/12-open-questions.md)
- [spec/13-traceability.md](spec/13-traceability.md)
- [spec/14-quality-gates.md](spec/14-quality-gates.md)
- [spec/15-prompts-and-automation.md](spec/15-prompts-and-automation.md)
- [spec/16-test-plan.md](spec/16-test-plan.md)
- [spec/18-cross-platform-architecture.md](spec/18-cross-platform-architecture.md)
- [spec/19-build-instructions.md](spec/19-build-instructions.md)
- [spec/20-cargo-configuration.md](spec/20-cargo-configuration.md)

### Primary Goal
Ensure coherence between API, storage, and P2P layers; maintain cryptographic integrity of communications; provide a foundation for a portable, extensible truth‑evaluation platform that enables collective intelligence through distributed consensus.

## Development Workflow and Quality Gates

- Test-first: follow [spec/16-test-plan.md](spec/16-test-plan.md); Red-Green-Refactor cycle enforced.
- Quality gates: see [spec/14-quality-gates.md](spec/14-quality-gates.md); CI runs fmt, clippy, unit/integration tests; coverage thresholds enforced.
- Reviews: PRs must verify compliance with this constitution and relevant specs.
- Traceability: keep [spec/13-traceability.md](spec/13-traceability.md) in sync; link commits/PRs to requirements.
- Breaking changes: require updates to [spec/03-architecture.md](spec/03-architecture.md), [spec/04-data-model.md](spec/04-data-model.md), [spec/05-api.md](spec/05-api.md), [spec/08-p2p-sync.md](spec/08-p2p-sync.md) and a migration plan.
- Storage migrations: provide forward/backward migrations and seed updates in `core`.
- API/P2P contracts: add/extend contract tests; bump versions; document in [spec/11-decision-log.md](spec/11-decision-log.md).
- Release process: per-crate semver bump, changelog entry, artifacts; maintain compatibility notes in specs.

## Governance

This constitution supersedes other practices. Amendments require documentation, approval, and a migration strategy.
- All PRs/reviews must assert compliance and link to updated specs.
- Complexity must be justified; prefer simple, observable solutions.
- Cryptographic/protocol changes require security review and test evidence.
- Use [spec/15-prompts-and-automation.md](spec/15-prompts-and-automation.md) for automation and agent guidance.
- Collective intelligence principles must be preserved in all architectural decisions.
- Truth training methodology must be reflected in all user-facing interfaces and data flows.

**Version**: 2.1.0 | **Ratified**: 2025-10-31 | **Last Amended**: 2025-10-31

_Version: v1.0.0_

