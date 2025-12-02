# Truth Training Platform Constitution

<!--
Sync Impact Report
Version: 2.1.0 → 2.2.0
Modified Principles: New Rule 1–7 block replaced prior implicit guidance
Added Sections: Summary, Formal Rules, Change History table
Removed Sections: Legacy trailing version banner only
Templates: [.specify/templates/plan-template.md](.specify/templates/plan-template.md) ✅, [.specify/templates/spec-template.md](.specify/templates/spec-template.md) ✅, [.specify/templates/tasks-template.md](.specify/templates/tasks-template.md) ✅, .specify/templates/commands (dir missing) ⚠ document added follow-up
Follow-ups: TODO(COMMAND_TEMPLATES): Recreate or document actual command templates under .specify/templates/commands to keep references resolvable.
-->

## Summary

Truth Training orchestrates a cryptographically verifiable, anonymous truth network that must run everywhere our participants reach it: CLI diagnostics, autonomous servers, desktop UI, and mobile clients. This constitution captures the philosophical intent, enumerates the immovable cross-platform rules, and binds the engineering workflow (README, CONTRIBUTING, SECURITY, CHANGELOG, Spec-Kit templates, and release automation) so Cursor agents and humans always operate from the same source of truth.

## Formal Rules

### Rule 1 — Cross-Platform Scope & Parity

1. The project simultaneously ships four first-class surfaces and keeps them feature-aligned: `app/src/bin/truthctl.rs` (CLI control & diagnostics), the Actix-based server node (autonomous FidoNet-style behavior with no sysop intervention), the Desktop UI for Linux/Windows/macOS (Tauri backend with glib/GTK renderer on Linux, operating both offline and network-connected), and mobile clients implemented in Kotlin (Android) and Swift (iOS) that mirror Desktop functionality.
2. Platform parity is verified through cross-platform specs/tests (e.g., `[docs/cross_platform_discovery_compatibility.md](docs/cross_platform_discovery_compatibility.md)`) and enforced for every API, schema, and consensus rule; drift is not permitted.

### Rule 2 — Source Documents as Authority

1. `[README.md](README.md)`, `[CONTRIBUTING.md](CONTRIBUTING.md)`, `[SECURITY.md](SECURITY.md)`, and `[CHANGELOG.md](CHANGELOG.md)` are binding references. When they change, this constitution must be updated accordingly; when this constitution adds constraints, those documents must be updated in the same pull request.
2. Any deviation from those sources is recorded (with justification) in the Change History table below so auditors see when and why governance diverged.

### Rule 3 — Releases, Installation & Automation

1. Every tagged release must be installable in four choices (CLI binary, server node, desktop UI installers, mobile binaries). If a surface is not ready, the release is blocked.
2. The README must contain up-to-date install instructions for each surface before a release is approved.
3. Release preparation and automation must remain executable by the Cursor AI agent exactly as described in `[CONTRIBUTING.md](CONTRIBUTING.md)` section “## 2. Release Preparation Requirements,” using `create-release.sh`, `release-info.txt`, and the associated versioned release-info files. Scripts and docs must be maintained alongside the code.
4. Release automation outputs (artifacts, CHANGELOG entry, README install sections) are audited by Spec-Kit workflows, making this policy testable.

### Rule 4 — Dependency, Vulnerability & Platform Safeguards

1. Dependencies with known vulnerabilities must be upgraded proactively when a non-breaking safe version exists; blocking advisories require justification and an action date.
2. Platform-specific vulnerabilities (e.g., glib < 0.20.0 impacting GTK3/Linux builds) demand an immediate mitigation strategy documented in specs plus conditional builds until patched.
3. Linux/GTK builds must perform a periodic glib version check; release automation validates that dependency manifests (`Cargo.lock`, `pnpm-lock.yaml`, Gradle catalogs) reflect those updates.
4. Automated scans (`cargo audit`, `npm audit`, `pnpm audit`, `gradlew lint`, Dependabot, CodeQL, or approved equivalents) run in CI and pre-release checklists; failures block merges until resolved or formally waived.

### Rule 5 — Database & Schema Integrity

1. Truth Training data schemas MUST satisfy 1NF, 2NF, 3NF, BCNF, 4NF, and 5NF; 6NF/DKNF may be adopted for temporal/domain-heavy modules when justified in specs.
2. Every table requires a unique key, unused tables are removed, and each schema change ships with documented forward/backward migrations plus cleanup scripts.
3. Release checklists include a database/schema review referencing `[spec/04-data-model.md](spec/04-data-model.md)`, `[docs/Data_Schema.md](docs/Data_Schema.md)`, and migration scripts; releases are blocked if documentation or migrations lag behind.

### Rule 6 — CI, Tooling & Automation Discipline

1. Spec-Kit artifacts (specs, plans, tasks), docs, release scripts, lint/test runners, and validation checks MUST exist, be versioned, and remain runnable by Cursor agents without manual intervention.
2. `[.specify/memory/constitution.md](.specify/memory/constitution.md)` is the canonical constitution; all `/speckit.*` commands reference it and update Spec-Kit templates when new governance gates appear.
3. Templates checked during this amendment (`.specify/templates/{spec,plan,tasks}[-template.md](-template.md)`) stay aligned; missing command templates are tracked until restored.

### Rule 7 — Security & Privacy Enforcement

1. `[SECURITY.md](SECURITY.md)` defines the security model; this constitution requires periodic dependency scans (Dependabot/CodeQL or equivalent) plus manual `cargo audit`, `npm audit`, and Android/Desktop security tooling runs before releases.
2. Remediation SLAs: Critical vulnerabilities addressed within 48 hours, High within 7 days, Medium within 14 days, and documentation recorded in issues/specs.
3. Privacy rules from `[SECURITY.md](SECURITY.md)` (no persistent identity, no telemetry, no plaintext secrets) are binding on every platform and must be validated during code review and release automation.

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

### I. Separation of Concerns by Crate and Surface
The system is organized into Rust crates with clear responsibilities and mirrored clients:
- `core` — domain logic, SQLite persistence, and data models; self-contained, independently testable, and documented.
- `server` — Actix Web HTTP API and P2P synchronization layer; exposes API and peer protocols while behaving autonomously in networks reminiscent of FidoNet.
- `app` — hosts both the CLI (`app/src/bin/truthctl.rs`) and launcher logic consumed by desktop/mobile shells for administration and monitoring.
- Desktop and mobile front ends call the same APIs and binaries; any shared logic lives in `core`.

### II. API-, CLI-, and UI-First Interfaces
All capabilities are accessible via HTTP API (`server`), command-line (`app`), desktop UI (Tauri + glib/GTK on Linux), and mobile clients (Kotlin/Swift wrappers):
- Text/JSON I/O: stdin/args → stdout; errors → stderr; JSON and human-readable outputs supported.
- CLI is a first-class client of the API; examples double as contract tests.
- Desktop and mobile UIs reuse the contracts; offline-first workflows are mandatory for desktop/mobile parity.

### III. Cryptographic Integrity (NON-NEGOTIABLE)
All inter-node communication and sensitive API operations must be signed and verifiable.
- Uses `ed25519_dalek` (Rust), platform-specific bindings on Kotlin/Swift, for signing and verifying messages.
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
- Rust — core logic, P2P communication, API, Tauri backend.
- TypeScript — desktop interface and admin tools.
- Kotlin — Android client, JNI bindings, mobile services.
- Swift — iOS client interop (sharing Kotlin feature parity).
- SQL (SQLite/Postgres) — local embedded data storage.

### Crates and Clients
1. `core` — domain logic, storage, and models.
2. `server` — Actix Web API and P2P synchronization.
3. `app` — CLI plus shared administration logic.
4. Desktop/mobile shells — Tauri/React front end and mobile Compose/SwiftUI clients tethered to the Rust core artifacts.

### Key Modules
- `p2p/` — node synchronization, peer discovery, crypto identity (Node, Sync, Encryption).
- `api.rs` — HTTP endpoints for TruthEvent, Impact, and signature validation.
- `main.rs` — initializes database, spawns node, starts HTTP server.
- `core/storage.rs` — CRUD and data seeding for domain entities.
- Mobile/desktop service bridges — FFI bindings to the Rust core, ensuring schema parity.

### Encryption and Identity
- `ed25519_dalek` for signatures; nodes authenticate via public-key headers and signatures.
- Keys are per-node; rotation follows a governed process; never log private material.

### Future Direction
- Expand Tauri/desktop offline workflows and publish GTK parity tests.
- Keep Kotlin/Swift releases aligned with desktop semantics for offline and network modes.
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
- Storage migrations: provide forward/backward migrations and seed updates in `core` while meeting the normal form guarantees in Rule 5.
- API/P2P contracts: add/extend contract tests; bump versions; document in [spec/11-decision-log.md](spec/11-decision-log.md).
- Release process: per-crate semver bump, changelog entry, artifacts; maintain compatibility notes in specs; ensure README install sections reflect the four release surfaces before running `create-release.sh`.
- Release automation must also confirm glib version checks, dependency audits, and schema reviews, logging outcomes in `[CHANGELOG.md](CHANGELOG.md)` and the versioned release-info files.

## Governance

This constitution supersedes other practices. Amendments require documentation, approval, and a migration strategy.
- All PRs/reviews must assert compliance and link to updated specs.
- Complexity must be justified; prefer simple, observable solutions.
- Cryptographic/protocol changes require security review and test evidence.
- Use [spec/15-prompts-and-automation.md](spec/15-prompts-and-automation.md) for automation and agent guidance; `/speckit.*` commands MUST confirm they sourced assumptions from this file.
- Collective intelligence principles must be preserved in all architectural decisions.
- Truth training methodology must be reflected in all user-facing interfaces and data flows.
- Acceptance criteria: this file (and associated Spec-Kit state) stays at `[.specify/memory/constitution.md](.specify/memory/constitution.md)`, referenced by all governance-related commands, and deviations from authoritative docs are captured in the Change History table.

## Change History

| Date       | Version | Author     | Notes                                                                                                    | Deviations vs README/CONTRIBUTING/SECURITY/CHANGELOG |
|------------|---------|------------|----------------------------------------------------------------------------------------------------------|------------------------------------------------------|
| 2025-12-01 | 2.2.0   | Cursor AI  | Added explicit cross-platform scope, release automation, dependency/DB policies, Spec-Kit enforcement.  | None; aligned with referenced docs                   |
| 2025-10-31 | 2.1.0   | Maintainers | Prior governance uplift aligning constitution with anonymous confession and collective intelligence.     | Not recorded                                          |

**Version**: 2.2.0 | **Ratified**: 2025-10-31 | **Last Amended**: 2025-12-01
